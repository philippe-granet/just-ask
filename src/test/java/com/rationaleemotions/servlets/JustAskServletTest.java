package com.rationaleemotions.servlets;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.grid.common.GridRole;
import org.openqa.grid.e2e.utils.GridTestHelper;
import org.openqa.grid.internal.Registry;
import org.openqa.grid.web.Hub;
import org.openqa.selenium.net.PortProber;
import org.openqa.selenium.net.UrlChecker;
import org.openqa.testing.FakeHttpServletResponse;
import org.seleniumhq.jetty9.server.handler.ContextHandler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rationaleemotions.config.ConfigReader;

public class JustAskServletTest extends BaseServletTest {
	Hub hub;

	@Before
	public void setUp() throws Exception {
		Integer hubPort = PortProber.findFreePort();
		String[] hubArgs = { "-role", GridRole.HUB.toString(), "-port", hubPort.toString() };
		ConfigReader config = ConfigReader.getInstance(hubArgs);

		hub = GridTestHelper.getHub(config.getGridHubConfiguration());

		servlet = new JustAskServlet() {
			@Override
			public ServletContext getServletContext() {
				final ContextHandler.Context servletContext = new ContextHandler().getServletContext();
				try {
					servletContext.setAttribute(Registry.KEY, Registry.newInstance(hub, hub.getConfiguration()));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				return servletContext;
			}
		};
		servlet.init();
		UrlChecker urlChecker = new UrlChecker();
		urlChecker.waitUntilAvailable(10, TimeUnit.SECONDS, new URL(
				String.format("http://%s:%d/grid/console", hub.getConfiguration().host, hub.getConfiguration().port)));
	}

	@Test
	public void testEnrollResponse() throws IOException, ServletException, URISyntaxException {
		FakeHttpServletResponse response = sendCommand("GET", "/");
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		assertNotNull(response.getBody());
	}
	
	@Test
	public void testretrieveSessionInformationsResponse() throws IOException, ServletException, URISyntaxException {
		FakeHttpServletResponse response = sendCommand("GET", "/?session=123456789");
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		assertNotNull(response.getBody());
		JsonObject json = new JsonParser().parse(response.getBody()).getAsJsonObject();
		assertFalse(json.getAsJsonObject().get("success").getAsBoolean());
		assertFalse(json.getAsJsonObject().get("success").getAsBoolean());
		assertEquals("Cannot find test slot running session 123456789 in the registry.",json.getAsJsonObject().get("msg").getAsString());
		
	}

	@After
	public void teardown() throws Exception {
		hub.stop();
		servlet.destroy();
	}
}