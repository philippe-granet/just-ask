package com.rationaleemotions.servlets;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openqa.grid.internal.Registry;
import org.openqa.grid.web.servlet.RegistryBasedServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.rationaleemotions.grid.client.RequestForwardingClient;
import com.rationaleemotions.grid.client.RequestForwardingClientProvider;
import com.rationaleemotions.grid.session.SeleniumSessions;

/**
 * @author IgorV Date: 13.2.2017
 */
public class HubRequestsProxyingServlet extends RegistryBasedServlet {

	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@VisibleForTesting
	static RequestForwardingClientProvider requestForwardingClientProvider = new RequestForwardingClientProvider();

	public HubRequestsProxyingServlet() {
		this(null);
	}

	public HubRequestsProxyingServlet(Registry registry) {
		super(registry);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		forwardRequest(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		forwardRequest(req, resp);
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		forwardRequest(req, resp);
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		forwardRequest(req, resp);
	}

	private void forwardRequest(HttpServletRequest req, HttpServletResponse resp) {
		RequestForwardingClient requestForwardingClient;
		try {
			requestForwardingClient = createExtensionClient(req.getPathInfo());
		} catch (IllegalArgumentException e) {
			try {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
			} catch (IOException e1) {
				LOGGER.error("Exception during sendError", e1);
			}
			return;
		}

		try {
			requestForwardingClient.forwardRequest(req, resp);
		} catch (IOException e) {
			LOGGER.error("Exception during request forwarding", e);
			try {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
			} catch (IOException e1) {
				LOGGER.error("Exception during sendError", e1);
			}
		}
	}

	private RequestForwardingClient createExtensionClient(String path) {
		String sessionId = SeleniumSessions.getSessionIdFromPath(path);
		LOGGER.info("Forwarding request with path: {} for session : {}", path, sessionId);
		
		SeleniumSessions sessions = new SeleniumSessions(getRegistry());
		sessions.refreshTimeout(sessionId);

		URL remoteHost = sessions.getRemoteHostForSession(sessionId);
		String host = remoteHost.getHost();
		int port = remoteHost.getPort();
		LOGGER.info("Remote host retrieved: {}:{}", host, port);

		return requestForwardingClientProvider.provide(host, port);
	}
}