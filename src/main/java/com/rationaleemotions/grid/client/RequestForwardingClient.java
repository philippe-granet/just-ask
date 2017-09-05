package com.rationaleemotions.grid.client;

import com.rationaleemotions.grid.session.SeleniumSessions;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;

/**
 * @author IgorV Date: 13.2.2017
 */
public class RequestForwardingClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static final String NODE_HOST = "http://%s:%d";

	private final HttpClientProvider httpClientProvider;
	private final String endpoint;

	public RequestForwardingClient(String host, int port) {
		this(String.format(NODE_HOST, host, port), new HttpClientProvider());
	}

	public RequestForwardingClient(String endpoint, HttpClientProvider httpClientProvider) {
		this.httpClientProvider = httpClientProvider;
		this.endpoint = endpoint;
	}

	public void forwardRequest(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
			throws IOException {
		try (CloseableHttpClient httpClient = httpClientProvider.provide()) {
			HttpRequestBase httpRequest = createHttpRequest(servletRequest);

			CloseableHttpResponse extensionResponse = httpClient.execute(httpRequest);
			HttpResponseConverter.copy(extensionResponse, servletResponse);
		}
	}

	private HttpRequestBase createHttpRequest(HttpServletRequest request) throws IOException {
		String method = request.getMethod();
		LOGGER.info("Creating {} request to forward", method);
		HttpRequestBase httpRequestBase = null;
		switch (method) {
		case HttpPost.METHOD_NAME:
			httpRequestBase = createPostRequest(request);
			break;
		case HttpGet.METHOD_NAME:
			httpRequestBase = new HttpGet();
			break;
		case HttpPut.METHOD_NAME:
			httpRequestBase = new HttpPut();
			break;
		case HttpDelete.METHOD_NAME:
			httpRequestBase = new HttpDelete();
			break;
		default:
			break;
		}

		if (httpRequestBase == null) {
			throw new UnsupportedHttpMethodException(method);
		}
		URI uri = URI.create(endpoint + SeleniumSessions.trimSessionPath(request.getPathInfo()));
		LOGGER.info("Trimming session id from path, new path: {}", uri);
		httpRequestBase.setURI(uri);

		return httpRequestBase;
	}

	private HttpRequestBase createPostRequest(HttpServletRequest request) throws IOException {
		HttpPost httpPost = new HttpPost();
		String strContentType = request.getContentType();
		LOGGER.info("Posting request with mime-type: {}", strContentType);
		ContentType contentType = ContentType.getByMimeType(strContentType);
		if (null == contentType)
			contentType = ContentType.APPLICATION_OCTET_STREAM;
		LOGGER.info("Posting request with mime-type {} handled as: {}", strContentType, contentType);
		InputStreamEntity entity = new InputStreamEntity(request.getInputStream(), request.getContentLength(),
				contentType);
		httpPost.setEntity(entity);

		return httpPost;
	}
}