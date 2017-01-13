/*
 * Copyright 2015-2017 original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;

/**
 * Unit tests for {@link Methods}.
 * 
 * @author Oliver Gierke
 */
public class MethodsUnitTests {

	@Test // DATAREST-582
	public void userMethodsFilterSkipsMethodsIntroducedByProxying() throws Exception {

		ProxyFactory factory = new ProxyFactory();
		factory.setTarget(new Sample());
		factory.setProxyTargetClass(true);

		final Set<Method> methods = new HashSet<Method>();

		ReflectionUtils.doWithMethods(factory.getProxy().getClass(), new MethodCallback() {

			@Override
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				methods.add(method);
			}
		}, Methods.USER_METHODS);

		assertThat(methods, hasSize(1));
		assertThat(methods, contains(Sample.class.getMethod("method")));
	}

	static class Sample {
		public void method() {}
	}
}
