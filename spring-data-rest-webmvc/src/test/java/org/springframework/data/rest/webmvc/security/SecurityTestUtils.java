/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.webmvc.security;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang.reflect.MethodUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.intercept.aopalliance.MethodSecurityInterceptor;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * @author Rob Winch
 * @author Greg Turnquist
 */
public class SecurityTestUtils {

	private final MethodSecurityInterceptor smi;

	public SecurityTestUtils(MethodSecurityInterceptor smi) {
		this.smi = smi;
	}

	/**
	 * Invoke the method to trigger a security check, but replace the real invocation target with a NoOp
	 * to prevent side effects.
	 *
	 * @param target
	 * @param methodName
	 * @param argTypes
	 * @return
	 * @throws Throwable
	 */
	public boolean hasAccess(Object target, String methodName, Class<?>... argTypes) throws Throwable {

		Method method = MethodUtils.getAccessibleMethod(target.getClass(), methodName, argTypes);
		MethodInvocation mi = new NoOpMethodInvocation(target, method);
		try {
			smi.invoke(mi);
			return true;
		} catch (AccessDeniedException denied) {
			return false;
		}
	}

	/**
	 * A method invocation that does nothing (on purpose).
	 */
	private class NoOpMethodInvocation extends SimpleMethodInvocation {

		public NoOpMethodInvocation(Object targetObject, Method method,
									Object... arguments) {
			super(targetObject, method, arguments);
		}

		public Object proceed() throws Throwable {
			return null;
		}
	}

}
