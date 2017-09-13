package com.rationaleemotions.grid.client;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * @author IgorV
 *         Date: 13.2.2017
 */
public class HttpClientProvider {
	int timeout = 10;
	
    public CloseableHttpClient provide() {
    	RequestConfig config = RequestConfig.custom()
    	  .setConnectTimeout(timeout * 1000)
    	  .setConnectionRequestTimeout(timeout * 1000)
    	  .setSocketTimeout(timeout * 1000).build();
        return HttpClientBuilder.create().setDefaultRequestConfig(config).build();
    }
}