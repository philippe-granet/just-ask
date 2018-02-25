package com.rationaleemotions.servlets;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

public class NewSessionTimeoutTest extends BaseServletTestHelper {
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@BeforeClass
	public static void setUp() throws Exception {
		setUp("src/test/resources/newSessionTimeout.json");
	}
	
	@Test
	public void testNewSessionTimeout() throws IOException, ServletException, URISyntaxException {
		DesiredCapabilities capabilities = DesiredCapabilities.chrome();
		WebDriver driver1 = null;
		WebDriver driver2 = null;
		try {
			driver1 = new RemoteWebDriver(new URL(
					String.format("http://%s:%d/wd/hub", hub.getConfiguration().host, hub.getConfiguration().port)),
					capabilities);
			driver1.get("https://www.google.com/");
			
			thrown.expect(WebDriverException.class);
			thrown.expectMessage("Request timed out waiting for a node to become available.");
			driver2 = new RemoteWebDriver(new URL(
					String.format("http://%s:%d/wd/hub", hub.getConfiguration().host, hub.getConfiguration().port)),
					capabilities);

		} finally {
			if (driver1 != null) {
				driver1.quit();
			}
			if (driver2 != null) {
				driver2.quit();
			}
		}
	}
}