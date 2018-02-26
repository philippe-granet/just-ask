package com.rationaleemotions.servlets;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.openqa.grid.internal.GridRegistry;
import org.seleniumhq.jetty9.server.Handler;
import org.seleniumhq.jetty9.server.Server;
import org.seleniumhq.jetty9.server.ShutdownMonitor;
import org.seleniumhq.jetty9.servlet.ServletContextHandler;
import org.seleniumhq.jetty9.util.component.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerHelper {

	private static ServletContextHandler servletContextHandler;
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private ServerHelper() {
		throw new IllegalStateException("Helper class");
	}

	public static Server getServer() {
		Set<LifeCycle> lifeCycles;
		try {
			lifeCycles = (Set<LifeCycle>) FieldUtils.readField(ShutdownMonitor.getInstance(), "_lifeCycles", true);
			for (LifeCycle lifeCycle : lifeCycles) {
				if (lifeCycle instanceof Server) {
					return (Server) lifeCycle;
				}
			}
		} catch (IllegalAccessException e) {
			LOG.error(e.getMessage(), e);
		}
		return null;
	}
	public static ServletContextHandler getServletContextHandler() {
		Server server = getServer();
		if(server==null){
			return null;
		}
		Handler handler = server.getHandler();
		if (handler instanceof ServletContextHandler) {
			servletContextHandler=(ServletContextHandler) handler;
		}
		return servletContextHandler;
	}
	
	public static GridRegistry getHubRegistry() {
		ServletContextHandler handler = getServletContextHandler();
		if(handler!=null){
			return (GridRegistry) handler.getAttribute(GridRegistry.KEY);
		}
		return null;
	}
}