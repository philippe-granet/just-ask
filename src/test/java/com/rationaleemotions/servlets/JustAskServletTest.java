package com.rationaleemotions.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.grid.common.GridRole;
import org.openqa.grid.e2e.utils.GridTestHelper;
import org.openqa.grid.e2e.utils.RegistryTestHelper;
import org.openqa.grid.internal.Registry;
import org.openqa.grid.web.Hub;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.net.PortProber;
import org.openqa.selenium.net.UrlChecker;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.openqa.testing.FakeHttpServletResponse;
import org.seleniumhq.jetty9.server.handler.ContextHandler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rationaleemotions.config.ConfigReader;

public class JustAskServletTest extends BaseServletTest {
	static Hub hub;

	@BeforeClass
	public static void setUp() throws Exception {
		Integer hubPort = PortProber.findFreePort();
		String[] hubArgs = { "-role", GridRole.HUB.toString(), "-port", hubPort.toString(), "-hubConfig",
				"src/test/resources/testConfig.json" };
		ConfigReader config = ConfigReader.getInstance(hubArgs);

		hub = GridTestHelper.getHub(config.getGridHubConfiguration());
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

	@Test
	public void testEnrollResponse() throws IOException, ServletException, URISyntaxException {
		FakeHttpServletResponse response = sendCommand("GET", "/");
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		assertNotNull(response.getBody());
	}

	@Test
	public void testRetrieveUnknownSessionInformationsResponse()
			throws IOException, ServletException, URISyntaxException {
		FakeHttpServletResponse response = sendCommand("GET", "/?session=123456789");
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		assertNotNull(response.getBody());
		JsonObject json = new JsonParser().parse(response.getBody()).getAsJsonObject();
		assertFalse(json.getAsJsonObject().get("success").getAsBoolean());
		assertEquals("Cannot find test slot running session 123456789 in the registry.",
				json.getAsJsonObject().get("msg").getAsString());
	}

	@Test
	public void testChromeBrowser() throws IOException, ServletException, URISyntaxException {
		DesiredCapabilities capabillities = DesiredCapabilities.chrome();
		WebDriver driver = null;
		try {
			driver = new RemoteWebDriver(new URL(
					String.format("http://%s:%d/wd/hub", hub.getConfiguration().host, hub.getConfiguration().port)),
					capabillities);
			driver.get("https://www.google.com/");
			assertEquals("Google", driver.getTitle());

			SessionId sessionId = ((RemoteWebDriver) driver).getSessionId();
			FakeHttpServletResponse response = sendCommand("GET", "/?session=" + sessionId.toString());
			assertEquals(HttpServletResponse.SC_OK, response.getStatus());
			assertNotNull(response.getBody());
			JsonObject json = new JsonParser().parse(response.getBody()).getAsJsonObject();
			assertTrue(json.getAsJsonObject().get("success").getAsBoolean());
			assertEquals("slot found !", json.getAsJsonObject().get("msg").getAsString());

			assertEquals(sessionId.toString(), json.getAsJsonObject().get("session").getAsString());
			assertNotNull(json.getAsJsonObject().get("remoteUrl"));
		} finally {
			if (driver != null) {
				driver.quit();
			}
		}
	}

	@Test
	public void testFirefoxBrowser() throws IOException, ServletException, URISyntaxException {
		DesiredCapabilities capabillities = DesiredCapabilities.firefox();
		WebDriver driver = null;
		try {
			driver = new RemoteWebDriver(new URL(
					String.format("http://%s:%d/wd/hub", hub.getConfiguration().host, hub.getConfiguration().port)),
					capabillities);
			driver.get("https://www.google.com/");
			assertEquals("Google", driver.getTitle());

			SessionId sessionId = ((RemoteWebDriver) driver).getSessionId();
			FakeHttpServletResponse response = sendCommand("GET", "/?session=" + sessionId.toString());
			assertEquals(HttpServletResponse.SC_OK, response.getStatus());
			assertNotNull(response.getBody());
			JsonObject json = new JsonParser().parse(response.getBody()).getAsJsonObject();
			assertTrue(json.getAsJsonObject().get("success").getAsBoolean());
			assertEquals("slot found !", json.getAsJsonObject().get("msg").getAsString());

			assertEquals(sessionId.toString(), json.getAsJsonObject().get("session").getAsString());
			assertNotNull(json.getAsJsonObject().get("remoteUrl"));

		} finally {
			if (driver != null) {
				driver.quit();
			}
		}
	}

	@Test
	public void testBrowserCapabilities() throws IOException, ServletException, URISyntaxException {
		DesiredCapabilities capabillities = DesiredCapabilities.firefox();
		capabillities.setCapability("DOCKER_ENV_SCREEN_WIDTH", "640");
		capabillities.setCapability("DOCKER_ENV_SCREEN_HEIGHT", "480");
		capabillities.setCapability("DOCKER_ENV_SCREEN_DEPTH", "24");
		capabillities.setCapability("DOCKER_ENV_TZ", "Europe/Paris");
		capabillities.setCapability(CapabilityType.BROWSER_VERSION, "54.0");

		WebDriver driver = null;
		try {
			driver = new RemoteWebDriver(new URL(
					String.format("http://%s:%d/wd/hub", hub.getConfiguration().host, hub.getConfiguration().port)),
					capabillities);
			driver.get("https://www.google.com/");
			assertEquals("Google", driver.getTitle());

			String screenSize = (String) ((JavascriptExecutor) driver)
					.executeScript("return screen.width + 'x' + screen.height;");
			assertEquals("640x480", screenSize);
			
			String timezone = (String) ((JavascriptExecutor) driver)
					.executeScript("return Intl.DateTimeFormat().resolvedOptions().timeZone;");
			assertEquals("Europe/Paris", timezone);
			
		} finally {
			if (driver != null) {
				driver.quit();
			}
		}
	}

	@AfterClass
	public static void teardown() throws Exception {
		hub.stop();
		servlet.destroy();
	}
}