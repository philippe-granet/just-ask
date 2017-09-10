package com.rationaleemotions.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openqa.grid.internal.SessionTerminationReason;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.openqa.testing.FakeHttpServletResponse;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JustAskServletTest extends BaseServletTestHelper {
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@BeforeClass
	public static void setUp() throws Exception {
		setUp("src/test/resources/testConfig.json");
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
	public void testSessionTimeout() throws IOException, ServletException, URISyntaxException, InterruptedException {
		DesiredCapabilities capabillities = DesiredCapabilities.chrome();
		WebDriver driver = null;
		try {
			driver = new RemoteWebDriver(new URL(
					String.format("http://%s:%d/wd/hub", hub.getConfiguration().host, hub.getConfiguration().port)),
					capabillities);
			driver.get("https://www.google.com/");
			SessionId sessionId = ((RemoteWebDriver) driver).getSessionId();

			Thread.sleep(20000);

			thrown.expect(WebDriverException.class);
			thrown.expectMessage(String.format("Session [%s] was terminated due to %s", sessionId,
					SessionTerminationReason.TIMEOUT));

		} finally {
			if (driver != null) {
				driver.quit();
			}
		}
	}

	@Test
	public void testChromeBrowser() throws IOException, ServletException, URISyntaxException {
		DesiredCapabilities capabilities = DesiredCapabilities.chrome();
		WebDriver driver = null;
		try {
			driver = new RemoteWebDriver(new URL(
					String.format("http://%s:%d/wd/hub", hub.getConfiguration().host, hub.getConfiguration().port)),
					capabilities);
			driver.get("https://www.google.com/");
			assertEquals("Google", driver.getTitle());

			SessionId sessionId = ((RemoteWebDriver) driver).getSessionId();
			FakeHttpServletResponse response = sendCommand("GET", "/?session=" + sessionId.toString());
			assertEquals(HttpServletResponse.SC_OK, response.getStatus());
			assertNotNull(response.getBody());
			JsonObject json = new JsonParser().parse(response.getBody()).getAsJsonObject();
			assertTrue(json.getAsJsonObject().toString(),json.getAsJsonObject().get("success").getAsBoolean());
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
	public void testChromeBrowserVersions() throws IOException, ServletException, URISyntaxException {
		DesiredCapabilities capabillities = DesiredCapabilities.chrome();
		WebDriver driver = null;
		Map<String, String> versionsUserAgent = new HashMap<>();
		versionsUserAgent.put("58.0.3029.81", "58.0.3029.81");
		versionsUserAgent.put("59.0.3071.115", "59.0.3071.115");
		versionsUserAgent.put("60.0.3112.113", "60.0.3112.113");
		versionsUserAgent.put("61.0.3163.79", "61.0.3163.79");
		for (Map.Entry<String, String> versionUserAgent : versionsUserAgent.entrySet()) {
			try {
				capabillities.setCapability(CapabilityType.BROWSER_VERSION, versionUserAgent.getKey());
				driver = new RemoteWebDriver(new URL(
						String.format("http://%s:%d/wd/hub", hub.getConfiguration().host, hub.getConfiguration().port)),
						capabillities);
				driver.get("http://www.browser-info.net/");
				File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
				FileUtils.copyFile(scrFile, new File("./target/chrome-" + versionUserAgent.getValue() + ".png"));
				List<WebElement> list = driver
						.findElements(By.xpath("//*[contains(text(),'" + versionUserAgent.getValue() + "')]"));
				Assert.assertTrue(versionUserAgent.getValue() + " not found!", list.size() > 0);

			} finally {
				if (driver != null) {
					driver.quit();
				}
			}
		}
	}

	@Test
	public void testFirefoxBrowserVersions() throws IOException, ServletException, URISyntaxException {
		DesiredCapabilities capabillities = DesiredCapabilities.firefox();
		WebDriver driver = null;
		Map<String, String> versionsUserAgent = new HashMap<>();
		versionsUserAgent.put("52.0.2", "52.0");
		versionsUserAgent.put("53.0", "53.0");
		versionsUserAgent.put("54.0", "54.0");
		versionsUserAgent.put("55.0.3", "55.0");
		for (Map.Entry<String, String> versionUserAgent : versionsUserAgent.entrySet()) {
			try {
				capabillities.setCapability(CapabilityType.BROWSER_VERSION, versionUserAgent.getKey());
				driver = new RemoteWebDriver(new URL(
						String.format("http://%s:%d/wd/hub", hub.getConfiguration().host, hub.getConfiguration().port)),
						capabillities);
				driver.get("http://www.browser-info.net/");
				File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
				FileUtils.copyFile(scrFile, new File("./target/firefox-" + versionUserAgent.getValue() + ".png"));
				List<WebElement> list = driver
						.findElements(By.xpath("//*[contains(text(),'" + versionUserAgent.getValue() + "')]"));
				Assert.assertTrue(versionUserAgent.getValue() + " not found!", list.size() > 0);

			} finally {
				if (driver != null) {
					driver.quit();
				}
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
			assertTrue(json.getAsJsonObject().toString(),json.getAsJsonObject().get("success").getAsBoolean());
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

}