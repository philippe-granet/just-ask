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

		Assert.assertTrue(config.getBrowsers().size() > 0);
	}

	@Test
	public void testCustomConfig() {
		String[] hubArgs = { "-role", GridRole.HUB.toString(), "-hubConfig", "src/test/resources/testConfig.json" };
		ConfigReader config = ConfigReader.getInstance(hubArgs);

		Assert.assertTrue(config.getBrowsers().size() > 0);
	}
}
