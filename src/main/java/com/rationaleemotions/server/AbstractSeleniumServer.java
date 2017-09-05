package com.rationaleemotions.server;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.openqa.grid.common.exception.GridConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractSeleniumServer implements ISeleniumServer {
	static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    
    @Override
    public boolean isServerRunning() {
        String url = String.format("http://%s:%d/wd/hub/status", getHost(), getPort());
        HttpGet host = new HttpGet(url);
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse response = client.execute(host);
            return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch (IOException | GridConfigurationException e) {
        	LOG.info("isServerRunning : {}",e.getMessage());
        }
        return false;
    }

}
