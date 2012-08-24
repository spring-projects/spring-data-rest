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
import org.springframework.data.rest.webmvc.ResourceProcessorHandlerMethodReturnValueHandler;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Unit tests for {@link ResourceProcessorHandlerMethodReturnValueHandler}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourceProcessorHandlerMethodReturnValueHandlerUnitTests {

	@Mock
	HandlerMethodReturnValueHandler delegate;

	@Mock
	MethodParameter parameter;

	List<ResourceProcessor<?>> resourceProcessors;

	Resource<String> source = new Resource<String>("foo");
	Resource<String> result = StringResourceProcessor.RESULT;

	@Before
	public void setUp() {
		resourceProcessors = new ArrayList<ResourceProcessor<?>>();
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
		HandlerMethodReturnValueHandler handler = new ResourceProcessorHandlerMethodReturnValueHandler(delegate,
				resourceProcessors);

		assertThat(handler.supportsReturnType(parameter), is(value));
	}

	@Test
	public void invokesStringPostProcessorForSimpleStringResource() throws Exception {

		resourceProcessors.add(new StringResourceProcessor());
		resourceProcessors.add(new LongResourceProcessor());

		HttpEntity<Resource<String>> input = new HttpEntity<Resource<String>>(source);
		HttpEntity<Resource<String>> output = new HttpEntity<Resource<String>>(result);

		assertProcessorInvokedForMethod("stringResourceEntity", input, output);
		assertProcessorInvokedForMethod("resourceEntity", input, output);
	}

	@Test
	public void invokesStringPostProcessorForSimpleStringResourceInResponseEntity() throws Exception {

		resourceProcessors.add(new StringResourceProcessor());
		resourceProcessors.add(new LongResourceProcessor());

		ResponseEntity<Resource<String>> input = new ResponseEntity<Resource<String>>(source, HttpStatus.OK);
		ResponseEntity<Resource<String>> output = new ResponseEntity<Resource<String>>(result, HttpStatus.OK);

		assertProcessorInvokedForMethod("stringResourceEntity", input, output);
		assertProcessorInvokedForMethod("resourceEntity", input, output);
	}

	@Test
	public void invokesStringPostProcessorForSimpleStringResources() throws Exception {

		resourceProcessors.add(new StringResourceProcessor());
		resourceProcessors.add(new LongResourceProcessor());
		resourceProcessors.add(new StringResourcesProcessor());

		Resources<Resource<String>> sources = new Resources<Resource<String>>(Collections.singleton(source));

		HttpEntity<Resources<Resource<String>>> input = new HttpEntity<Resources<Resource<String>>>(sources);
		HttpEntity<Resources<Resource<String>>> output = new HttpEntity<Resources<Resource<String>>>(
				StringResourcesProcessor.RESULT);

		assertProcessorInvokedForMethod("stringResourceEntity", input, output);
		assertProcessorInvokedForMethod("resourceEntity", input, output);
	}

	@Test
	public void invokesStringPostProcessorForSpecializedStringResource() throws Exception {

		resourceProcessors.add(new StringResourceProcessor());
		resourceProcessors.add(new LongResourceProcessor());

		HttpEntity<Resource<String>> stringOutput = new HttpEntity<Resource<String>>(result);
		HttpEntity<StringResource> specializedInput = new HttpEntity<StringResource>(new StringResource("foo"));

		assertProcessorInvokedForMethod("stringResourceEntity", specializedInput, stringOutput);
		assertProcessorInvokedForMethod("resourceEntity", specializedInput, stringOutput);
		assertProcessorInvokedForMethod("specializedStringResourceEntity", specializedInput, stringOutput);
	}

	@Test
	public void doesNotInvokeSpecializedStringPostProcessorForSimpleStringResource() throws Exception {

		resourceProcessors.add(new SpecializedStringResourceProcessor());
		resourceProcessors.add(new LongResourceProcessor());

		HttpEntity<Resource<String>> input = new HttpEntity<Resource<String>>(source);

		assertProcessorInvokedForMethod("stringResourceEntity", input, input);
	}

	@Test
	public void invokesSpecializedStringPostProcessor() throws Exception {

		resourceProcessors.add(new SpecializedStringResourceProcessor());
		resourceProcessors.add(new LongResourceProcessor());

		HttpEntity<StringResource> input = new HttpEntity<StringResource>(new StringResource("foo"));
		HttpEntity<StringResource> output = new HttpEntity<StringResource>(SpecializedStringResourceProcessor.RESULT);

		assertProcessorInvokedForMethod("specializedStringResourceEntity", input, output);
	}

	@Test
	public void invokesLongPostProcessorForLongResource() throws Exception {

		resourceProcessors.add(new StringResourceProcessor());
		resourceProcessors.add(new LongResourceProcessor());

		HttpEntity<Resource<Long>> input = new HttpEntity<Resource<Long>>(new Resource<Long>(50L));
		HttpEntity<LongResource> specializedInput = new HttpEntity<LongResource>(new LongResource(50L));
		HttpEntity<Resource<Long>> output = new HttpEntity<Resource<Long>>(LongResourceProcessor.RESULT);

		assertProcessorInvokedForMethod("resourceEntity", specializedInput, output);
		assertProcessorInvokedForMethod("numberResourceEntity", input, output);
	}

	private void assertProcessorInvokedForMethod(String methodName, Object returnValue, Object processedValue)
			throws Exception {

		HandlerMethodReturnValueHandler handler = new ResourceProcessorHandlerMethodReturnValueHandler(delegate,
				resourceProcessors);

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

	/**
	 * {@link ResourceProcessor} to process {@link String}s.
	 * 
	 * @author Oliver Gierke
	 */
	static class StringResourceProcessor implements ResourceProcessor<Resource<String>> {

		static final Resource<String> RESULT = new Resource<String>("bar");

		@Override
		public Resource<String> enrich(Resource<String> resource) {
			return RESULT;
		}
	}

	static class StringResourcesProcessor implements ResourceProcessor<Resources<Resource<String>>> {

		static final Resources<Resource<String>> RESULT = new Resources<Resource<String>>(
				Collections.singleton(StringResourceProcessor.RESULT));

		@Override
		public Resources<Resource<String>> enrich(Resources<Resource<String>> resources) {
			return RESULT;
		}
	}

	/**
	 * {@link ResourceProcessor} to process {@link Long} values.
	 * 
	 * @author Oliver Gierke
	 */
	static class LongResourceProcessor implements ResourceProcessor<Resource<Long>> {

		static final Resource<Long> RESULT = new Resource<Long>(10L);

		@Override
		public Resource<Long> enrich(Resource<Long> resource) {
			return RESULT;
		}
	}

	static class StringResource extends Resource<String> {

		public StringResource(String value) {
			super(value);
		}
	}

	static class LongResource extends Resource<Long> {

		public LongResource(Long value) {
			super(value);
		}
	}

	static class SpecializedStringResourceProcessor implements ResourceProcessor<StringResource> {

		static final StringResource RESULT = new StringResource("foobar");

		@Override
		public StringResource enrich(StringResource resource) {
			return RESULT;
		}
	}
}
