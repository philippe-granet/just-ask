package com.rationaleemotions.config;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.SystemUtils;
import org.openqa.grid.common.JSONConfigurationUtils;
import org.openqa.grid.internal.utils.configuration.GridHubConfiguration;
import org.openqa.selenium.net.NetworkUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.rationaleemotions.server.DockerHelper;
import com.rationaleemotions.servlets.ServerHelper;

/**
 * A singleton instance that works as a configuration data source.
 */
public class ConfigReader {
	public static final String DEFAULT_CONFIG_FILE = "defaults/just-ask.json";

	private Configuration configuration;
	private String dockerRestApiUri;
	
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * @return - A {@link ConfigReader} that represents the configuration.
	 */
	public static ConfigReader getInstance() {
		return ReaderInstance.instance;
	}

	/**
	 * @return - The docker rest api uri.
	 */
	public String getDockerRestApiUri() {
		if (configuration == null) {
			return null;
		}
		if(dockerRestApiUri != null){
			return dockerRestApiUri;
		}
		String apiUri = configuration.getDockerRestApiUri();
		if (apiUri.startsWith(DockerHelper.UNIX_SCHEME) && SystemUtils.IS_OS_WINDOWS) {
			NetworkUtils networkUtils = new NetworkUtils();
			String defaultDockerRestApiUri = "http://" + networkUtils.getIpOfLoopBackIp4()
					+ ":2375";

			LOG.warn(
					"\n\n*************************************************************\n"
							+ "*************************************************************\n"
							+ "Spotify client doesn't yet support npipe windows socket\n"
							+ "https://github.com/spotify/docker-client/issues/875\n" + "Try to use TCP daemon {}\n"
							+ "*************************************************************\n"
							+ "*************************************************************\n\n",
					defaultDockerRestApiUri);

			apiUri = defaultDockerRestApiUri;
		}
		dockerRestApiUri=apiUri;
		return dockerRestApiUri;
	}

	/**
	 * @return - The IP address of the machine wherein the docker daemon is
	 *         running. It is typically left as <code>0.0.0.0</code>
	 */
	public String getLocalhost() {
		if (configuration == null) {
			return null;
		}
		return configuration.getLocalhost();
	}

	/**
	 * @return - The browser to target (target could for e.g., be docker image)
	 *         mapping.
	 */
	public Map<String, BrowserInfo> getBrowsers() {
		Map<String, BrowserInfo> browsers = new HashMap<>();
		for (BrowserInfo each : configuration.getBrowsers()) {
			browsers.put(each.getBrowser(), each);
		}
		return Collections.unmodifiableMap(new LinkedHashMap<String, BrowserInfo>(browsers));
	}

	/**
	 * @param browser
	 *            name
	 * @return The browser to target (target could for e.g., be docker image)
	 *         mapping.
	 */
	public Map<String, BrowserVersionInfo> getBrowserVersions(final String browser) {
		Map<String, BrowserVersionInfo> browserVersions = new HashMap<>();
		for (BrowserVersionInfo each : getBrowsers().get(browser).getVersions()) {
			browserVersions.put(each.getVersion(), each);
		}
		return Collections.unmodifiableMap(new LinkedHashMap<String, BrowserVersionInfo>(browserVersions));
	}

	public BrowserVersionInfo getBrowserVersion(final String browser, final String version) {
		BrowserInfo browserInfo = getBrowsers().get(browser);
		if (Strings.isNullOrEmpty(version)) {
			return getBrowserVersions(browser).get(browserInfo.getDefaultVersion());
		} else {
			return getBrowserVersions(browser).get(version);
		}
	}
	
	public BrowserVersionInfo getBrowserDefaultVersion(final String browser) {
		return getBrowserVersion(browser, null);
	}

	/**
	 * @return - How many number of sessions are to be honoured at any given
	 *         point in time. This kind of resembles the threshold value after
	 *         which new session requests would be put into the Hub's wait
	 *         queue.
	 */
	public int getMaxSession() {
		if (configuration == null) {
			return 10;
		}
		return configuration.getMaxSession();
	}

	private static final class ReaderInstance {
		private static final ConfigReader instance = new ConfigReader();

		static {
			init();
		}

		private ReaderInstance() {
			// Defeat instantiation.
		}

		private static void init() {
			JsonElement jsonConf = getJustAskConfiguration();
			instance.configuration = new Gson().fromJson(jsonConf, Configuration.class);
			LOG.info("Working with the Configuration : {}", instance.configuration);
		}

		private static JsonElement getJustAskConfiguration() {
			
			GridHubConfiguration config = ServerHelper.getHubRegistry().getHub().getConfiguration();
			String confFile = DEFAULT_CONFIG_FILE;
			if (config.hubConfig != null) {
				confFile = config.hubConfig;
			}
			LOG.info("Load configuration file {}", confFile);
			return JSONConfigurationUtils.loadJSON(confFile).get("justAsk");
		}
	}

	private static class Configuration {
		private String dockerRestApiUri;
		private String localhost;
		private int maxSession;
		private List<BrowserInfo> browsers;

		public String getDockerRestApiUri() {
			return dockerRestApiUri;
		}

		public String getLocalhost() {
			return localhost;
		}

		public int getMaxSession() {
			return maxSession;
		}

		public List<BrowserInfo> getBrowsers() {
			return Collections.unmodifiableList(new LinkedList<BrowserInfo>(browsers));
		}

		@Override
		public String toString() {
			return "Configuration{" + "dockerRestApiUri='" + dockerRestApiUri + '\'' + ", localhost='" + localhost
					+ '\'' + ", maxSession=" + maxSession + ", browsers=" + browsers + '}';
		}
	}
}
