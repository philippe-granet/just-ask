package com.rationaleemotions.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

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
	    throws IOException, ServletException {
	    return sendCommand(method, commandPath, null);
	  }

	  protected FakeHttpServletResponse sendCommand(String method, String commandPath,
	                                              JsonObject parameters) throws IOException, ServletException {
	    FakeHttpServletRequest request = new FakeHttpServletRequest(method, createUrl(commandPath));
	    if (parameters != null) {
	      request.setBody(parameters.toString());
	    }
	    FakeHttpServletResponse response = new FakeHttpServletResponse();
	    servlet.service(request, response);
	    return response;
	  }
	}