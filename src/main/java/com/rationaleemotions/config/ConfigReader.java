package com.rationaleemotions.config;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A singleton instance that works as a configuration data source.
 */
public class ConfigReader {
    private static final String JVM_ARG = "config.file";
    private static final String CONFIG = System.getProperty(JVM_ARG);
    private Configuration configuration;

	private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * @return - A {@link ConfigReader} that represents the configuration.
     */
    public static ConfigReader getInstance() {
        return ReaderInstance.instance;
    }

    /**
     * @return - The docker rest api uri.
     */
    public URI getDockerRestApiUri() {
        if (configuration == null) {
            return null;
        }
        return URI.create(configuration.getDockerRestApiUri().replaceAll("^unix:///", "unix://localhost/"));
    }

    /**
     * @return - The IP address of the machine wherein the docker daemon is running. It is typically left as
     * <code>0.0.0.0</code>
     */
    public String getLocalhost() {
        if (configuration == null) {
            return null;
        }
        return configuration.getLocalhost();
    }

    /**
     * @return - The browser to target (target could for e.g., be docker image) mapping.
     */
    public Map<String, BrowserInfo> getBrowsers() {
        Map<String, BrowserInfo> browsers = new HashMap<>();
        for (BrowserInfo each : configuration.getBrowsers()) {
        	browsers.put(each.getBrowser(), each);
        }
        return Collections.unmodifiableMap(new LinkedHashMap<String, BrowserInfo>(browsers));
    }
    
    /**
     * @return - The browser to target (target could for e.g., be docker image) mapping.
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
		if(Strings.isNullOrEmpty(version)){
			return getBrowserVersions(browser).get(browserInfo.getDefaultVersion());
		}else{
			return getBrowserVersions(browser).get(version);
		}
	}
    /**
     * @return - How many number of sessions are to be honoured at any given point in time.
     * This kind of resembles the threshold value after which new session requests would be put into the
     * Hub's wait queue.
     */
    public int getMaxSession() {
        if (configuration == null) {
            return 10;
        }
        return configuration.getMaxSession();
    }

    private static InputStream getStream() {
        try {
            LOG.debug("Attempting to read {} as resource.", CONFIG);
            InputStream stream =  Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIG);
            if (stream == null) {
                LOG.debug("Re-attempting to read {} as a local file.", CONFIG);
                return new FileInputStream(new File(CONFIG));
            }
        } catch (Exception e) {
        	LOG.error(e.getMessage(), e);
        }
        return null;
    }
    
    private static class ReaderInstance {
        static final ConfigReader instance = new ConfigReader();

        static {
            init();
        }

        private ReaderInstance() {
            //Defeat instantiation.
        }

        private static void init() {
            InputStream stream = getStream();
            Preconditions.checkState(stream != null, "Unable to load configuration file.");
            instance.configuration = new Gson().fromJson(new JsonReader(new InputStreamReader(stream)), Configuration
                .class);
            LOG.info("Working with the Configuration : {}", instance.configuration);
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
            return "Configuration{" +
                "dockerRestApiUri='" + dockerRestApiUri + '\'' +
                ", localhost='" + localhost + '\'' +
                ", maxSession=" + maxSession +
                ", browsers=" + browsers +
                '}';
        }
    }
}
