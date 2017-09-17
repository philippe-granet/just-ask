package com.rationaleemotions.grid.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Ints;

/**
 * @author IgorV Date: 13.2.2017
 */
public final class HttpResponseConverter {

	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private HttpResponseConverter() {
	}

	public static void copy(HttpResponse source, HttpServletResponse target) {
		int statusCode = source.getStatusLine().getStatusCode();
		target.setStatus(statusCode);
		LOGGER.debug("Response from extension returned {} status code", statusCode);

		HttpEntity entity = source.getEntity();

		Header contentType = entity.getContentType();
		if (contentType != null) {
			if (null == contentType.getValue()) {
				LOGGER.warn(
						"Response from extension returned null content type - it will not be copied to target servlet response.");
			} else {
				target.setContentType(contentType.getValue());
				LOGGER.debug("Response from extension returned {} content type", contentType.getValue());
			}
		}

		long contentLength = entity.getContentLength();
		target.setContentLength(Ints.checkedCast(contentLength));
		LOGGER.debug("Response from extension has {} content length", contentLength);

		LOGGER.debug("Copying body content to original servlet response");
		try (InputStream content = entity.getContent(); OutputStream response = target.getOutputStream()) {
			IOUtils.copy(content, response);
		} catch (IOException e) {
			LOGGER.error("Failed to copy response body content", e);
		}
	}
}