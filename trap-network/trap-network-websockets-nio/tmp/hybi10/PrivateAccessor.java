package com.ericsson.research.transport.ws.hybi10;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import junit.framework.Assert;

/**
 * Provides access to private members in classes.
 */
public class PrivateAccessor {

	public static Object getPrivateField(Object o, String fieldName) {
		// Check we have valid arguments...
		Assert.assertNotNull(o);
		Assert.assertNotNull(fieldName);

		// Go and find the private field...
		final Field fields[] = o.getClass().getDeclaredFields();
		for (int i = 0; i < fields.length; ++i) {
			if (fieldName.equals(fields[i].getName())) {
				try {
					fields[i].setAccessible(true);
					return fields[i].get(o);
				} catch (IllegalAccessException ex) {
					Assert.fail("IllegalAccessException accessing " + fieldName);
				}
			}
		}
		Assert.fail("Field '" + fieldName + "' not found");
		return null;
	}

	public static void setPrivateField(Object o, String fieldName, Object newValue) {
		// Check we have valid arguments...
		Assert.assertNotNull(o);
		Assert.assertNotNull(fieldName);

		// Go and find the private field...
		final Field fields[] = o.getClass().getDeclaredFields();
		for (int i = 0; i < fields.length; ++i) {
			if (fieldName.equals(fields[i].getName())) {
				try {
					Field field = fields[i];
					field.setAccessible(true);
					field.set(o, newValue);
					return;
				} catch (IllegalAccessException ex) {
					Assert.fail("IllegalAccessException accessing " + fieldName);
				}
			}
		}
		Assert.fail("Field '" + fieldName + "' not found");
	}

	public static Object invokePrivateMethod(Object o, String methodName, Object[] params) {
		// Check we have valid arguments...
		Assert.assertNotNull(o);
		Assert.assertNotNull(methodName);
		Assert.assertNotNull(params);

		// Go and find the private method...
		final Method methods[] = o.getClass().getDeclaredMethods();
		for (int i = 0; i < methods.length; ++i) {
			if (methodName.equals(methods[i].getName())) {
				try {
					methods[i].setAccessible(true);
					return methods[i].invoke(o, params);
				} catch (IllegalAccessException ex) {
					Assert.fail("IllegalAccessException accessing " + methodName);
				} catch (InvocationTargetException ite) {
					Assert.fail("InvocationTargetException accessing " + methodName);
				}
			}
		}
		Assert.fail("Method '" + methodName + "' not found");
		return null;
	}
}