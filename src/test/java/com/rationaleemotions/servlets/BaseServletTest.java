package com.rationaleemotions.servlets;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.openqa.testing.FakeHttpServletRequest;
import org.openqa.testing.FakeHttpServletResponse;
import org.openqa.testing.UrlInfo;

import com.google.gson.JsonObject;

public class BaseServletTest {
	private static final String BASE_URL = "http://localhost:4444";
	private static final String CONTEXT_PATH = "/";

	protected HttpServlet servlet;

	protected static UrlInfo createUrl(String path) {
		return new UrlInfo(BASE_URL, CONTEXT_PATH, path);
	}

	protected FakeHttpServletResponse sendCommand(String method, String commandPath)
			throws IOException, ServletException, URISyntaxException {
		return sendCommand(method, commandPath, null);
	}

	protected FakeHttpServletResponse sendCommand(String method, String commandPath, JsonObject parameters)
			throws IOException, ServletException, URISyntaxException {
		UrlInfo urlInfo = createUrl(commandPath);
		FakeHttpServletRequest request = new FakeHttpServletRequest(method, urlInfo);
		if (parameters != null) {
			request.setBody(parameters.toString());
		}
		URIBuilder uriBuilder = new URIBuilder(urlInfo.toString());
		List<NameValuePair> urlParameters = uriBuilder.getQueryParams();
		
		Map<String, String> urlParams = new HashMap<>();
		for (NameValuePair nameValuePair : urlParameters) {
			urlParams.put(nameValuePair.getName(), nameValuePair.getValue());
		}
		request.setParameters(urlParams);
		FakeHttpServletResponse response = new FakeHttpServletResponse();
		servlet.service(request, response);
		return response;
	}
}