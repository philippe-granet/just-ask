package com.rationaleemotions.server;

import static org.junit.Assert.*;

import org.junit.Test;

public class DockerBasedSeleniumServerTest {

	@Test
	public void testShmSize(){
		DockerBasedSeleniumServer dockerServer = new DockerBasedSeleniumServer();
		assertNull(dockerServer.getShmSize(null));
		assertNull(dockerServer.getShmSize("xxx"));
		assertEquals(1L, dockerServer.getShmSize("1b").longValue());
		assertEquals(1024*1L, dockerServer.getShmSize("1k").longValue());
		assertEquals(1024*1024*1L, dockerServer.getShmSize("1m").longValue());
		assertEquals(1024*1024*1024*1L, dockerServer.getShmSize("1g").longValue());
		assertEquals(1024*1024*1024*1L, dockerServer.getShmSize("1").longValue());
	}
}
