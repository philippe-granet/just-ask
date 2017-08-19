package com.rationaleemotions.config;

import org.junit.Before;
import org.junit.Test;

import com.rationaleemotions.config.ConfigReader;

public class ConfigReaderTest {

	@Before
	public void setUp() throws Exception {
		System.setProperty("config.file","src/test/resources/just-ask.json");
	}
	
	@Test
	public void testConfig() {
		ConfigReader.getInstance();
	}
}
