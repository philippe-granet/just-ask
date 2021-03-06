package com.rationaleemotions.proxy;

import static org.openqa.grid.web.servlet.handler.RequestType.START_SESSION;
import static org.openqa.grid.web.servlet.handler.RequestType.STOP_SESSION;
import static org.openqa.selenium.remote.ErrorCodes.SUCCESS;

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.openqa.grid.common.RegistrationRequest;
import org.openqa.grid.common.SeleniumProtocol;
import org.openqa.grid.common.exception.GridException;
import org.openqa.grid.internal.GridRegistry;
import org.openqa.grid.internal.SessionTerminationReason;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.internal.TestSlot;
import org.openqa.grid.internal.listeners.RegistrationListener;
import org.openqa.grid.selenium.proxy.DefaultRemoteProxy;
import org.openqa.grid.web.servlet.handler.RequestType;
import org.openqa.grid.web.servlet.handler.SeleniumBasedRequest;
import org.openqa.selenium.remote.BeanToJsonConverter;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.server.jmx.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapMaker;
import com.google.gson.JsonObject;
import com.rationaleemotions.config.ConfigReader;
import com.rationaleemotions.internal.ProxiedTestSlot;
import com.rationaleemotions.server.DockerHelper;
import com.rationaleemotions.server.SpawnedServer;

/**
 * Represents a simple {@link DefaultRemoteProxy} implementation that relies on
 * spinning off a server and then routing the session traffic to the spawned
 * server.
 */
@ManagedService(description = "Selenium Grid Hub TestSlot")
public class GhostProxy extends DefaultRemoteProxy implements RegistrationListener {
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private Map<String, SpawnedServer> servers = new MapMaker().initialCapacity(500).makeMap();

	private Thread cleaningThread = null;

	private boolean poll = true;

	private volatile int pollingInterval = 10 * 60 * 1000;

	public GhostProxy(final RegistrationRequest request, final GridRegistry registry) {
		super(request, registry);
		LOG.info("Maximum sessions supported : {}", config.maxSession);
	}

	@Override
	public TestSlot createTestSlot(final SeleniumProtocol protocol, final Map<String, Object> capabilities) {
		return new ProxiedTestSlot(this, protocol, capabilities);
	}

	@Override
	public TestSession getNewSession(final Map<String, Object> requestedCapability) {

		if (getTotalUsed() >= config.maxSession) {
			LOG.info("Waiting for remote nodes to be available");
			return null;
		}

		LOG.debug("Trying to create a new session on node {}", this);

		Map<String, Object> requestedCapabilityWithVersion = new HashMap<>(requestedCapability);

		// if version is empty, use default version
		String version = (String) requestedCapability.get(CapabilityType.BROWSER_VERSION);
		if (StringUtils.isBlank(version)) {
			String browser = requestedCapability.get(CapabilityType.BROWSER_NAME).toString();
			version = ConfigReader.getInstance().getBrowserDefaultVersion(browser).getVersion();
			requestedCapabilityWithVersion.put(CapabilityType.BROWSER_VERSION, version);
		}
		// any slot left for the given app ?
		for (TestSlot testslot : getTestSlots()) {
			TestSession session = testslot.getNewSession(requestedCapabilityWithVersion);
			if (session != null) {
				return session;
			}
		}
		return null;
	}

	@Override
	public void startPolling() {
		super.startPolling();
		Runnable task = () -> {
			while (poll) {
				try {
					Thread.sleep(pollingInterval);
					DockerHelper.removeContainersWithLabel("just-ask-node", Duration.ofHours(1));
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		};
		String taskName = GhostProxy.class.getSimpleName() + " CleanUpThread for " + getId();
		cleaningThread = new Thread(task, taskName);
		cleaningThread.start();
	}

	@Override
	public void stopPolling() {
		super.stopPolling();
		poll = false;
		cleaningThread.interrupt();
	}

	@Override
	public void beforeRegistration() {
		LOG.debug("Executing before registration...");
	}

	@Override
	public void beforeCommand(final TestSession session, final HttpServletRequest request,
			final HttpServletResponse response) {
		RequestType type = identifyRequestType(request);
		if (type == START_SESSION) {
			try {
				if (processTestSession(session)) {
					startServerForTestSession(session);
				} else {
					String msg = "Missing target mapping. Available mappings are "
							+ ConfigReader.getInstance().getBrowsers();
					throw new IllegalStateException(msg);
				}
			} catch (Exception e) {
				getRegistry().terminate(session, SessionTerminationReason.CREATIONFAILED);
				LOG.error("Failed creating a session. Root cause :" + e.getMessage(), e);
				throw e;
			}
		}
		super.beforeCommand(session, request, response);
	}

	@Override
	public void afterCommand(final TestSession session, final HttpServletRequest request,
			final HttpServletResponse response) {
		super.afterCommand(session, request, response);
		RequestType type = identifyRequestType(request);
		if (type == STOP_SESSION || (type == START_SESSION && response.getStatus() != HttpServletResponse.SC_OK)) {
			stopServerForTestSession(session);
		}
	}

	@Override
	public JsonObject getStatus() {
		ImmutableMap.Builder<String, Object> value = ImmutableMap.builder();

		// W3C spec
		value.put("ready", true);
		value.put("message", "Node is running");

		value.put("build", ImmutableMap.of("revision", "unknown", "time", "unknown", "version",
				getClass().getPackage().getImplementationVersion() + "&nbsp;&#128123;"));

		value.put("os", ImmutableMap.of("arch", System.getProperty("os.arch"), "name", System.getProperty("os.name"),
				"version", System.getProperty("os.version")));

		value.put("java", ImmutableMap.of("version", System.getProperty("java.version")));

		Map<String, Object> payloadObj = ImmutableMap.of("status", SUCCESS, "value", value.build());

		return new BeanToJsonConverter().convertObject(payloadObj).getAsJsonObject();
	}

	private RequestType identifyRequestType(final HttpServletRequest request) {
		return SeleniumBasedRequest.createFromRequest(request, getRegistry()).extractRequestType();
	}

	private boolean processTestSession(final TestSession session) {
		Map<String, Object> requestedCapabilities = session.getRequestedCapabilities();
		String browser = (String) requestedCapabilities.get(CapabilityType.BROWSER_NAME);
		return ConfigReader.getInstance().getBrowsers().containsKey(browser);
	}

	private void startServerForTestSession(final TestSession session) {
		try {
			SpawnedServer server = SpawnedServer.spawnInstance(session);
			String key = "http://" + server.getHost() + ":" + server.getPort();
			URL url = new URL(key);
			servers.put(key, server);
			((ProxiedTestSlot) session.getSlot()).setRemoteURL(url);
			LOG.debug("Forwarding session to :{}", session.getSlot().getRemoteURL());

		} catch (Exception e) {
			throw new GridException(e.getMessage(), e);
		}
	}

	private void stopServerForTestSession(final TestSession session) {
		try {
			if (session == null) {
				return;
			}
			if (session.getSlot() == null) {
				return;
			}
			URL url = session.getSlot().getRemoteURL();
			if (url == null) {
				return;
			}
			String key = String.format("%s://%s:%d", url.getProtocol(), url.getHost(), url.getPort());
			SpawnedServer localServer = servers.get(key);
			if (localServer != null) {
				localServer.shutdown();
				servers.remove(key);
			}
		} catch (Exception e) {
			LOG.error("Error stopping server...", e);
		}
	}

	@Override
	public void afterSession(TestSession session) {
		super.afterSession(session);
		stopServerForTestSession(session);
	}
}