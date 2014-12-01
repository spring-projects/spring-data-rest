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
package org.springframework.data.rest.core.projection;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Unit tests for {@link ProjectingMethodInterceptor}.
 * 
 * @author Oliver Gierke
 * @author Saulo Medeiros de Araujo 
 */
@RunWith(MockitoJUnitRunner.class)
public class ProjectingMethodInterceptorUnitTests {

	@Mock MethodInterceptor interceptor;
	@Mock MethodInvocation invocation;
	@Mock ProjectionFactory factory;

	/**
	 * @see DATAREST-221
	 */
	@Test
	public void wrapsDelegateResultInProxyIfTypesDontMatch() throws Throwable {

		MethodInterceptor methodInterceptor = new ProjectingMethodInterceptor(new ProxyProjectionFactory(null), interceptor);

		when(invocation.getMethod()).thenReturn(Helper.class.getMethod("getHelper"));
		when(interceptor.invoke(invocation)).thenReturn("Foo");

		assertThat(methodInterceptor.invoke(invocation), is(instanceOf(Helper.class)));
	}

	/**
	 * @see DATAREST-221
	 */
	@Test
	public void retunsDelegateResultAsIsIfTypesMatch() throws Throwable {

		MethodInterceptor methodInterceptor = new ProjectingMethodInterceptor(factory, interceptor);

		when(invocation.getMethod()).thenReturn(Helper.class.getMethod("getString"));
		when(interceptor.invoke(invocation)).thenReturn("Foo");

		assertThat(methodInterceptor.invoke(invocation), is((Object) "Foo"));
	}

	/**
	 * @see DATAREST-221
	 */
	@Test
	public void returnsNullAsIs() throws Throwable {

		MethodInterceptor methodInterceptor = new ProjectingMethodInterceptor(factory, interceptor);

		when(interceptor.invoke(invocation)).thenReturn(null);

		assertThat(methodInterceptor.invoke(invocation), is(nullValue()));
	}

	/**
	 * @see DATAREST-221
	 */
	@Test
	public void considersPrimitivesAsWrappers() throws Throwable {

		MethodInterceptor methodInterceptor = new ProjectingMethodInterceptor(factory, interceptor);

		when(invocation.getMethod()).thenReturn(Helper.class.getMethod("getPrimitive"));
		when(interceptor.invoke(invocation)).thenReturn(1L);

		assertThat(methodInterceptor.invoke(invocation), is((Object) 1L));
		verify(factory, times(0)).createProjection(anyObject(), (Class<?>) anyObject());
	}

	/**
	 * @see DATAREST-394
	 */		
	@Test
	public void appliesProjectionToNonEmptyCollections() throws Throwable {
		
		MethodInterceptor methodInterceptor = new ProjectingMethodInterceptor(new ProxyProjectionFactory(null), interceptor);

		Helper helper = mock(Helper.class); 

		Collection<Helper> helpers = new ArrayList<Helper>();
		helpers.add(helper);

		when(invocation.getMethod()).thenReturn(Helper.class.getMethod("getHelperCollection"));
		when(interceptor.invoke(invocation)).thenReturn(helpers);

		Object result = methodInterceptor.invoke(invocation);
		assertThat(result, is(instanceOf(Collection.class)));
		
		Collection<?> projections = (Collection<?>) result;
		assertThat(projections, is(not(empty())));
		
		Object projection = projections.iterator().next();
		assertThat(projection, is(instanceOf(HelperProjection.class)));
	}

	/**
	 * @see DATAREST-394
	 */			
	@Test
	public void appliesProjectionToNonEmptyLists() throws Throwable {
		
		MethodInterceptor methodInterceptor = new ProjectingMethodInterceptor(new ProxyProjectionFactory(null), interceptor);

		Helper helper = mock(Helper.class); 

		List<Helper> helpers = new ArrayList<Helper>();
		helpers.add(helper);

		when(invocation.getMethod()).thenReturn(Helper.class.getMethod("getHelperList"));
		when(interceptor.invoke(invocation)).thenReturn(helpers);

		Object result = methodInterceptor.invoke(invocation);
		assertThat(result, is(instanceOf(List.class)));
		
		List<?> projections = (List<?>) result;
		assertThat(projections, is(not(empty())));
		
		Object projection = projections.get(0);
		assertThat(projection, is(instanceOf(HelperProjection.class)));		
	}

	/**
	 * @see DATAREST-394
	 */			
	@Test
	public void doesNotApplyProjectionSets() throws Throwable {
		MethodInterceptor methodInterceptor = new ProjectingMethodInterceptor(new ProxyProjectionFactory(null), interceptor);

		Helper helper = mock(Helper.class); 

		Set<Helper> helpers = new HashSet<Helper>();
		helpers.add(helper);

		when(invocation.getMethod()).thenReturn(Helper.class.getMethod("getHelperSet"));
		when(interceptor.invoke(invocation)).thenReturn(helpers);

		Object result = methodInterceptor.invoke(invocation);
		assertThat(result, is(instanceOf(Set.class)));
		
		Set<?> projections = (Set<?>) result;
		assertThat(projections, is(not(empty())));
		
		Object projection = projections.iterator().next();
		assertThat(projection, is(instanceOf(Helper.class)));		
	}
	
	interface Helper {

		Helper getHelper();

		String getString();

		long getPrimitive();
		
		Collection<HelperProjection> getHelperCollection();
		
		List<HelperProjection> getHelperList();
		
		Set<HelperProjection> getHelperSet();
	}
	
	interface HelperProjection {
		Helper getHelper();

		String getString();		
	}
}
