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
package org.springframework.data.rest.webmvc;

import static org.springframework.data.util.ClassTypeInformation.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.ResourcesProcessor;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link HandlerMethodReturnValueHandler} to post-process the objects returned from controller methods using the
 * configured {@link ResourceProcessor}s / {@link ResourcesProcessor}s.
 * 
 * @author Oliver Gierke
 */
public class ResourceProcessingHandlerMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	private static final Set<Class<?>> RESOURCE_TYPES = new HashSet<Class<?>>(2, 1);
	private static final Set<Class<?>> PARAMETER_TYPES = new HashSet<Class<?>>(3, 1);

	static {

		RESOURCE_TYPES.add(Resource.class);
		RESOURCE_TYPES.add(Resources.class);
		PARAMETER_TYPES.addAll(RESOURCE_TYPES);
		PARAMETER_TYPES.add(HttpEntity.class);
	}

	private final HandlerMethodReturnValueHandler delegate;
	private final List<PostProcessorWrapper> processorWrapper;

	/**
	 * Creates a new {@link ResourceProcessingHandlerMethodReturnValueHandler} using the given delegate to eventually
	 * delegate calls to {@link #handleReturnValue(Object, MethodParameter, ModelAndViewContainer, NativeWebRequest)} to.
	 * Will consider the given {@link ResourceProcessor} / {@link ResourcesProcessor} to post-process the controller
	 * methods return value to before invoking the delegate.
	 * 
	 * @param delegate the {@link HandlerMethodReturnValueHandler} to evenually delegate calls to, must not be
	 *          {@literal null}.
	 * @param resourceProcessors the {@link ResourceProcessor}s to be considered, must not be {@literal null}.
	 * @param resourcesProcessors the {@link ResourcesProcessor}s to be considered, must not be {@literal null}.
	 */
	@Autowired
	public ResourceProcessingHandlerMethodReturnValueHandler(HandlerMethodReturnValueHandler delegate,
			List<ResourceProcessor<?>> resourceProcessors, List<ResourcesProcessor<?>> resourcesProcessors) {

		Assert.notNull(delegate, "Delegate must not be null!");
		Assert.notNull(resourceProcessors, "ResourceProcessors must not be null!");
		Assert.notNull(resourcesProcessors, "ResourcesProcessors must not be null!");

		this.delegate = delegate;

		this.processorWrapper = new ArrayList<PostProcessorWrapper>();

		for (ResourceProcessor<? extends Resource<?>> processor : resourceProcessors) {
			this.processorWrapper.add(new ResourceProcessorWrapper(processor));
		}

		for (ResourcesProcessor<? extends Resources<?>> processor : resourcesProcessors) {
			this.processorWrapper.add(new ResourcesProcessorWrapper(processor));
		}

		Collections.sort(this.processorWrapper, AnnotationAwareOrderComparator.INSTANCE);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodReturnValueHandler#supportsReturnType(org.springframework.core.MethodParameter)
	 */
	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return delegate.supportsReturnType(returnType);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodReturnValueHandler#handleReturnValue(java.lang.Object, org.springframework.core.MethodParameter, org.springframework.web.method.support.ModelAndViewContainer, org.springframework.web.context.request.NativeWebRequest)
	 */
	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest) throws Exception {

		Object value = returnValue;

		if (returnValue instanceof HttpEntity) {
			value = ((HttpEntity<?>) returnValue).getBody();
		}

		// No post-processable type found - proceed with delegate
		if (!isResourceType(value)) {
			delegate.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
			return;
		}

		// We have a Resource or Resources - find suitable processors
		TypeInformation<?> targetType = ClassTypeInformation.fromReturnTypeOf(returnType.getMethod());

		// Unbox HttpEntity
		if (HttpEntity.class.isAssignableFrom(targetType.getType())) {
			targetType = targetType.getTypeArguments().get(0);
		}

		TypeInformation<? extends Object> returnValueTypeInformation = ClassTypeInformation.from(value.getClass());
		// Returned value is actually of a more specific type, use this type information
		if (!targetType.getType().equals(returnValueTypeInformation.getType())) {
			targetType = returnValueTypeInformation;
		}

		Object processedValue = value;

		for (PostProcessorWrapper wrapper : this.processorWrapper) {
			if (wrapper.supports(targetType, processedValue)) {
				processedValue = wrapper.invokeProcessor(processedValue);
			}
		}

		delegate.handleReturnValue(rewrapResult(processedValue, returnValue), returnType, mavContainer, webRequest);
	}

	/**
	 * Returns whether the given value is a resource (i.e. implements {@link Resource) or {@link Resources}).
	 * 
	 * @param value
	 * @return
	 */
	private boolean isResourceType(Object value) {

		if (value == null) {
			return false;
		}

		for (Class<?> type : RESOURCE_TYPES) {
			if (type.isAssignableFrom(value.getClass())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Re-wraps the result of the post-processing work into an {@link HttpEntity} or {@link ResponseEntity} if the
	 * original value was one of those two types. Copies headers and status code from the original value but uses the new
	 * body.
	 * 
	 * @param newBody the post-processed value.
	 * @param originalValue the original input value.
	 * @return
	 */
	static Object rewrapResult(Object newBody, Object originalValue) {

		if (!(originalValue instanceof HttpEntity)) {
			return newBody;
		}

		if (originalValue instanceof ResponseEntity) {
			ResponseEntity<?> source = (ResponseEntity<?>) originalValue;
			return new ResponseEntity<Object>(newBody, source.getHeaders(), source.getStatusCode());
		} else {
			HttpEntity<?> source = (HttpEntity<?>) originalValue;
			return new HttpEntity<Object>(newBody, source.getHeaders());
		}
	}

	/**
	 * Interface to unify interaction with {@link ResourceProcessor} and {@link ResourcesProcessor}. The {@link Ordered}
	 * rank should be determined by the underlying processor.
	 * 
	 * @author Oliver Gierke
	 */
	private interface PostProcessorWrapper extends Ordered {

		/**
		 * Returns whether the underlying processor supports the given {@link TypeInformation}. It might also aditionally
		 * inspect the object that would eventually be handed to the processor.
		 * 
		 * @param typeInformation the type of object to be post processed, will never be {@literal null}.
		 * @param value the object that would be passed into the processor eventually, can be {@literal null}.
		 * @return
		 */
		boolean supports(TypeInformation<?> typeInformation, Object value);

		/**
		 * Performs the actual invocation of the processor. Implementations can be sure
		 * {@link #supports(TypeInformation, Object)} has been called before and returned {@literal true}.
		 * 
		 * @param object
		 * @return
		 */
		Object invokeProcessor(Object object);
	}

	/**
	 * {@link PostProcessorWrapper} to invoke {@link ResourceProcessor} instances.
	 * 
	 * @author Oliver Gierke
	 */
	private static class ResourceProcessorWrapper implements PostProcessorWrapper {

		private final ResourceProcessor<? extends Resource<?>> processor;
		private final TypeInformation<?> targetType;

		/**
		 * Creates a new {@link ResourceProcessorWrapper} for the given {@link ResourceProcessor}.
		 * 
		 * @param processor must not be {@literal null}.
		 */
		public ResourceProcessorWrapper(ResourceProcessor<? extends Resource<?>> processor) {

			Assert.notNull(processor);

			this.processor = processor;
			this.targetType = from(processor.getClass()).getSuperTypeInformation(ResourceProcessor.class).getComponentType();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.webmvc.ResourceProcessingHandlerMethodReturnValueHandler.PostProcessorWrapper#supports(org.springframework.data.util.TypeInformation, java.lang.Object)
		 */
		@Override
		public boolean supports(TypeInformation<?> typeInformation, Object value) {

			if (value == null || !Resource.class.isAssignableFrom(value.getClass())) {
				return false;
			}

			return targetType.isAssignableFrom(typeInformation) || isValueTypeMatch((Resource<?>) value, targetType);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.webmvc.ResourceProcessingHandlerMethodReturnValueHandler.PostProcessorWrapper#invokeProcessor(java.lang.Object)
		 */
		@Override
		@SuppressWarnings("unchecked")
		public Object invokeProcessor(Object object) {
			return ((ResourceProcessor<Resource<?>>) processor).process((Resource<?>) object);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.Ordered#getOrder()
		 */
		@Override
		public int getOrder() {
			return CustomOrderAwareComparator.INSTANCE.getOrder(processor);
		}

		/**
		 * Returns whether the given {@link Resource} matches the given target {@link TypeInformation}. We inspect the
		 * {@link Resource}'s value to determine the match.
		 * 
		 * @param resource
		 * @param target must not be {@literal null}.
		 * @return whether the given {@link Resource} can be assigned to the given target {@link TypeInformation}
		 */
		private static boolean isValueTypeMatch(Resource<?> resource, TypeInformation<?> target) {

			if (resource == null || !target.getType().equals(resource.getClass())) {
				return false;
			}

			Object content = resource.getContent();

			if (content == null) {
				return false;
			}

			return target.getSuperTypeInformation(Resource.class).getComponentType().getType()
					.isAssignableFrom(content.getClass());
		}
	}

	/**
	 * {@link PostProcessorWrapper} for {@link ResourcesProcessor}s.
	 * 
	 * @author Oliver Gierke
	 */
	private static class ResourcesProcessorWrapper implements PostProcessorWrapper {

		private final ResourcesProcessor<? extends Resources<?>> processor;
		private final TypeInformation<?> targetType;

		/**
		 * Creates a new {@link ResourcesProcessorWrapper} for the given {@link ResourcesProcessor}.
		 * 
		 * @param processor must not be {@literal null}.
		 */
		public ResourcesProcessorWrapper(ResourcesProcessor<? extends Resources<?>> processor) {

			Assert.notNull(processor);

			this.processor = processor;
			this.targetType = from(processor.getClass()).getSuperTypeInformation(ResourcesProcessor.class).getComponentType();
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.rest.webmvc.PostProcessorWrapper#supports(org.springframework.data.util.TypeInformation, java.lang.Object)
		 */
		@Override
		public boolean supports(TypeInformation<?> typeInformation, Object value) {

			if (value == null || !Resources.class.isAssignableFrom(value.getClass())) {
				return false;
			}

			return targetType.isAssignableFrom(typeInformation) || isValueTypeMatch((Resources<?>) value, targetType);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.webmvc.ResourceProcessingHandlerMethodReturnValueHandler.PostProcessorWrapper#invokeProcessor(java.lang.Object)
		 */
		@Override
		@SuppressWarnings("unchecked")
		public Object invokeProcessor(Object object) {
			return ((ResourcesProcessor<Resources<?>>) processor).process((Resources<?>) object);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.Ordered#getOrder()
		 */
		@Override
		public int getOrder() {
			return CustomOrderAwareComparator.INSTANCE.getOrder(processor);
		}

		/**
		 * Returns whether the given {@link Resources} instance matches the given {@link TypeInformation}. We predict this
		 * by inspecting the first element of the content of the {@link Resources}.
		 * 
		 * @param resources the {@link Resources} to inspect.
		 * @param target that target {@link TypeInformation}.
		 * @return
		 */
		private static boolean isValueTypeMatch(Resources<?> resources, TypeInformation<?> target) {

			if (resources == null || !Resources.class.equals(resources.getClass())) {
				return false;
			}

			Collection<?> content = resources.getContent();

			if (content.isEmpty()) {
				return false;
			}

			Object element = content.iterator().next();

			if (!(element instanceof Resource)) {
				return false;
			}

			TypeInformation<?> resourceTypeInformation = target.getSuperTypeInformation(Resources.class).getComponentType();
			return ResourceProcessorWrapper.isValueTypeMatch((Resource<?>) element, resourceTypeInformation);
		}
	}

	/**
	 * Helper extension of {@link AnnotationAwareOrderComparator} to make {@link #getOrder()} public to allow it being
	 * used in a standalone fashion.
	 * 
	 * @author Oliver Gierke
	 */
	private static class CustomOrderAwareComparator extends AnnotationAwareOrderComparator {

		public static CustomOrderAwareComparator INSTANCE = new CustomOrderAwareComparator();

		@Override
		protected int getOrder(Object obj) {
			return super.getOrder(obj);
		}
	}
}
