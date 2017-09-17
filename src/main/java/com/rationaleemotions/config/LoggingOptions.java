package com.rationaleemotions.config;

import java.util.logging.Level;

public class LoggingOptions {

	public static Level getDefaultLogLevel() {
		final String logLevelProperty = System.getProperty("just-ask.LOGGER.level");
		if (null == logLevelProperty) {
			return org.openqa.selenium.remote.server.log.LoggingOptions.getDefaultLogLevel();
		}
		return Level.parse(logLevelProperty);
	}
}