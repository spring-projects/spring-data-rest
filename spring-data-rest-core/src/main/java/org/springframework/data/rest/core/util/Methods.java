package org.springframework.data.rest.core.util;

import java.lang.reflect.Method;

import org.springframework.util.ReflectionUtils;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public abstract class Methods {

	private Methods() {}

	public static final ReflectionUtils.MethodFilter USER_METHODS = new ReflectionUtils.MethodFilter() {

		/*
		 * (non-Javadoc)
		 * @see org.springframework.util.ReflectionUtils.MethodFilter#matches(java.lang.reflect.Method)
		 */
		@Override
		public boolean matches(Method method) {
			return (!method.isSynthetic() && !method.isBridge() && method.getDeclaringClass() != Object.class && !method
					.getName().contains("$"));
		}
	};
}
