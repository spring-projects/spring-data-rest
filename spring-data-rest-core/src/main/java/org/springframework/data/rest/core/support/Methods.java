package org.springframework.data.rest.core.support;

import java.lang.reflect.Method;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.util.ReflectionUtils;

/**
 * @author Jon Brisbin
 */
public abstract class Methods {

	private Methods() {}

	public static final ReflectionUtils.MethodFilter USER_METHODS = new ReflectionUtils.MethodFilter() {
		@Override
		public boolean matches(Method method) {
			return (!method.isSynthetic() && !method.isBridge() && method.getDeclaringClass() != Object.class && !method
					.getName().contains("$"));
		}
	};
	public static final LocalVariableTableParameterNameDiscoverer NAME_DISCOVERER = new LocalVariableTableParameterNameDiscoverer();

}
