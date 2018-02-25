package org.openqa.grid.internal.utils.configuration;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.grid.common.GridRole;
import org.openqa.grid.e2e.utils.GridTestHelper;
import org.openqa.grid.selenium.GridLauncherV3;
import org.openqa.selenium.net.PortProber;

import com.rationaleemotions.config.ConfigReader;
import com.rationaleemotions.servlets.ServerHelper;

public class ConfigReaderTest {

	@Before
	public void setUp() throws Exception {

	}

	@Test
	public void testDefaultConfig() throws Exception {
		Integer hubPort = PortProber.findFreePort();
		String[] hubArgs = { "-role", GridRole.HUB.toString(), "-port", hubPort.toString() };
		GridLauncherV3.main(hubArgs);
		GridTestHelper.waitForGrid();

		ConfigReader config = ConfigReader.getInstance();

		Assert.assertTrue(config.getBrowsers().size() > 0);
		try {
			ServerHelper.getHubRegistry().getHub().stop();
		} catch (Exception e) {
			// TODO: handle exception
		}
		try {
			ServerHelper.getHubRegistry().stop();
		} catch (Exception e) {
			// TODO: handle exception
		}
		try {
			ServerHelper.getServer().stop();
		} catch (Exception e) {
			// TODO: handle exception
		}
		try {
			ServerHelper.getServer().destroy();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	@Test
	public void testCustomConfig() throws Exception {
		Integer hubPort = PortProber.findFreePort();
		String[] hubArgs = { "-role", GridRole.HUB.toString(), "-port", hubPort.toString(), "-hubConfig",
				"src/test/resources/testConfig.json" };
		GridLauncherV3.main(hubArgs);
		GridTestHelper.waitForGrid();

		ConfigReader config = ConfigReader.getInstance();

		Assert.assertTrue(config.getBrowsers().size() > 0);
		try {
			ServerHelper.getHubRegistry().getHub().stop();
		} catch (Exception e) {
			// TODO: handle exception
		}
		try {
			ServerHelper.getHubRegistry().stop();
		} catch (Exception e) {
			// TODO: handle exception
		}
		try {
			ServerHelper.getServer().stop();
		} catch (Exception e) {
			// TODO: handle exception
		}
		try {
			ServerHelper.getServer().destroy();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}
