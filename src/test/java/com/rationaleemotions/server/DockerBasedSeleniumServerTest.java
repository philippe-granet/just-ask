package com.rationaleemotions.server;

import static org.junit.Assert.*;

import org.junit.Test;

public class DockerBasedSeleniumServerTest {

	@Test
	public void testMemorySize(){
		DockerBasedSeleniumServer dockerServer = new DockerBasedSeleniumServer();
		assertNull(dockerServer.getMemorySize(null));
		assertNull(dockerServer.getMemorySize("xxx"));
		assertEquals(1L, dockerServer.getMemorySize("1b").longValue());
		assertEquals(1024*1L, dockerServer.getMemorySize("1k").longValue());
		assertEquals(1024*1024*1L, dockerServer.getMemorySize("1m").longValue());
		assertEquals(1024*1024*1024*1L, dockerServer.getMemorySize("1g").longValue());
		assertEquals(1024*1024*1024*1L, dockerServer.getMemorySize("1").longValue());
	}
}
