package com.rationaleemotions.server;

import static com.rationaleemotions.config.ConfigReader.getInstance;
import static com.spotify.docker.client.DockerClient.LogsParam.stderr;
import static com.spotify.docker.client.DockerClient.LogsParam.stdout;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.openqa.selenium.net.PortProber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.rationaleemotions.server.ISeleniumServer.ServerException;
import com.rationaleemotions.server.docker.ContainerAttributes;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerClient.RemoveContainerParam;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.LoggingBuildHandler;
import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.exceptions.ContainerNotFoundException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.Image;
import com.spotify.docker.client.messages.PortBinding;

/**
 * A Helper class that facilitates interaction with a Docker Daemon.
 */
public final class DockerHelper {
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public static final String UNIX_SCHEME = "unix";

	private static DockerClient dockerClient = null;

	private DockerHelper() {
		throw new IllegalStateException("Helper class");
	}

	/**
	 * @param id
	 *            - The ID of the container that is to be cleaned up.
	 * @throws DockerException
	 *             - In case of any issues.
	 * @throws InterruptedException
	 *             - In case of any issues.
	 */
	public static void killAndRemoveContainer(final String id) throws DockerException, InterruptedException {
		LOG.debug("Killing and removing the container : [{}].", id);
		try {
			getClient().killContainer(id);
		} catch (ContainerNotFoundException e) {
			LOG.info("Fail to kill container {}, already removed!", id);

		} catch (DockerException | InterruptedException e) {
			// Fail if containers already stopped
			LOG.error("Fail to kill container {}", id, e);
		}
		removeContainer(id);
	}

	/**
	 * @param id
	 *            - The ID of the container that is to be removed.
	 * @throws DockerException
	 *             - In case of any issues.
	 * @throws InterruptedException
	 *             - In case of any issues.
	 */
	public static void removeContainer(final String id) throws DockerException, InterruptedException {
		LOG.debug("Removing the container : [{}].", id);
		try {
			if (!getClient().inspectContainer(id).hostConfig().autoRemove()) {
				getClient().removeContainer(id, RemoveContainerParam.removeVolumes());
			}
		} catch (ContainerNotFoundException e) {
			LOG.info("Fail to remove container {}, already removed!", id);

		} catch (Exception e) {
			LOG.warn("Fail to remove container {} : {}", id, e.getMessage(), e);
		}
	}

	public static void removeContainersWithLabel(String label, Duration createdSince) {
		List<Container> containers = null;
		try {
			containers = getClient().listContainers(DockerClient.ListContainersParam.withLabel(label));
		} catch (DockerException | InterruptedException e) {
			LOG.warn("Fail to retrieve list of containers", e.getMessage(), e);
		}
		for (Container container : containers) {
			try {
				boolean removeContainer = createdSince == null ? true : false;

				if (createdSince != null) {
					com.spotify.docker.client.messages.ContainerInfo containerInfo = getClient()
							.inspectContainer(container.id());
					if (containerInfo.created().toInstant().isBefore(Instant.now().minus(createdSince))) {
						removeContainer = true;
					}
				}
				if (removeContainer) {
					LOG.info("Removing old container {} ...", container.id());
					killAndRemoveContainer(container.id());
				} else {
					LOG.info("keep container {}...", container.id());
				}
			} catch (DockerException | InterruptedException e) {
				LOG.warn("Fail to remove container {} : {}", container.id(), e.getMessage(), e);
			}
		}
	}

	/**
	 * @param A
	 *            {@link ContainerAttributes} object
	 * @return - A {@link ContainerInfo} object that represents the newly spun
	 *         off container.
	 * @throws DockerException
	 *             - In case of any issues.
	 * @throws InterruptedException
	 *             - In case of any issues.
	 * @throws ServerException
	 */
	public static ContainerInfo startContainerFor(final ContainerAttributes containerAttributes)
			throws DockerException, InterruptedException, ServerException {
		LOG.debug("Starting of container for the image [{}].", containerAttributes.getImage());

		Preconditions.checkState("ok".equalsIgnoreCase(getClient().ping()),
				"Ensuring that the Docker Daemon is up and running.");
		DockerHelper.predownloadImageIfRequired(containerAttributes.getImage());

		final Map<String, List<PortBinding>> portBindings = new HashMap<>();

		String localHost = getInstance().getLocalhost();
		List<String> exposedPorts = new LinkedList<>();
		if (containerAttributes.getPorts() != null) {
			exposedPorts = new LinkedList<>(containerAttributes.getPorts());
		}
		exposedPorts.add(Integer.toString(containerAttributes.getPort()));
		int containerPort = 0;
		for (String port : exposedPorts) {
			int randomPort = PortProber.findFreePort();
			if (Integer.parseInt(port) == containerAttributes.getPort()) {
				containerPort = randomPort;
			}
			PortBinding binding = PortBinding.create(localHost, Integer.toString(randomPort));
			List<PortBinding> portBinding = new ArrayList<>();
			portBinding.add(binding);
			portBindings.put(port, portBinding);
		}
		String containerName = generateContainerName("just-ask-node", Integer.toString(containerPort));

		final ContainerCreation creation = createContainer(containerName, containerAttributes, portBindings,
				exposedPorts);
		final String id = creation.id();

		com.spotify.docker.client.messages.ContainerInfo containerInfo = getClient().inspectContainer(id);

		if (!containerInfo.state().running()) {
			// Start container
			getClient().startContainer(id);
			LOG.debug("Starting {}", containerName);
		} else {
			LOG.debug("{} was already running.", containerName);
		}

		waitContainerAvailable(id);

		containerInfo = getClient().inspectContainer(id);
		String gatewayIP = containerInfo.networkSettings().gateway();

		ContainerInfo info = new ContainerInfo(id, gatewayIP, containerPort);
		LOG.debug("******{}******", info);

		return info;
	}

