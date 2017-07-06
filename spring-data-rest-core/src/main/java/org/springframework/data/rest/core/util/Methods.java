/*
 * Copyright 2012-2017 the original author or authors.
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
package org.springframework.data.rest.core.util;

import java.lang.reflect.Method;

import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Simple helper with utilities to work with {@link Method}s.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 * @deprecated prefer {@link ReflectionUtils#getUniqueDeclaredMethods(Class)}, to be removed with 3.0
 */
@Deprecated
public abstract class Methods {

	private Methods() {}

	public static final ReflectionUtils.MethodFilter USER_METHODS = new ReflectionUtils.MethodFilter() {

		/*
		 * (non-Javadoc)
		 * @see org.springframework.util.ReflectionUtils.MethodFilter#matches(java.lang.reflect.Method)
		 */
		@Override
		public boolean matches(Method method) {

			return !method.isSynthetic() && //
			!method.isBridge() && //
			!ReflectionUtils.isObjectMethod(method) && //
			!ClassUtils.isCglibProxyClass(method.getDeclaringClass()) && //
			!ReflectionUtils.isCglibRenamedMethod(method);
		}
	};
}
