/*
 * Copyright 2012 the original author or authors.
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
package org.springframework.data.rest.test.webmvc;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.matchers.Equals;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.MethodParameter;
import org.springframework.data.rest.webmvc.ResourceEnricherHandlerMethodReturnValueHandler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceEnricher;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Unit tests for {@link ResourceEnricherHandlerMethodReturnValueHandler}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourceEnricherHandlerMethodReturnValueHandlerUnitTests {

	@Mock
	HandlerMethodReturnValueHandler delegate;

	@Mock
	MethodParameter parameter;

	List<ResourceEnricher<?>> resourceEnrichers;

	Resource<String> source;
	Resource<String> result;

	@Before
	public void setUp() {
		resourceEnrichers = new ArrayList<ResourceEnricher<?>>();
		resetResources();
	}

	private void resetResources() {
		source = new Resource<String>("foo");
		result = new Resource<String>("foo", StringResourceEnricher.LINK);
	}

	@Test
	public void supportsIfDelegateSupports() {
		assertSupport(true);
	}

	@Test
	public void doesNotSupportIfDelegateDoesNot() {
		assertSupport(false);
	}

	private void assertSupport(boolean value) {

		when(delegate.supportsReturnType(Mockito.any(MethodParameter.class))).thenReturn(value);
		HandlerMethodReturnValueHandler handler = new ResourceEnricherHandlerMethodReturnValueHandler(delegate,
				resourceEnrichers);

		assertThat(handler.supportsReturnType(parameter), is(value));
	}

	@Test
	public void invokesStringPostProcessorForSimpleStringResource() throws Exception {

		resourceEnrichers.add(new StringResourceEnricher());
		resourceEnrichers.add(new LongResourceEnricher());

		HttpEntity<Resource<String>> input = new HttpEntity<Resource<String>>(source);
		HttpEntity<Resource<String>> output = new HttpEntity<Resource<String>>(result);

		assertProcessorInvokedForMethod("stringResourceEntity", input, output);
		resetResources();
		input = new HttpEntity<Resource<String>>(source);
		assertProcessorInvokedForMethod("resourceEntity", input, output);
	}

	@Test
	public void invokesStringPostProcessorForSimpleStringResourceInResponseEntity() throws Exception {

		resourceEnrichers.add(new StringResourceEnricher());
		resourceEnrichers.add(new LongResourceEnricher());

		ResponseEntity<Resource<String>> input = new ResponseEntity<Resource<String>>(source, HttpStatus.OK);
		ResponseEntity<Resource<String>> output = new ResponseEntity<Resource<String>>(result, HttpStatus.OK);

		assertProcessorInvokedForMethod("stringResourceEntity", input, output);

		resetResources();
		input = new ResponseEntity<Resource<String>>(source, HttpStatus.OK);
		assertProcessorInvokedForMethod("resourceEntity", input, output);
	}

	@Test
	public void invokesStringPostProcessorForSimpleStringResources() throws Exception {

		resourceEnrichers.add(new StringResourceEnricher());
		resourceEnrichers.add(new LongResourceEnricher());
		resourceEnrichers.add(new StringResourcesEnricher());

		Resources<Resource<String>> sources = new Resources<Resource<String>>(Collections.singleton(source));
		

		HttpEntity<Resources<Resource<String>>> input = new HttpEntity<Resources<Resource<String>>>(sources);
		HttpEntity<Resources<Resource<String>>> output = new HttpEntity<Resources<Resource<String>>>(
				new Resources<Resource<String>>(Collections.singleton(result), StringResourcesEnricher.LINK));

		assertProcessorInvokedForMethod("stringResourceEntity", input, output);

		resetResources();
		input = new HttpEntity<Resources<Resource<String>>>(new Resources<Resource<String>>(Collections.singleton(source)));
		output = new HttpEntity<Resources<Resource<String>>>(
				new Resources<Resource<String>>(Collections.singleton(result), StringResourcesEnricher.LINK));
		assertProcessorInvokedForMethod("resourceEntity", input, output);
	}

	@Test
	public void invokesStringPostProcessorForSpecializedStringResource() throws Exception {

		resourceEnrichers.add(new StringResourceEnricher());
		resourceEnrichers.add(new LongResourceEnricher());

		HttpEntity<StringResource> specializedInput = new HttpEntity<StringResource>(new StringResource("foo"));
		HttpEntity<StringResource> stringOutput = new HttpEntity<StringResource>(new StringResource("foo",
				StringResourceEnricher.LINK));

		assertProcessorInvokedForMethod("stringResourceEntity", specializedInput, stringOutput);

		specializedInput = new HttpEntity<StringResource>(new StringResource("foo"));
		assertProcessorInvokedForMethod("resourceEntity", specializedInput, stringOutput);

		specializedInput = new HttpEntity<StringResource>(new StringResource("foo"));
		assertProcessorInvokedForMethod("specializedStringResourceEntity", specializedInput, stringOutput);
	}

	@Test
	public void doesNotInvokeSpecializedStringPostProcessorForSimpleStringResource() throws Exception {

		resourceEnrichers.add(new SpecializedStringResourceEnricher());
		resourceEnrichers.add(new LongResourceEnricher());

		HttpEntity<Resource<String>> input = new HttpEntity<Resource<String>>(source);

		assertProcessorInvokedForMethod("stringResourceEntity", input, input);
	}

	@Test
	public void invokesSpecializedStringPostProcessor() throws Exception {

		resourceEnrichers.add(new SpecializedStringResourceEnricher());
		resourceEnrichers.add(new LongResourceEnricher());

		HttpEntity<StringResource> input = new HttpEntity<StringResource>(new StringResource("foo"));
		HttpEntity<StringResource> output = new HttpEntity<StringResource>(new StringResource("foo",
				SpecializedStringResourceEnricher.LINK));

		assertProcessorInvokedForMethod("specializedStringResourceEntity", input, output);
	}

	@Test
	public void invokesLongPostProcessorForLongResource() throws Exception {

		resourceEnrichers.add(new StringResourceEnricher());
		resourceEnrichers.add(new LongResourceEnricher());

		HttpEntity<Resource<Long>> input = new HttpEntity<Resource<Long>>(new Resource<Long>(50L));
		HttpEntity<LongResource> specializedInput = new HttpEntity<LongResource>(new LongResource(50L));

		assertProcessorInvokedForMethod("resourceEntity", specializedInput, new HttpEntity<LongResource>(new LongResource(
				50L, LongResourceEnricher.LINK)));
		assertProcessorInvokedForMethod("numberResourceEntity", input, new HttpEntity<Resource<Long>>(new Resource<Long>(
				50L, LongResourceEnricher.LINK)));
	}

	private void assertProcessorInvokedForMethod(String methodName, Object returnValue, Object processedValue)
			throws Exception {

		HandlerMethodReturnValueHandler handler = new ResourceEnricherHandlerMethodReturnValueHandler(delegate,
				resourceEnrichers);

		Method method = Controller.class.getMethod(methodName);
		MethodParameter returnType = new MethodParameter(method, -1);

		handler.handleReturnValue(returnValue, returnType, null, null);

		verify(delegate, times(1)).handleReturnValue(argThat(new HttpEntityMatcher(processedValue)), eq(returnType),
				eq((ModelAndViewContainer) null), eq((NativeWebRequest) null));
	}

	@SuppressWarnings("serial")
	static class HttpEntityMatcher extends Equals {

		public HttpEntityMatcher(Object wanted) {
			super(wanted);
		}

		/* (non-Javadoc)
		 * @see org.mockito.internal.matchers.Equals#matches(java.lang.Object)
		 */
		@Override
		public boolean matches(Object actual) {

			Object wanted = getWanted();

			if (actual instanceof ResponseEntity && wanted instanceof ResponseEntity) {

				ResponseEntity<?> left = (ResponseEntity<?>) wanted;
				ResponseEntity<?> right = (ResponseEntity<?>) actual;

				if (!left.getStatusCode().equals(right.getStatusCode())) {
					return false;
				}
			}

			if (actual instanceof HttpEntity && wanted instanceof HttpEntity) {

				HttpEntity<?> left = (HttpEntity<?>) wanted;
				HttpEntity<?> right = (HttpEntity<?>) actual;

				if (!left.getBody().equals(right.getBody())) {
					return false;
				}

				if (!left.getHeaders().equals(right.getHeaders())) {
					return false;
				}

				return true;
			}

			return super.matches(actual);
		}
	}

	interface Controller {

		Resources<Resource<String>> resources();

		Resource<String> resource();

		StringResource specializedResource();

		Object object();

		HttpEntity<Resource<?>> resourceEntity();

		HttpEntity<Resources<?>> resourcesEntity();

		HttpEntity<Object> objectEntity();

		HttpEntity<Resource<String>> stringResourceEntity();

		HttpEntity<Resource<? extends Number>> numberResourceEntity();

		HttpEntity<StringResource> specializedStringResourceEntity();

		ResponseEntity<Resource<?>> resourceResponseEntity();

		ResponseEntity<Resources<?>> resourcesResponseEntity();
	}
	
	static class StringResource extends Resource<String> {

		public StringResource(String value, Link... links) {
			super(value, links);
		}
	}

	static class LongResource extends Resource<Long> {

		public LongResource(Long value, Link... links) {
			super(value, links);
		}
	}

	static class StringResourceEnricher implements ResourceEnricher<Resource<String>> {

		static final Link LINK = new Link("string-resource");

		@Override
		public void enrich(Resource<String> resource) {
			resource.add(LINK);
		}
	}

	static class StringResourcesEnricher implements ResourceEnricher<Resources<Resource<String>>> {

		static final Link LINK = new Link("string-resources");

		@Override
		public void enrich(Resources<Resource<String>> resource) {
			resource.add(LINK);
		}
	}

	static class LongResourceEnricher implements ResourceEnricher<Resource<Long>> {

		static final Link LINK = new Link("long-resource");

		@Override
		public void enrich(Resource<Long> resource) {
			resource.add(LINK);
		}
	}

	static class SpecializedStringResourceEnricher implements ResourceEnricher<StringResource> {

		static final Link LINK = new Link("specialized-string");

		@Override
		public void enrich(StringResource resource) {
			resource.add(LINK);
		}
	}
}
