package com.rationaleemotions.servlets;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.openqa.grid.common.RegistrationRequest;
import org.openqa.grid.common.exception.GridConfigurationException;
import org.openqa.grid.common.exception.GridException;
import org.openqa.grid.internal.ExternalSessionKey;
import org.openqa.grid.internal.Registry;
import org.openqa.grid.internal.RemoteProxy;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.internal.TestSlot;
import org.openqa.grid.internal.utils.configuration.GridHubConfiguration;
import org.openqa.grid.web.servlet.RegistryBasedServlet;
import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.internal.HttpClientFactory;
import org.seleniumhq.jetty9.servlet.FilterHolder;
import org.seleniumhq.jetty9.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.rationaleemotions.config.BrowserInfo;
import com.rationaleemotions.config.BrowserVersionInfo;
import com.rationaleemotions.config.ConfigReader;
import com.rationaleemotions.server.DockerBasedSeleniumServer;

import net.bull.javamelody.MonitoringFilter;
import net.bull.javamelody.Parameter;

/**
 * Represents a simple servlet that needs to be invoked in order to wire in our
 * ghost node which will act as a proxy for all proxies.
 */
public class JustAskServlet extends RegistryBasedServlet {

	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static final int TIMEOUT_TEN_SECONDS = (int) SECONDS.toMillis(10);

	private static final String SUCCESS = "success";
	private static final String SESSION = "session";

	static {
		new EnrollServletPoller().start();
	}

	public JustAskServlet(final Registry registry) {
		super(registry);
	}

	public JustAskServlet() {
		this(null);
	}

	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {
		if (req.getParameterMap().containsKey(SESSION)) {
			retrieveSessionInformations(req, resp);
		} else {
			addProxy();
		}
	}

	protected void retrieveSessionInformations(final HttpServletRequest request, final HttpServletResponse response) {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setStatus(HttpServletResponse.SC_OK);
		JsonObject res;
		try {
			res = getJsonSessionInformations(request);
			response.getWriter().print(res);
			response.getWriter().close();
		} catch (JsonSyntaxException | IOException e) {
			throw new GridException(e.getMessage(), e);
		}
	}

	private void addProxy() {
		// After the construction is finished, lets wrap up.
		int status;

		HttpClientFactory httpClientFactory = new HttpClientFactory(TIMEOUT_TEN_SECONDS, TIMEOUT_TEN_SECONDS);
		try {
			final int port = getRegistry().getHub().getConfiguration().port;
			String hubHost = getRegistry().getHub().getConfiguration().host;
			final URL registration = new URL(String.format("http://%s:%d/grid/register", hubHost, port));
			BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("POST",
					registration.toExternalForm());
			request.setEntity(getJsonAsEntity(hubHost, port));
			HttpHost host = new HttpHost(registration.getHost(), registration.getPort());
			HttpClient client = httpClientFactory.getHttpClient();
			HttpResponse response = client.execute(host, request);
			status = response.getStatusLine().getStatusCode();

		} catch (IOException | GridConfigurationException e) {
			throw new GridException(e.getMessage(), e);
		} finally {
			httpClientFactory.close();
		}
		Preconditions.checkState(status == HttpStatus.SC_OK, "There was a problem in hooking in the ghost node.");
	}

	private StringEntity getJsonAsEntity(final String host, final int port) throws UnsupportedEncodingException {
		try {
			InputStream isr = Thread.currentThread().getContextClassLoader().getResourceAsStream("ondemand.json");
			String result = IOUtils.toString(new InputStreamReader(isr));
			JsonObject ondemand = new JsonParser().parse(result).getAsJsonObject();
			int maxSession = ConfigReader.getInstance().getMaxSession();
			JsonArray capsArray = new JsonArray();
			ondemand.add("capabilities", capsArray);

			Map<String, BrowserInfo> browsers = ConfigReader.getInstance().getBrowsers();
			for (Entry<String, BrowserInfo> browser : browsers.entrySet()) {
				BrowserInfo browserInfo = browser.getValue();
				String browserName = browserInfo.getBrowser();
				for (BrowserVersionInfo browserVersion : browserInfo.getVersions()) {
					JsonObject jsonObject = new JsonObject();
					jsonObject.addProperty(CapabilityType.BROWSER_NAME, browserName);
					jsonObject.addProperty(CapabilityType.VERSION, browserVersion.getVersion());
					jsonObject.addProperty(RegistrationRequest.MAX_INSTANCES, maxSession);

					Class<?> clazz = Class.forName(browserVersion.getImplementation());
					if (clazz.isAssignableFrom(DockerBasedSeleniumServer.class)) {
						jsonObject.addProperty(CapabilityType.PLATFORM, Platform.LINUX.toString());
					} else {
						jsonObject.addProperty(CapabilityType.PLATFORM, Platform.ANY.toString());
					}
					capsArray.add(jsonObject);
				}
			}
			JsonObject configuration = ondemand.get("configuration").getAsJsonObject();

			GridHubConfiguration gridHubConfiguration = getRegistry().getHub().getConfiguration();

			configuration.addProperty("maxSession", maxSession);
			configuration.addProperty("browserTimeout", gridHubConfiguration.browserTimeout);
			configuration.addProperty("timeout", gridHubConfiguration.timeout);
			configuration.addProperty("enablePassThrough", gridHubConfiguration.enablePassThrough);
			configuration.addProperty("hub", String.format("http://%s:%d", host, port));
			result = ondemand.toString();
			return new StringEntity(result);
		} catch (IOException | ClassNotFoundException e) {
			throw new GridException(e.getMessage(), e);
		}
	}

