package com.rationaleemotions.servlets;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.openqa.grid.internal.Registry;
import org.seleniumhq.jetty9.server.Handler;
import org.seleniumhq.jetty9.server.Server;
import org.seleniumhq.jetty9.server.ShutdownMonitor;
import org.seleniumhq.jetty9.servlet.ServletContextHandler;
import org.seleniumhq.jetty9.util.component.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerHelper {

	private static Server server;
	private static Registry hubRegistry;
	private static ServletContextHandler servletContextHandler;
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private ServerHelper() {
		throw new IllegalStateException("Helper class");
	}

	public static Server getServer() {
		if (server != null) {
			return server;
		}
		Set<LifeCycle> lifeCycles;
		try {
			lifeCycles = (Set<LifeCycle>) FieldUtils.readField(ShutdownMonitor.getInstance(), "_lifeCycles", true);
			for (LifeCycle lifeCycle : lifeCycles) {
				if (lifeCycle instanceof Server) {
					server = (Server) lifeCycle;
					break;
				}
			}
		} catch (IllegalAccessException e) {
			LOG.error(e.getMessage(), e);
		}
		return null;
	}
	public static ServletContextHandler getServletContextHandler() {
		if (servletContextHandler != null) {
			return servletContextHandler;
		}
		if(getServer()==null){
			return null;
		}
		Handler handler = server.getHandler();
		if (handler instanceof ServletContextHandler) {
			servletContextHandler=(ServletContextHandler) handler;
		}
		return servletContextHandler;
	}
	
	public static Registry getHubRegistry() {
		if (hubRegistry != null) {
			return hubRegistry;
		}
		if(getServer()==null){
			return null;
		}
		ServletContextHandler handler = getServletContextHandler();
		if(handler!=null){
			hubRegistry = (Registry) handler.getAttribute(Registry.KEY);
		}
		return hubRegistry;
	}
}