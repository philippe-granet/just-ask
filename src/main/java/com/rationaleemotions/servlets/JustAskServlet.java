package com.rationaleemotions.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

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

/**
 * Represents a simple servlet that needs to be invoked in order to wire in our
 * ghost node which will act as a proxy for all proxies.
 */
public class JustAskServlet extends RegistryBasedServlet {

	private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static String hubHost;

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
		if (req.getParameterMap().containsKey("session")) {
			retrieveSessionInformations(req, resp);
		} else {
			addProxy();
		}
	}

	protected void retrieveSessionInformations(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setStatus(200);
		JsonObject res;
		try {
			res = getResponse(request);
			response.getWriter().print(res);
			response.getWriter().close();
		} catch (JsonSyntaxException e) {
			throw new GridException(e.getMessage());
		}
	}

	private void addProxy() {
		// After the construction is finished, lets wrap up.
		int status;

		HttpClientFactory httpClientFactory = new HttpClientFactory();
		try {
			final int port = getRegistry().getHub().getConfiguration().port;
			hubHost = getRegistry().getHub().getConfiguration().host;
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
			String string = IOUtils.toString(new InputStreamReader(isr));
			JsonObject ondemand = new JsonParser().parse(string).getAsJsonObject();
			int maxSession = ConfigReader.getInstance().getMaxSession();
			// JsonArray capsArray =
			// ondemand.get("capabilities").getAsJsonArray();
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

					if (browserVersion.getImplementation().equals(DockerBasedSeleniumServer.class.getName())) {
						jsonObject.addProperty(CapabilityType.PLATFORM, Platform.LINUX.toString());
					} else {
						jsonObject.addProperty(CapabilityType.PLATFORM, Platform.ANY.toString());
					}
					capsArray.add(jsonObject);
				}
			}
			JsonObject configuration = ondemand.get("configuration").getAsJsonObject();
			configuration.addProperty("maxSession", maxSession);
			configuration.addProperty("hub", String.format("http://%s:%d", host, port));
			string = ondemand.toString();
			return new StringEntity(string);
		} catch (IOException e) {
			throw new GridException(e.getMessage(), e);
		}
	}

	public static String getHubHost() {
		return hubHost;
	}

	private static class EnrollServletPoller extends Thread {

		private static long sleepTimeBetweenChecks = 500;
		private static GridHubConfiguration gridHubConfiguration = ConfigReader.getGridHubConfiguration();

		protected long getSleepTimeBetweenChecks() {
			return sleepTimeBetweenChecks;
		}

		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(getSleepTimeBetweenChecks());
				} catch (InterruptedException e) {
					return;
				}
				HttpClientFactory httpClientFactory = new HttpClientFactory();
				try {
					final URL enrollServletEndpoint = new URL(
							String.format("http://%s:%d/grid/admin/%s", gridHubConfiguration.host,
									gridHubConfiguration.port, JustAskServlet.class.getSimpleName()));

					BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("GET",
							enrollServletEndpoint.toExternalForm());
					HttpHost host = new HttpHost(enrollServletEndpoint.getHost(), enrollServletEndpoint.getPort());
					HttpClient client = httpClientFactory.getHttpClient();
					HttpResponse response = client.execute(host, request);

					if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
						return;
					}

				} catch (IOException e) {
					LOG.error(e.getMessage(), e);
				} finally {
					httpClientFactory.close();
				}
			}
		}
	}

	private JsonObject getResponse(HttpServletRequest request) throws IOException {
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
		res.addProperty("success", false);

		// the id can be specified via a param, or in the json request.
		String session;
		if (requestJSON == null) {
			session = request.getParameter("session");
		} else {
			if (!requestJSON.has("session")) {
				res.addProperty("msg",
						"you need to specify at least a session or internalKey when call the test slot status service.");
				return res;
			}
			session = requestJSON.get("session").getAsString();
		}

		TestSession testSession = getRegistry().getSession(ExternalSessionKey.fromString(session));

		if (testSession == null) {
			res.addProperty("msg", "Cannot find test slot running session " + session + " in the registry.");
			return res;
		}
		res.addProperty("msg", "slot found !");
		res.remove("success");
		res.addProperty("success", true);
		res.addProperty("session", testSession.getExternalKey().getKey());
		res.addProperty("internalKey", testSession.getInternalKey());
		res.addProperty("inactivityTime", testSession.getInactivityTime());
		TestSlot testSlot = testSession.getSlot();
		res.addProperty("remoteUrl", testSlot.getRemoteURL().toExternalForm());
		res.addProperty("lastSessionStart", testSlot.getLastSessionStart());
		Gson gson = new GsonBuilder().enableComplexMapKeySerialization()
		        .setPrettyPrinting().create();
		res.add("capabilities", gson.toJsonTree(testSlot.getCapabilities()));
		RemoteProxy p = testSlot.getProxy();
		res.addProperty("proxyId", p.getId());
		return res;
	}
}