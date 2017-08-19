package com.rationaleemotions.config;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A simple POJO that represents the mapping
 */
public class BrowserInfo {
	private String browser;
	private String defaultVersion;
	private List<BrowserVersionInfo> versions;

    public String getBrowser() {
        return browser;
    }

	public String getDefaultVersion() {
		return defaultVersion;
	}

    public List<BrowserVersionInfo> getVersions() {
        return Collections.unmodifiableList(new LinkedList<BrowserVersionInfo>(versions));
    }

    @Override
    public String toString() {
        return String.format("BrowserInfo{browser='%s', defaultVersion='%s', versions='%s'}",
            browser,defaultVersion,versions);
    }
}