	private static String generateContainerName(String containerName, String nodePort) {
		return String.format("%s-%s", containerName, nodePort);
	}

	/**
	 * @param containerName
	 * @param containerAttributes
	 * @param portBindings
	 * @param exposedPorts
	 * @return
	 * @throws DockerException
	 * @throws InterruptedException
	 */
	private static ContainerCreation createContainer(String containerName,
			final ContainerAttributes containerAttributes, final Map<String, List<PortBinding>> portBindings,
			List<String> exposedPorts) throws DockerException, InterruptedException {

		final HostConfig hostConfig = HostConfig.builder().portBindings(portBindings)
				.privileged(containerAttributes.isPrivileged()).binds(containerAttributes.getVolumes()).autoRemove(true)
				.shmSize(containerAttributes.getShmSize()).build();

		final ContainerConfig containerConfig = ContainerConfig.builder().hostConfig(hostConfig)
				.image(containerAttributes.getImage()).exposedPorts(new HashSet<String>(exposedPorts))
				.labels(containerAttributes.getLabels()).env(containerAttributes.getEnvs()).build();
		return getClient().createContainer(containerConfig, containerName);
	}

	/**
	 * @param id
	 * @throws InterruptedException
	 * @throws DockerException
	 * @throws ServerException
	 */
	private static void waitContainerAvailable(final String id)
			throws InterruptedException, DockerException, ServerException {
		com.spotify.docker.client.messages.ContainerInfo containerInfo;
		AtomicInteger attempts = new AtomicInteger(0);
		Integer exitCode = 0;
		do {
			// Inspect container
			TimeUnit.SECONDS.sleep(1);
			containerInfo = getClient().inspectContainer(id);
			exitCode = containerInfo.state().exitCode();
			if (exitCode == null) {
				exitCode = 0;
			}
			LOG.info("Container {} with id {} - Information {}", containerInfo.name(), id, containerInfo);

		} while (!containerInfo.state().running() && exitCode == 0 && attempts.incrementAndGet() <= 10);

		if (!containerInfo.state().running() || exitCode != 0) {
			try (LogStream stream = getClient().logs(id, stdout(), stderr())) {
				final String logs;
				logs = stream.readFully();
				LOG.error("Container logs:\n{}", logs);

			} catch (Exception e) {
				LOG.error("Fail to retrieve container logs...", e);
			}
			LOG.error("Failed to start Container {} with id {} - Information {}", containerInfo.name(), id,
					containerInfo);

			if (exitCode != 0) {
				// Fail to start container, remove it!
				removeContainer(id);
			} else {
				// No exit code? System busy? try to kill it and remove it!
				killAndRemoveContainer(id);
			}

			throw new ServerException(
					String.format("Failed to start Container %s with id %s", containerInfo.name(), id));
		}
		LOG.debug("Cantainer available : {}", containerInfo);
	}

	private static void predownloadImageIfRequired(String dockerImage) throws DockerException, InterruptedException {

		LOG.info("Start downloading of image {}", dockerImage);

		ProgressHandler handler = new LoggingBuildHandler();
		List<Image> foundImages = getClient().listImages(DockerClient.ListImagesParam.byName(dockerImage));
		if (!foundImages.isEmpty()) {
			LOG.info("Skipping download for Image [{}] because it's already available.", dockerImage);
		} else {
			getClient().pull(dockerImage, handler);
		}
	}

	private static synchronized DockerClient getClient() {
		if (dockerClient == null) {
			dockerClient = new DefaultDockerClient(getInstance().getDockerRestApiUri());
		}
		return dockerClient;
	}

	/**
	 * A Simple POJO that represents the newly spun off container, encapsulating
	 * the container Id and the port on which the container is running.
	 */
	public static class ContainerInfo {
		private int port;
		private String containerId;
		private String gatewayIP;

		ContainerInfo(final String containerId, String gatewayIP, final int port) {
			this.port = port;
			this.containerId = containerId;
			this.gatewayIP = gatewayIP;
		}

		public int getPort() {
			return port;
		}

		public String getContainerId() {
			return containerId;
		}

		public String getGatewayIP() {
			return gatewayIP;
		}

		@Override
		public String toString() {
			return String.format("%s running on %s:%d", containerId, gatewayIP, port);
		}
	}
}