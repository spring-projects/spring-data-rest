/*
 * Copyright 2017-2024 the original author or authors.
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

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Unit tests for {@link BasePathAwareHandlerMapping}.
 *
 * @author Oliver Gierke
 */
class BasePathAwareHandlerMappingUnitTests {

	HandlerMappingStub mapping;
	RepositoryRestConfiguration configuration = mock(RepositoryRestConfiguration.class);

	@BeforeEach
	void setUp() {

		doReturn(URI.create("")).when(configuration).getBasePath();

		mapping = getHandlerMappingStub(configuration);
	}

	@Test // DATAREST-1132
	void detectsAnnotationsOnProxies() {

		Class<?> type = createProxy(new SomeController());

		assertThat(mapping.isHandler(type)).isTrue();
	}

	@Test // DATAREST-1132
	void doesNotConsiderMetaAnnotation() {

		Class<?> type = createProxy(new RepositoryController());

		assertThat(mapping.isHandler(type)).isFalse();
	}

	@Test // #1342, #1628, #1686, #1946
	void rejectsAtRequestMappingOnCustomController() {

		assertThatIllegalStateException()
				.isThrownBy(() -> {
					mapping.isHandler(InvalidController.class);
				})
				.withMessageContaining(InvalidController.class.getName());
	}

	@Test // #1342, #1628, #1686, #1946
	void doesNotRejectAtRequestMappingOnStandardMvcController() {

		assertThatNoException()
				.isThrownBy(() -> mapping.isHandler(ValidController.class));
	}

	@Test // #2087
	void combinesBasePathAndControllerPrefixesCorrectly() throws Exception {

		doReturn(URI.create("/base")).when(configuration).getBasePath();
		mapping = getHandlerMappingStub(configuration);

		var method = ReflectionUtils.findMethod(PrefixedController.class, "someMethod");
		var info = mapping.getMappingForMethod(method, PrefixedController.class);

		var next = info.getPatternValues().iterator().next();

		assertThat(next).isEqualTo("/base/controllerBase/method");
	}

	private static Class<?> createProxy(Object source) {

		ProxyFactory factory = new ProxyFactory(source);
		Object proxy = factory.getProxy();

		assertThat(AopUtils.isCglibProxy(proxy)).isTrue();

		return proxy.getClass();
	}

	private static HandlerMappingStub getHandlerMappingStub(RepositoryRestConfiguration configuration) {

		HandlerMappingStub mapping = new HandlerMappingStub(configuration);
		mapping.setApplicationContext(new StaticApplicationContext());
		mapping.afterPropertiesSet();

		return mapping;
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

	@BasePathAwareController
	@RequestMapping("/sample")
	static class InvalidController {}

	@Controller
	@RequestMapping("/sample")
	static class ValidController {}

	@BasePathAwareController("/controllerBase")
	static class PrefixedController {

		@GetMapping("/method")
		void someMethod() {}
	}
}
