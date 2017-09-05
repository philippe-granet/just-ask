package com.rationaleemotions.server;

import com.rationaleemotions.servlets.JustAskServlet;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.selenium.GridLauncherV3;
import org.openqa.selenium.net.NetworkUtils;
import org.openqa.selenium.net.PortProber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;

/**
 * Represents a {@link ISeleniumServer} implementation that is backed by a new JVM which executes the
 * selenium server as a separate process.
 */
public class JvmBasedSeleniumServer implements ISeleniumServer {
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String JAVA = System.getProperty("java.home") + File.separator + "bin" + File.separator +
        "java";
    private static final String CP = "-cp";
    private static final String CLASSPATH = System.getProperty("java.class.path");
    private static final String PORT_ARG = "-port";
    private static final String MAIN_CLASS = GridLauncherV3.class.getCanonicalName();
    private Process process;
    private int port;

    private static String[] getArgs(final int port) {
        return new String[] {
            JAVA,
            CP,
            CLASSPATH,
            MAIN_CLASS,
            PORT_ARG,
            Integer.toString(port)
        };
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public int startServer(final TestSession session) throws ServerException {
        port = PortProber.findFreePort();
        String[] args = getArgs(port);
        LOG.info("Spawning a Selenium server using the arguments [{}]", Arrays.toString(args));
        
        ProcessBuilder pb = new ProcessBuilder(getArgs(port));
        try {
            this.process = pb.start();
            return port;
        } catch (Exception e) {
            throw new ServerException(e.getMessage(), e);
        }
    }

    @Override
    public void shutdownServer() {
        if (process != null) {
            process.destroyForcibly();
            LOG.info("***Server shutdown****");
            process = null;
        }
    }

    @Override
    public String getHost() {
    	NetworkUtils networkUtils=new NetworkUtils();
        return networkUtils.getIpOfLoopBackIp4();
    }
}
