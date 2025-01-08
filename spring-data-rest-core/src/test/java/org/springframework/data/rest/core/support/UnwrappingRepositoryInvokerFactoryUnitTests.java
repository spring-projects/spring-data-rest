/*
 * Copyright 2015-2025 the original author or authors.
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
package org.springframework.data.rest.core.support;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.core.domain.Profile;

/**
 * Unit tests for {@link UnwrappingRepositoryInvokerFactory}.
 *
 * @author Oliver Gierke
 */
class UnwrappingRepositoryInvokerFactoryUnitTests {

	static final Object REFERENCE = new Object();

	RepositoryInvokerFactory delegate = mock(RepositoryInvokerFactory.class);
	RepositoryInvoker invoker = mock(RepositoryInvoker.class);

	RepositoryInvokerFactory factory;
	Method method;

	@BeforeEach
	void setUp() throws Exception {

		when(delegate.getInvokerFor(Object.class)).thenReturn(invoker);

		this.factory = new UnwrappingRepositoryInvokerFactory(delegate, Collections.<EntityLookup<?>> emptyList());
		this.method = Object.class.getMethod("toString");
	}

	@Test // DATAREST-724, DATAREST-1261
	@SuppressWarnings("unchecked")
	void usesRegisteredEntityLookup() {

		EntityLookup<Object> lookup = mock(EntityLookup.class);

		when(lookup.supports(Profile.class)).thenReturn(true);
		when(delegate.getInvokerFor(Profile.class)).thenReturn(invoker);

		factory = new UnwrappingRepositoryInvokerFactory(delegate, Arrays.asList(lookup));
		factory.getInvokerFor(Profile.class).invokeFindById(1L);

		verify(lookup, times(1)).lookupEntity(eq(1L));
		verify(invoker, never()).invokeFindById(eq(1L)); // DATAREST-1261
	}
}
