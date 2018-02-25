package com.rationaleemotions.servlets;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
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
import org.openqa.selenium.remote.internal.HttpClientFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JustAskServletTest extends BaseServletTestHelper {

	private static final int TIMEOUT_TEN_SECONDS = (int) SECONDS.toMillis(10);

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@BeforeClass
	public static void setUp() throws Exception {
		setUp("src/test/resources/testConfig.json");
	}

	private HttpResponse sendCommand(String method, URL url) throws ClientProtocolException, IOException {
		HttpClientFactory httpClientFactory = new HttpClientFactory(TIMEOUT_TEN_SECONDS, TIMEOUT_TEN_SECONDS);

		BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest(method, url.toExternalForm());
		HttpHost host = new HttpHost(url.getHost(), url.getPort());
		HttpClient client = httpClientFactory.getHttpClient();
		return client.execute(host, request);
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
			
			URL url = new URL(String.format("http://%s:%d/grid/api/testsession?session="+ sessionId.toString(), hub.getConfiguration().host,
					hub.getConfiguration().port));

			HttpResponse response = sendCommand("GET", url);
			assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
			String body = IOUtils.toString(response.getEntity().getContent(),Charset.forName("UTF-8"));
			assertNotNull(body);
			JsonObject json = new JsonParser().parse(body).getAsJsonObject();
			assertTrue(json.getAsJsonObject().toString(), json.getAsJsonObject().get("success").getAsBoolean());
			assertEquals("slot found !", json.getAsJsonObject().get("msg").getAsString());

			assertEquals(sessionId.toString(), json.getAsJsonObject().get("session").getAsString());

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
		versionsUserAgent.put("64.0.3282.140", "64.0.3282.140");
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
		versionsUserAgent.put("58.0.1", "58.0");
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
			
			URL url = new URL(String.format("http://%s:%d/grid/api/testsession?session="+ sessionId.toString(), hub.getConfiguration().host,
					hub.getConfiguration().port));

			HttpResponse response = sendCommand("GET", url);
			assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
			String body = IOUtils.toString(response.getEntity().getContent(),Charset.forName("UTF-8"));
			
			assertNotNull(body);
			JsonObject json = new JsonParser().parse(body).getAsJsonObject();
			assertTrue(json.getAsJsonObject().toString(), json.getAsJsonObject().get("success").getAsBoolean());
			assertEquals("slot found !", json.getAsJsonObject().get("msg").getAsString());

			assertEquals(sessionId.toString(), json.getAsJsonObject().get("session").getAsString());

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
		capabillities.setCapability(CapabilityType.BROWSER_VERSION, "58.0.1");

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