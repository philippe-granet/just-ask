package com.rationaleemotions.servlets;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.junit.AfterClass;
import org.openqa.grid.common.GridRole;
import org.openqa.grid.e2e.utils.GridTestHelper;
import org.openqa.grid.e2e.utils.RegistryTestHelper;
import org.openqa.grid.internal.Registry;
import org.openqa.grid.internal.utils.configuration.GridHubConfiguration;
import org.openqa.grid.web.Hub;
import org.openqa.selenium.net.PortProber;
import org.openqa.selenium.net.UrlChecker;
import org.openqa.selenium.remote.server.log.TerseFormatter;
import org.openqa.testing.FakeHttpServletRequest;
import org.openqa.testing.FakeHttpServletResponse;
import org.openqa.testing.UrlInfo;
import org.seleniumhq.jetty9.server.handler.ContextHandler;

import com.google.gson.JsonObject;
import com.rationaleemotions.config.ConfigReader;

public class BaseServletTestHelper {
	private static final String BASE_URL = "http://localhost:4444";
	private static final String CONTEXT_PATH = "/";

	protected static HttpServlet servlet;
	static Hub hub;

	public static void setUp(String configFile) throws Exception {
		Integer hubPort = PortProber.findFreePort();
		String[] hubArgs = { "-role", GridRole.HUB.toString(), "-port", hubPort.toString(), "-hubConfig",
				configFile };
		ConfigReader.getInstance(hubArgs);

		GridHubConfiguration gridHubConfiguration = GridHubConfiguration.loadFromJSON(configFile);
		gridHubConfiguration.port = PortProber.findFreePort();

		Level logLevel = Level.INFO;
		Logger.getLogger("").setLevel(logLevel);

		for (Handler handler : Logger.getLogger("").getHandlers()) {
			if (handler instanceof ConsoleHandler) {
				handler.setLevel(logLevel);
				handler.setFormatter(new TerseFormatter());
			}
		}
		Logger.getLogger("org.glassfish").setLevel(Level.WARNING);
		Logger.getLogger("org.apache").setLevel(Level.WARNING);
		Logger.getLogger("net.bull").setLevel(Level.WARNING);
		Logger.getLogger("org.seleniumhq.jetty9").setLevel(Level.WARNING);

		hub = GridTestHelper.getHub(gridHubConfiguration);
		Registry registry = Registry.newInstance(hub, hub.getConfiguration());
		servlet = new JustAskServlet() {
			@Override
			public ServletContext getServletContext() {
				final ContextHandler.Context servletContext = new ContextHandler().getServletContext();
				servletContext.setAttribute(Registry.KEY, registry);
				return servletContext;
			}
		};
		servlet.init();
		UrlChecker urlChecker = new UrlChecker();
		urlChecker.waitUntilAvailable(10, TimeUnit.SECONDS, new URL(
				String.format("http://%s:%d/grid/console", hub.getConfiguration().host, hub.getConfiguration().port)));
		sendCommand("GET", "/");
		RegistryTestHelper.waitForNodes(registry);
	}

	@AfterClass
	public static void teardown() throws Exception {
		hub.stop();
		servlet.destroy();
	}

	protected static UrlInfo createUrl(String path) {
		return new UrlInfo(BASE_URL, CONTEXT_PATH, path);
	}

	protected static FakeHttpServletResponse sendCommand(String method, String commandPath)
			throws IOException, ServletException, URISyntaxException {
		return sendCommand(method, commandPath, null);
	}

	protected static FakeHttpServletResponse sendCommand(String method, String commandPath, JsonObject parameters)
			throws IOException, ServletException, URISyntaxException {
		UrlInfo urlInfo = createUrl(commandPath);
		FakeHttpServletRequest request = new FakeHttpServletRequest(method, urlInfo);
		if (parameters != null) {
			request.setBody(parameters.toString());
		}
		URIBuilder uriBuilder = new URIBuilder(urlInfo.toString());
		List<NameValuePair> urlParameters = uriBuilder.getQueryParams();

		Map<String, String> urlParams = new HashMap<>();
		for (NameValuePair nameValuePair : urlParameters) {
			urlParams.put(nameValuePair.getName(), nameValuePair.getValue());
		}
		request.setParameters(urlParams);
		FakeHttpServletResponse response = new FakeHttpServletResponse();
		servlet.service(request, response);
		return response;
	}
}