	private static class EnrollServletPoller extends Thread {

		private static long sleepTimeBetweenChecks = 500;

		protected long getSleepTimeBetweenChecks() {
			return sleepTimeBetweenChecks;
		}

		@Override
		public void run() {
			Registry registry = null;

			while (true) {
				try {
					Thread.sleep(getSleepTimeBetweenChecks());
				} catch (InterruptedException e) {
					LOG.warn("Interrupted!", e);
					Thread.currentThread().interrupt();
				}
				if (registry == null) {
					registry = ServerHelper.getHubRegistry();
				}
				if(registry!=null){
					ServletContextHandler handler = ServerHelper.getServletContextHandler();
					if (handler != null) {
						addJavaMelodyMonitoringFilter(handler);
						if (callServletToRegister(registry)) {
							return;
						}
					}
				}
			}
		}

		private boolean callServletToRegister(Registry registry) {
			if (registry == null) {
				return false;
			}

			HttpClientFactory httpClientFactory = new HttpClientFactory(TIMEOUT_TEN_SECONDS, TIMEOUT_TEN_SECONDS);
			try {
				GridHubConfiguration gridHubConfiguration = registry.getHub().getConfiguration();
				final URL enrollServletEndpoint = new URL(String.format("http://%s:%d/grid/admin/%s",
						gridHubConfiguration.host, gridHubConfiguration.port, JustAskServlet.class.getSimpleName()));

				BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("GET",
						enrollServletEndpoint.toExternalForm());
				HttpHost host = new HttpHost(enrollServletEndpoint.getHost(), enrollServletEndpoint.getPort());
				HttpClient client = httpClientFactory.getHttpClient();
				HttpResponse response = client.execute(host, request);

				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					return true;
				}

			} catch (IOException e) {
				LOG.error("Error calling enroll servlet : " + e.getMessage());
			} finally {
				httpClientFactory.close();
			}
			return false;
		}

		private void addJavaMelodyMonitoringFilter(final ServletContextHandler root) {
			// Add JavaMelody monitoring filter
			final MonitoringFilter monitoringFilter = new MonitoringFilter();
			monitoringFilter.setApplicationType("Standalone");
			final FilterHolder filterHolder = new FilterHolder(monitoringFilter);
			final Map<Parameter, String> parameters = initJavaMelodyParameters();
			for (final Map.Entry<Parameter, String> entry : parameters.entrySet()) {
				final Parameter parameter = entry.getKey();
				final String value = entry.getValue();
				filterHolder.setInitParameter(parameter.getCode(), value);
			}
			root.addFilter(filterHolder, "/*", EnumSet.of(DispatcherType.INCLUDE, DispatcherType.REQUEST));
		}

		private Map<Parameter, String> initJavaMelodyParameters() {
			final Map<Parameter, String> parameters = new EnumMap<>(Parameter.class);
			parameters.put(Parameter.SAMPLING_SECONDS, "10");
			parameters.put(Parameter.UPDATE_CHECK_DISABLED, "true");
			parameters.put(Parameter.NO_DATABASE, "true");

			// set the path of the reports:
			parameters.put(Parameter.MONITORING_PATH, "/grid/monitoring");
			return parameters;
		}

	}

	private JsonObject getJsonSessionInformations(final HttpServletRequest request) throws IOException {
		JsonObject requestJSON = null;
		if (request.getInputStream() != null) {
			String json;
			try (Reader rd = new BufferedReader(new InputStreamReader(request.getInputStream()))) {
				json = CharStreams.toString(rd);
			}
			if (!"".equals(json)) {
				requestJSON = new JsonParser().parse(json).getAsJsonObject();
			}
		}

		JsonObject res = new JsonObject();
		res.addProperty(SUCCESS, false);

		// the id can be specified via a param, or in the json request.
		String session;
		if (requestJSON == null) {
			session = request.getParameter(SESSION);
		} else {
			if (!requestJSON.has(SESSION)) {
				res.addProperty("msg",
						"you need to specify at least a session or internalKey when call the test slot status service.");
				return res;
			}
			session = requestJSON.get(SESSION).getAsString();
		}

		TestSession testSession = getRegistry().getHub().getRegistry()
				.getSession(ExternalSessionKey.fromString(session));

		if (testSession == null) {
			res.addProperty("msg", "Cannot find test slot running session " + session + " in the registry.");
			return res;
		}
		res.addProperty("msg", "slot found !");
		res.remove(SUCCESS);
		res.addProperty(SUCCESS, true);
		res.addProperty(SESSION, testSession.getExternalKey().getKey());
		res.addProperty("internalKey", testSession.getInternalKey());
		res.addProperty("inactivityTime", testSession.getInactivityTime());
		TestSlot testSlot = testSession.getSlot();
		res.addProperty("remoteUrl", testSlot.getRemoteURL().toExternalForm());
		res.addProperty("lastSessionStart", testSlot.getLastSessionStart());
		Gson gson = new GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting().create();
		res.add("capabilities", gson.toJsonTree(testSlot.getCapabilities()));
		RemoteProxy p = testSlot.getProxy();
		res.addProperty("proxyId", p.getId());
		return res;
	}
}