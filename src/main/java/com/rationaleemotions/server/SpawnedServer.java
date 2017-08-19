package com.rationaleemotions.server;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.openqa.grid.internal.TestSession;
import org.openqa.selenium.remote.CapabilityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rationaleemotions.config.BrowserVersionInfo;
import com.rationaleemotions.config.ConfigReader;
import com.rationaleemotions.server.ISeleniumServer.ServerException;

public class SpawnedServer {
	private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ISeleniumServer server;

    private SpawnedServer() {
        //We have a factory method. Hiding the constructor.
    }

    public static SpawnedServer spawnInstance(TestSession session) throws Exception {
        SpawnedServer server = new SpawnedServer();
        AtomicInteger attempts = new AtomicInteger(0);
        String browser = (String) session.getRequestedCapabilities().get(CapabilityType.BROWSER_NAME);
        String version = (String) session.getRequestedCapabilities().get(CapabilityType.BROWSER_VERSION);
        server.server = newInstance(browser,version);
        int port = server.server.startServer(session);

        boolean isServerRunning=false;
        do {
            TimeUnit.SECONDS.sleep(1);
            isServerRunning=server.server.isServerRunning();
        } while (!isServerRunning && attempts.incrementAndGet() <= 10);
        
        if(!isServerRunning){
        	 throw new ServerException(String.format("Failed to access Selenium Node"));
        }
        LOG.info("***Server started on [{}]****", port);
        
        return server;
    }

    public String getHost() {
        return server.getHost();
    }

    public int getPort() {
        return server.getPort();
    }

    private static ISeleniumServer newInstance(final String browser, final String version)
        throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return (ISeleniumServer) getServerClass(browser,version).newInstance();
    }

    private static Class<?> getServerClass(final String browser, final String version) throws ClassNotFoundException {
        BrowserVersionInfo browserVersion = ConfigReader.getInstance().getBrowserVersion(browser,version);
        String serverImpl=browserVersion.getImplementation();
        Class<?> clazz = Class.forName(serverImpl);
        LOG.info("Working with the implementation : [{}].", clazz.getCanonicalName());
        if (ISeleniumServer.class.isAssignableFrom(clazz)) {
            return clazz;
        }
        throw new IllegalStateException(serverImpl + " does not extend " + ISeleniumServer.class
            .getCanonicalName());
    }

    public void shutdown() {
        try {
            server.shutdownServer();
            LOG.info("***Server running on [{}] has been stopped****", getPort());
        } catch (Exception e) {
            LOG.error(e.getMessage(),e);
        }
    }

    @Override
    public String toString() {
        return "SpawnedServer[" + getHost() + ":" + getPort() + "]";
    }
}
