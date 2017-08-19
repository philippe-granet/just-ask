package com.rationaleemotions.server;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openqa.grid.internal.TestSession;
import org.openqa.selenium.remote.CapabilityType;

import com.rationaleemotions.config.BrowserVersionInfo;
import com.rationaleemotions.config.ConfigReader;
import com.rationaleemotions.server.ISeleniumServer.ServerException;

public class SpawnedServer {
    private interface Marker {
    }

    private static final Logger LOG = Logger.getLogger(Marker.class.getEnclosingClass().getName());

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
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info(String.format("***Server started on [%d]****", port));
        }
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
        LOG.info("Working with the implementation : [" + clazz.getCanonicalName() + "].");
        if (ISeleniumServer.class.isAssignableFrom(clazz)) {
            return clazz;
        }
        throw new IllegalStateException(serverImpl + " does not extend " + ISeleniumServer.class
            .getCanonicalName());
    }

    public void shutdown() {
        try {
            server.shutdownServer();
            LOG.info("***Server running on [" + getPort() + "] has been stopped****");
        } catch (Exception e) {
            LOG.log(Level.SEVERE,e.getMessage(),e);
        }
    }

    @Override
    public String toString() {
        return "SpawnedServer[" + getHost() + ":" + getPort() + "]";
    }
}
