/*
 * Copyright 2017-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.webmvc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;

/**
 * Unit tests for {@link BasePathAwareHandlerMapping}.
 *
 * @author Oliver Gierke
 */
public class BasePathAwareHandlerMappingUnitTests {

	RepositoryRestConfiguration configuration = mock(RepositoryRestConfiguration.class);
	HandlerMappingStub mapping = new HandlerMappingStub(configuration);

	@Test // DATAREST-1132
	public void detectsAnnotationsOnProxies() {

		Class<?> type = createProxy(new SomeController());

		assertThat(mapping.isHandler(type)).isTrue();
	}

	@Test // DATAREST-1132
	public void doesNotConsiderMetaAnnotation() {

		Class<?> type = createProxy(new RepositoryController());

		assertThat(mapping.isHandler(type)).isFalse();
	}

	private static Class<?> createProxy(Object source) {

		ProxyFactory factory = new ProxyFactory(source);
		Object proxy = factory.getProxy();

		assertThat(AopUtils.isCglibProxy(proxy)).isTrue();

		return proxy.getClass();
	}

	@BasePathAwareController
	static class SomeController {}

	@RepositoryRestController
	static class RepositoryController {}

	static class HandlerMappingStub extends BasePathAwareHandlerMapping {

		public HandlerMappingStub(RepositoryRestConfiguration configuration) {
			super(configuration);
		}

		@Override
		public boolean isHandler(Class<?> beanType) {
			return super.isHandler(beanType);
		}
	}
}
