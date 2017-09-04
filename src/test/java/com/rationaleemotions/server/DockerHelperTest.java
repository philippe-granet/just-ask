package com.rationaleemotions.server;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.hamcrest.core.Is.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DockerHelperTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testConstructorIsPrivate() throws NoSuchMethodException, SecurityException, InstantiationException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Constructor<DockerHelper> constructor = DockerHelper.class.getDeclaredConstructor();
		assertTrue(Modifier.isPrivate(constructor.getModifiers()));

		constructor.setAccessible(true);

		thrown.expect(InvocationTargetException.class);
		thrown.expectCause(isA(IllegalStateException.class));

		constructor.newInstance();
	}
}