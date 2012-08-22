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
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceEnricher;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpEntity;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link HandlerMethodReturnValueHandler} to post-process the objects returned from controller methods using the
 * configured {@link ResourceEnricher}s.
 * 
 * @author Oliver Gierke
 */
public class ResourceEnricherHandlerMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	private static final TypeInformation<?> RESOURCE_TYPE = from(Resource.class);
	private static final TypeInformation<?> RESOURCES_TYPE = from(Resources.class);

	private final HandlerMethodReturnValueHandler delegate;
	private final List<EnricherWrapper> enrichers;

	/**
	 * Creates a new {@link ResourceEnricherHandlerMethodReturnValueHandler} using the given delegate to eventually
	 * delegate calls to {@link #handleReturnValue(Object, MethodParameter, ModelAndViewContainer, NativeWebRequest)} to.
	 * Will consider the given {@link ResourceEnricher} to post-process the controller methods return value to before
	 * invoking the delegate.
	 * 
	 * @param delegate the {@link HandlerMethodReturnValueHandler} to evenually delegate calls to, must not be
	 *          {@literal null}.
	 * @param enrichers the {@link ResourceEnricher}s to be considered, must not be {@literal null}.
	 */
	public ResourceEnricherHandlerMethodReturnValueHandler(HandlerMethodReturnValueHandler delegate,
			List<ResourceEnricher<?>> enrichers) {

		Assert.notNull(delegate, "Delegate must not be null!");
		Assert.notNull(enrichers, "ResourceEnrichers must not be null!");

		this.delegate = delegate;
		this.enrichers = new ArrayList<EnricherWrapper>();

		for (ResourceEnricher<?> enricher : enrichers) {

			TypeInformation<?> componentType = from(enricher.getClass()).getSuperTypeInformation(ResourceEnricher.class)
					.getComponentType();
			Class<?> rawType = componentType.getType();

			if (Resource.class.isAssignableFrom(rawType)) {
				this.enrichers.add(new ResourceEnricherWrapper(enricher));
			} else if (Resources.class.isAssignableFrom(rawType)) {
				this.enrichers.add(new ResourcesEnricherWrapper(enricher));
			} else {
				this.enrichers.add(new DefaultEnricherWrapper(enricher));
			}
		}

		Collections.sort(this.enrichers, AnnotationAwareOrderComparator.INSTANCE);
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

		// We have a Resource or Resources - find suitable enrichers
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

			for (Object element : resources) {

				TypeInformation<?> elementTypeInformation = from(element.getClass());
				if (!elementTargetType.getType().equals(elementTypeInformation.getType())) {
					elementTargetType = elementTypeInformation;
				}

				for (EnricherWrapper wrapper : this.enrichers) {
					if (wrapper.supports(elementTargetType, element)) {
						wrapper.invokeEnricher(element);
					}
				}
			}
		}

		// Process actual value
		for (EnricherWrapper wrapper : this.enrichers) {
			if (wrapper.supports(targetType, value)) {
				wrapper.invokeEnricher(value);
			}
		}

		delegate.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
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
	 * Interface to unify interaction with {@link ResourceEnricher}s. The {@link Ordered} rank should be determined by the
	 * underlying processor.
	 * 
	 * @author Oliver Gierke
	 */
	private interface EnricherWrapper extends Ordered {

		/**
		 * Returns whether the underlying processor supports the given {@link TypeInformation}. It might also aditionally
		 * inspect the object that would eventually be handed to the enricher.
		 * 
		 * @param typeInformation the type of object to be post processed, will never be {@literal null}.
		 * @param value the object that would be passed into the enricher eventually, can be {@literal null}.
		 * @return
		 */
		boolean supports(TypeInformation<?> typeInformation, Object value);

		/**
		 * Performs the actual invocation of the enricher. Implementations can be sure
		 * {@link #supports(TypeInformation, Object)} has been called before and returned {@literal true}.
		 * 
		 * @param object
		 */
		void invokeEnricher(Object object);
	}

	/**
	 * Default implementation of {@link EnricherWrapper} to generically deal with {@link ResourceSupport} types.
	 * 
	 * @author Oliver Gierke
	 */
	private static class DefaultEnricherWrapper implements EnricherWrapper {

		private final ResourceEnricher<?> enricher;
		private final TypeInformation<?> targetType;

		/**
		 * Creates a ne {@link DefaultEnricherWrapper} with the given {@link ResourceEnricher}.
		 * 
		 * @param enricher must not be {@literal null}.
		 */
		public DefaultEnricherWrapper(ResourceEnricher<?> enricher) {

			Assert.notNull(enricher);

			this.enricher = enricher;
			this.targetType = from(enricher.getClass()).getSuperTypeInformation(ResourceEnricher.class).getComponentType();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.webmvc.ResourceEnricherHandlerMethodReturnValueHandler.PostProcessorWrapper#supports(org.springframework.data.util.TypeInformation, java.lang.Object)
		 */
		@Override
		public boolean supports(TypeInformation<?> typeInformation, Object value) {
			return targetType.isAssignableFrom(typeInformation);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.webmvc.ResourceEnricherHandlerMethodReturnValueHandler.PostProcessorWrapper#invokeProcessor(java.lang.Object)
		 */
		@Override
		@SuppressWarnings("unchecked")
		public void invokeEnricher(Object object) {
			((ResourceEnricher<ResourceSupport>) enricher).enrich((ResourceSupport) object);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.Ordered#getOrder()
		 */
		@Override
		public int getOrder() {
			return CustomOrderAwareComparator.INSTANCE.getOrder(enricher);
		}

		/**
		 * Returns the target type the underlying {@link ResourceEnricher} wants to get invoked for.
		 * 
		 * @return the targetType
		 */
		public TypeInformation<?> getTargetType() {
			return targetType;
		}
	}

	/**
	 * {@link EnricherWrapper} to deal with {@link ResourceEnricher}s for {@link Resource}s. Will fall back to peeking
	 * into the {@link Resource}'s content for type resolution.
	 * 
	 * @author Oliver Gierke
	 */
	private static class ResourceEnricherWrapper extends DefaultEnricherWrapper {

		/**
		 * Creates a new {@link ResourceEnricherWrapper} for the given {@link ResourceEnricher}.
		 * 
		 * @param processor must not be {@literal null}.
		 */
		public ResourceEnricherWrapper(ResourceEnricher<?> processor) {
			super(processor);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.webmvc.ResourceEnricherHandlerMethodReturnValueHandler.PostProcessorWrapper#supports(org.springframework.data.util.TypeInformation, java.lang.Object)
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
	 * {@link EnricherWrapper} for {@link ResourceEnricher}s targeting {@link Resources}. Will peek into the content of
	 * the {@link Resources} for type matching decisions if needed.
	 * 
	 * @author Oliver Gierke
	 */
	private static class ResourcesEnricherWrapper extends DefaultEnricherWrapper {

		/**
		 * Creates a new {@link ResourcesEnricherWrapper} for the given {@link ResourceEnricher}.
		 * 
		 * @param enricher must not be {@literal null}.
		 */
		public ResourcesEnricherWrapper(ResourceEnricher<?> enricher) {
			super(enricher);
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
			return ResourceEnricherWrapper.isValueTypeMatch((Resource<?>) element, resourceTypeInformation);
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
