package com.rationaleemotions.servlets;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.openqa.grid.common.GridRole;
import org.openqa.grid.e2e.utils.GridTestHelper;
import org.openqa.grid.e2e.utils.RegistryTestHelper;
import org.openqa.grid.selenium.GridLauncherV3;
import org.openqa.grid.web.Hub;
import org.openqa.selenium.net.PortProber;
import org.openqa.selenium.remote.server.log.TerseFormatter;

public class BaseServletTestHelper {
	protected static Hub hub;
	
	public static void setUp(String configFile) throws Exception {
		Integer hubPort = PortProber.findFreePort();
		String[] hubArgs = { "-role", GridRole.HUB.toString(), "-port", hubPort.toString(), "-hubConfig",
				configFile };
		GridLauncherV3.main(hubArgs);
		GridTestHelper.waitForGrid();
		
		Level logLevel = Level.INFO;
		Logger.getLogger("").setLevel(logLevel);

		for (Handler handler : Logger.getLogger("").getHandlers()) {
			if (handler instanceof ConsoleHandler) {
				handler.setLevel(logLevel);
				handler.setFormatter(new TerseFormatter());
			}
		}
		Logger.getLogger("org.glassfish").setLevel(Level.WARNING);
		Logger.getLogger("org.apache").setLevel(Level.WARNING);
		Logger.getLogger("net.bull").setLevel(Level.WARNING);
		Logger.getLogger("org.seleniumhq.jetty9").setLevel(Level.WARNING);

		hub = ServerHelper.getHubRegistry().getHub();
		RegistryTestHelper.waitForNodes(ServerHelper.getHubRegistry().getHub().getRegistry());
	}

	@AfterClass
	public static void teardown() throws Exception {
		ServerHelper.getHubRegistry().getHub().stop();
	}
}