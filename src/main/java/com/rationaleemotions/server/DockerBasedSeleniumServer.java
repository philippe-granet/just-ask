package com.rationaleemotions.server;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openqa.grid.internal.TestSession;
import org.openqa.selenium.remote.CapabilityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rationaleemotions.config.BrowserVersionInfo;
import com.rationaleemotions.config.ConfigReader;
import com.rationaleemotions.server.docker.ContainerAttributes;
import com.spotify.docker.client.exceptions.DockerException;

/**
 * Represents a {@link ISeleniumServer} implementation that is backed by a
 * docker container which executes the selenium server within a docker
 * container.
 *
 */
public class DockerBasedSeleniumServer extends AbstractSeleniumServer {

	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private DockerHelper.ContainerInfo containerInfo;

	public DockerBasedSeleniumServer() {
		// Nothing to do
	}

	@Override
	public int startServer(final TestSession session) throws ServerException {
		try {
			containerInfo = DockerHelper.startContainerFor(getContainerAttributes(session.getRequestedCapabilities()));
			session.put("container", containerInfo);
			return containerInfo.getPort();
		} catch (DockerException | InterruptedException e) {
			throw new ServerException(e.getMessage(), e);
		}
	}

	@Override
	public int getPort() {
		return this.containerInfo.getPort();
	}

	@Override
	public String getHost() {
		String uri = ConfigReader.getInstance().getDockerRestApiUri();
		if (uri.startsWith(DockerHelper.UNIX_SCHEME)) {
			return containerInfo.getGatewayIP();
		} else {
			return URI.create(uri).getHost();
		}
	}

	@Override
	public void shutdownServer() throws ServerException {
		try {
			DockerHelper.killAndRemoveContainer(containerInfo.getContainerId());
		} catch (DockerException | InterruptedException e) {
			throw new ServerException(e.getMessage(), e);
		}
	}

	private ContainerAttributes getContainerAttributes(final Map<String, Object> requestedCapabilities) {
		String browser = (String) requestedCapabilities.get(CapabilityType.BROWSER_NAME);
		String version = (String) requestedCapabilities.get(CapabilityType.BROWSER_VERSION);

		BrowserVersionInfo browserVersion = ConfigReader.getInstance().getBrowserVersion(browser, version);
		String image = browserVersion.getTargetAttribute("image");
		int port = Integer.parseInt(browserVersion.getTargetAttribute("port"));

		List<String> ports = browserVersion.getTargetAttributeAsList("ports");
		List<String> volumes = browserVersion.getTargetAttributeAsList("volumes");

		List<String> envs = getEnvs(requestedCapabilities, browserVersion);
		Map<String, String> labels = getLabels(browser, version);

		Long shmSize = getMemorySize(browserVersion.getTargetAttribute("shmSize"));
		Long memory = getMemorySize(browserVersion.getTargetAttribute("memory"));

		ContainerAttributes containerAttributes = new ContainerAttributes();
		containerAttributes.setImage(image);
		containerAttributes.setPort(port);
		containerAttributes.setPorts(ports);
		containerAttributes.setVolumes(volumes);
		containerAttributes.setEnvs(envs);
		containerAttributes.setLabels(labels);
		containerAttributes.setPrivileged(false);
		containerAttributes.setShmSize(shmSize);
		containerAttributes.setMemory(memory);
		return containerAttributes;
	}

	private Map<String, String> getLabels(String browser, String version) {
		Map<String, String> labels = new HashMap<>();
		labels.put(browser, version);
		labels.put("just-ask-node", "just-ask-node");
		return labels;
	}

	/**
	 * @param requestedCapabilities
	 * @param browserVersion
	 * @return
	 */
	private List<String> getEnvs(final Map<String, Object> requestedCapabilities, BrowserVersionInfo browserVersion) {
		List<String> envs = new ArrayList<>();

		List<String> configEnv = browserVersion.getTargetAttributeAsList("env");
		if (configEnv != null && !configEnv.isEmpty()) {
			envs.addAll(configEnv);
		}
		// Capabilities envs overrides configured envs
		List<String> capabilitiesEnv = getConfiguredDockerEnvsFromCapabilities(requestedCapabilities);
		envs.addAll(capabilitiesEnv);
		return envs;
	}

	private List<String> getConfiguredDockerEnvsFromCapabilities(final Map<String, Object> requestedCapabilities) {

		List<String> envs = new ArrayList<>();
		for (Entry<String, Object> capability : requestedCapabilities.entrySet()) {
			if (capability.getKey().startsWith("DOCKER_ENV_")) {
				String value = capability.getValue() != null ? capability.getValue().toString() : null;
				envs.add(capability.getKey().replace("DOCKER_ENV_", "") + "=" + value);
			}
		}
		return envs;
	}

	protected Long getMemorySize(final String size) {
		if (size == null) {
			return null;
		}
		try {
			if (size.endsWith("b")) {
				return Long.parseLong(size.replace("b", ""));

			} else if (size.endsWith("k")) {
				return Long.parseLong(size.replace("k", "")) * 1024L;

			} else if (size.endsWith("m")) {
				return Long.parseLong(size.replace("m", "")) * 1024L * 1024L;

			} else if (size.endsWith("g")) {
				return Long.parseLong(size.replace("g", "")) * 1024L * 1024L * 1024L;

			} else {
				return Long.parseLong(size) * 1024L * 1024L * 1024L;
			}
		} catch (NumberFormatException e) {
			LOG.error("invalid size - {}", e.getMessage());
		}
		return null;
	}
}
