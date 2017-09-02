package org.openqa.grid.internal.utils.configuration;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.grid.common.GridRole;

import com.rationaleemotions.config.ConfigReader;

public class ConfigReaderTest {

	@Before
	public void setUp() throws Exception {

	}

	@Test
	public void testDefaultConfig() {
		String[] hubArgs = { "-role", GridRole.HUB.toString() };
		ConfigReader config = ConfigReader.getInstance(hubArgs);

		Assert.assertEquals(GridRole.HUB.toString(), config.getGridHubConfiguration().role);
		Assert.assertTrue(GridHubConfiguration.DEFAULT_PORT == config.getGridHubConfiguration().port);
		Assert.assertTrue(GridHubConfiguration.DEFAULT_CLEANUP_CYCLE == config.getGridHubConfiguration().cleanUpCycle);
		Assert.assertTrue(GridHubConfiguration.DEFAULT_NEW_SESSION_WAIT_TIMEOUT == config
				.getGridHubConfiguration().newSessionWaitTimeout);
		Assert.assertTrue(
				GridHubConfiguration.DEFAULT_BROWSER_TIMEOUT == config.getGridHubConfiguration().browserTimeout);
		Assert.assertTrue(GridHubConfiguration.DEFAULT_TIMEOUT == config.getGridHubConfiguration().timeout);

		Assert.assertTrue(config.getBrowsers().size() > 0);
	}

	@Test
	public void testCustomConfig() {
		String[] hubArgs = { "-role", GridRole.HUB.toString(), "-hubConfig", "src/test/resources/testConfig.json" };
		ConfigReader config = ConfigReader.getInstance(hubArgs);

		Assert.assertEquals(GridRole.HUB.toString(), config.getGridHubConfiguration().role);
		Assert.assertTrue(1234 == config.getGridHubConfiguration().port);
		Assert.assertTrue(1000 == config.getGridHubConfiguration().cleanUpCycle);
		Assert.assertTrue(GridHubConfiguration.DEFAULT_NEW_SESSION_WAIT_TIMEOUT == config.getGridHubConfiguration().newSessionWaitTimeout);
		Assert.assertTrue(
				GridHubConfiguration.DEFAULT_BROWSER_TIMEOUT == config.getGridHubConfiguration().browserTimeout);
		Assert.assertTrue(15 == config.getGridHubConfiguration().timeout);

		Assert.assertTrue(config.getBrowsers().size() > 0);
	}
}
