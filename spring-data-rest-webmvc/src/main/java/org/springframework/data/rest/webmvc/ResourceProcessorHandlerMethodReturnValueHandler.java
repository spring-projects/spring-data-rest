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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.mvc.HeaderLinksResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link HandlerMethodReturnValueHandler} to post-process the objects returned from controller methods using the
 * configured {@link ResourceProcessor}s.
 * 
 * @author Oliver Gierke
 */
public class ResourceProcessorHandlerMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	private static final TypeInformation<?> RESOURCE_TYPE = from(Resource.class);
	private static final TypeInformation<?> RESOURCES_TYPE = from(Resources.class);
	private static final Field CONTENT_FIELD = ReflectionUtils.findField(Resources.class, "content");

	static {
		ReflectionUtils.makeAccessible(CONTENT_FIELD);
	}

	private final HandlerMethodReturnValueHandler delegate;
	private final List<ProcessorWrapper> processors;
	private boolean rootLinksAsHeaders = false;

	/**
	 * Creates a new {@link ResourceProcessorHandlerMethodReturnValueHandler} using the given delegate to eventually
	 * delegate calls to {@link #handleReturnValue(Object, MethodParameter, ModelAndViewContainer, NativeWebRequest)} to.
	 * Will consider the given {@link ResourceProcessor} to post-process the controller methods return value to before
	 * invoking the delegate.
	 * 
	 * @param delegate the {@link HandlerMethodReturnValueHandler} to evenually delegate calls to, must not be
	 *          {@literal null}.
	 * @param processors the {@link ResourceProcessor}s to be considered, must not be {@literal null}.
	 */
	public ResourceProcessorHandlerMethodReturnValueHandler(HandlerMethodReturnValueHandler delegate,
			List<ResourceProcessor<?>> processors) {

		Assert.notNull(delegate, "Delegate must not be null!");
		Assert.notNull(processors, "ResourceProcessors must not be null!");

		this.delegate = delegate;
		this.processors = new ArrayList<ProcessorWrapper>();

		for (ResourceProcessor<?> processor : processors) {

			TypeInformation<?> componentType = from(processor.getClass()).getSuperTypeInformation(ResourceProcessor.class)
					.getComponentType();
			Class<?> rawType = componentType.getType();

			if (Resource.class.isAssignableFrom(rawType)) {
				this.processors.add(new ResourceProcessorWrapper(processor));
			} else if (Resources.class.isAssignableFrom(rawType)) {
				this.processors.add(new ResourcesProcessorWrapper(processor));
			} else {
				this.processors.add(new DefaultProcessorWrapper(processor));
			}
		}

		Collections.sort(this.processors, AnnotationAwareOrderComparator.INSTANCE);
	}

	/**
	 * @param rootLinksAsHeaders the rootLinksAsHeaders to set
	 */
	public void setRootLinksAsHeaders(boolean rootLinksAsHeaders) {
		this.rootLinksAsHeaders = rootLinksAsHeaders;
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

		// For Resources implementations, process elements first
		if (RESOURCES_TYPE.isAssignableFrom(targetType)) {

			Resources<?> resources = (Resources<?>) value;
			TypeInformation<?> elementTargetType = targetType.getSuperTypeInformation(Resources.class).getComponentType();
			List<Object> result = new ArrayList<Object>(resources.getContent().size());

			for (Object element : resources) {

				TypeInformation<?> elementTypeInformation = from(element.getClass());
				if (!elementTargetType.getType().equals(elementTypeInformation.getType())) {
					elementTargetType = elementTypeInformation;
				}

				result.add(invokeProcessorsFor(element, elementTargetType));
			}

			ReflectionUtils.setField(CONTENT_FIELD, resources, result);
		}

		ResourceSupport result = (ResourceSupport) invokeProcessorsFor(value, targetType);
		delegate.handleReturnValue(rewrapResult(result, returnValue), returnType, mavContainer, webRequest);
	}

	/**
	 * Invokes all registered {@link ResourceProcessor}s registered for the given {@link TypeInformation}.
	 * 
	 * @param value the object to process
	 * @param targetType
	 * @return
	 */
	private Object invokeProcessorsFor(Object value, TypeInformation<?> targetType) {

		Object currentValue = value;

		// Process actual value
		for (ProcessorWrapper wrapper : this.processors) {
			if (wrapper.supports(targetType, currentValue)) {
				currentValue = wrapper.invokeProcessor(currentValue);
			}
		}

		return currentValue;
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
	Object rewrapResult(ResourceSupport newBody, Object originalValue) {

		if (!(originalValue instanceof HttpEntity)) {
			return newBody;
		}

		HttpEntity<ResourceSupport> entity = null;

		if (originalValue instanceof ResponseEntity) {
			ResponseEntity<?> source = (ResponseEntity<?>) originalValue;
			entity = new ResponseEntity<ResourceSupport>(newBody, source.getHeaders(), source.getStatusCode());
		} else {
			HttpEntity<?> source = (HttpEntity<?>) originalValue;
			entity = new HttpEntity<ResourceSupport>(newBody, source.getHeaders());
		}

		return addLinksToHeaderWrapper(entity);
	}

	private HttpEntity<?> addLinksToHeaderWrapper(HttpEntity<ResourceSupport> entity) {

		return rootLinksAsHeaders ? HeaderLinksResponseEntity.wrap(entity) : entity;
	}

	/**
	 * Returns whether the given value is a resource (i.e. implements {@link Resource) or {@link Resources}).
	 * 
	 * @param value
	 * @return
	 */
	private boolean isResourceType(Object value) {
		return value instanceof ResourceSupport;
	}

	/**
	 * Interface to unify interaction with {@link ResourceProcessor}s. The {@link Ordered} rank should be determined by
	 * the underlying processor.
	 * 
	 * @author Oliver Gierke
	 */
	private interface ProcessorWrapper extends Ordered {

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
		 */
		Object invokeProcessor(Object object);
	}

	/**
	 * Default implementation of {@link ProcessorWrapper} to generically deal with {@link ResourceSupport} types.
	 * 
	 * @author Oliver Gierke
	 */
	private static class DefaultProcessorWrapper implements ProcessorWrapper {

		private final ResourceProcessor<?> processor;
		private final TypeInformation<?> targetType;

		/**
		 * Creates a ne {@link DefaultProcessorWrapper} with the given {@link ResourceProcessor}.
		 * 
		 * @param processor must not be {@literal null}.
		 */
		public DefaultProcessorWrapper(ResourceProcessor<?> processor) {

			Assert.notNull(processor);

			this.processor = processor;
			this.targetType = from(processor.getClass()).getSuperTypeInformation(ResourceProcessor.class).getComponentType();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.webmvc.ResourceProcessorHandlerMethodReturnValueHandler.PostProcessorWrapper#supports(org.springframework.data.util.TypeInformation, java.lang.Object)
		 */
		@Override
		public boolean supports(TypeInformation<?> typeInformation, Object value) {
			return targetType.isAssignableFrom(typeInformation);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.webmvc.ResourceProcessorHandlerMethodReturnValueHandler.PostProcessorWrapper#invokeProcessor(java.lang.Object)
		 */
		@Override
		@SuppressWarnings("unchecked")
		public Object invokeProcessor(Object object) {
			return ((ResourceProcessor<ResourceSupport>) processor).process((ResourceSupport) object);
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
		 * Returns the target type the underlying {@link ResourceProcessor} wants to get invoked for.
		 * 
		 * @return the targetType
		 */
		public TypeInformation<?> getTargetType() {
			return targetType;
		}
	}

	/**
	 * {@link ProcessorWrapper} to deal with {@link ResourceProcessor}s for {@link Resource}s. Will fall back to peeking
	 * into the {@link Resource}'s content for type resolution.
	 * 
	 * @author Oliver Gierke
	 */
	private static class ResourceProcessorWrapper extends DefaultProcessorWrapper {

		/**
		 * Creates a new {@link ResourceProcessorWrapper} for the given {@link ResourceProcessor}.
		 * 
		 * @param processor must not be {@literal null}.
		 */
		public ResourceProcessorWrapper(ResourceProcessor<?> processor) {
			super(processor);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.webmvc.ResourceProcessorHandlerMethodReturnValueHandler.PostProcessorWrapper#supports(org.springframework.data.util.TypeInformation, java.lang.Object)
		 */
		@Override
		public boolean supports(TypeInformation<?> typeInformation, Object value) {

			if (!RESOURCE_TYPE.isAssignableFrom(typeInformation)) {
				return false;
			}

			return super.supports(typeInformation, value) || isValueTypeMatch((Resource<?>) value, getTargetType());
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

			if (resource == null || !target.getType().isAssignableFrom(resource.getClass())) {
				return false;
			}

			Object content = resource.getContent();

			if (content == null) {
				return false;
			}

			TypeInformation<?> typeInfo = target.getSuperTypeInformation(Resource.class);
			return null != typeInfo && typeInfo.getComponentType().getType().isAssignableFrom(content.getClass());
		}
	}

	/**
	 * {@link ProcessorWrapper} for {@link ResourceProcessor}s targeting {@link Resources}. Will peek into the content of
	 * the {@link Resources} for type matching decisions if needed.
	 * 
	 * @author Oliver Gierke
	 */
	private static class ResourcesProcessorWrapper extends DefaultProcessorWrapper {

		/**
		 * Creates a new {@link ResourcesProcessorWrapper} for the given {@link ResourceProcessor}.
		 * 
		 * @param processor must not be {@literal null}.
		 */
		public ResourcesProcessorWrapper(ResourceProcessor<?> processor) {
			super(processor);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.webmvc.PostProcessorWrapper#supports(org.springframework.data.util.TypeInformation, java.lang.Object)
		 */
		@Override
		public boolean supports(TypeInformation<?> typeInformation, Object value) {

			if (!RESOURCES_TYPE.isAssignableFrom(typeInformation)) {
				return false;
			}

			return super.supports(typeInformation, value) || isValueTypeMatch((Resources<?>) value, getTargetType());
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
	 * Helper extension of {@link AnnotationAwareOrderComparator} to make {@link #getOrder(Object)} public to allow it
	 * being used in a standalone fashion.
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
