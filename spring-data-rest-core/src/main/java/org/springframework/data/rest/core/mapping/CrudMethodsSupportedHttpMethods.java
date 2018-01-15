/*
 * Copyright 2014-2018 the original author or authors.
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
package org.springframework.data.rest.core.mapping;

import static org.springframework.data.rest.core.mapping.ResourceType.*;
import static org.springframework.http.HttpMethod.*;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.repository.core.CrudMethods;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * {@link SupportedHttpMethods} that are determined by a {@link CrudMethods} instance.
 *
 * @author Oliver Gierke
 * @since 2.3
 */
public class CrudMethodsSupportedHttpMethods implements SupportedHttpMethods {

	private final ExposureAwareCrudMethods exposedMethods;

	/**
	 * Creates a new {@link CrudMethodsSupportedHttpMethods} for the given {@link CrudMethods}.
	 *
	 * @param crudMethods must not be {@literal null}.
	 * @param methodsExposedByDefault whether repository methods should be considered exposed by default or need to be
	 *          annotated with {@link RestResource} to really be visible.
	 */
	public CrudMethodsSupportedHttpMethods(CrudMethods crudMethods, boolean methodsExposedByDefault) {

		Assert.notNull(crudMethods, "CrudMethods must not be null!");

		this.exposedMethods = new DefaultExposureAwareCrudMethods(crudMethods, methodsExposedByDefault);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.SupportedHttpMethods#getSupportedHttpMethods(org.springframework.data.rest.core.mapping.ResourceType)
	 */
	@Override
	public Set<HttpMethod> getMethodsFor(ResourceType resourceType) {

		Assert.notNull(resourceType, "Resource type must not be null!");

		Set<HttpMethod> methods = new HashSet<HttpMethod>();
		methods.add(OPTIONS);

		switch (resourceType) {

			case COLLECTION:

				if (exposedMethods.exposesFindAll()) {
					methods.add(GET);
					methods.add(HEAD);
				}

				if (exposedMethods.exposesSave()) {
					methods.add(POST);
				}

				break;

			case ITEM:

				if (exposedMethods.exposesDelete()) {
					methods.add(DELETE);
				}

				if (exposedMethods.exposesFindOne()) {
					methods.add(GET);
					methods.add(HEAD);
				}

				if (exposedMethods.exposesSave()) {
					methods.add(PUT);
					methods.add(PATCH);
				}

				break;

			default:
				throw new IllegalArgumentException(String.format("Unsupported resource type %s!", resourceType));
		}

		return Collections.unmodifiableSet(methods);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.SupportedHttpMethods#getMethodsFor(org.springframework.data.mapping.PersistentProperty)
	 */
	@Override
	public Set<HttpMethod> getMethodsFor(PersistentProperty<?> property) {

		if (!property.isAssociation()) {
			return Collections.emptySet();
		}

		Set<HttpMethod> methods = new HashSet<HttpMethod>();

		methods.add(GET);

		if (property.isWritable() && getMethodsFor(ITEM).contains(PUT)) {
			methods.add(PUT);
			methods.add(PATCH);
			methods.add(DELETE);
		}

		if (property.isCollectionLike() && property.isWritable()) {
			methods.add(POST);
		}

		return methods;
	}

	/**
	 * @author Oliver Gierke
	 */
	@RequiredArgsConstructor
	private static class DefaultExposureAwareCrudMethods implements ExposureAwareCrudMethods {

		private final @NonNull CrudMethods crudMethods;
		private final boolean exportedDefault;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.core.mapping.ExposureAwareCrudMethods#exposesSave()
		 */
		@Override
		public boolean exposesSave() {
			return exposes(crudMethods.getSaveMethod());
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.core.mapping.ExposureAwareCrudMethods#exposesDelete()
		 */
		@Override
		public boolean exposesDelete() {
			return exposes(crudMethods.getDeleteMethod()) && crudMethods.hasFindOneMethod();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.core.mapping.ExposureAwareCrudMethods#exposesFindOne()
		 */
		@Override
		public boolean exposesFindOne() {
			return exposes(crudMethods.getFindOneMethod());
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.core.mapping.ExposureAwareCrudMethods#exposesFindAll()
		 */
		@Override
		public boolean exposesFindAll() {
			return exposes(crudMethods.getFindAllMethod());
		}

		private boolean exposes(Optional<Method> method) {

			return method.map(it -> {

				RestResource annotation = AnnotationUtils.findAnnotation(it, RestResource.class);
				return annotation == null ? exportedDefault : annotation.exported();

			}).orElse(false);
		}
	}

	/**
	 * @author Oliver Gierke
	 */
	interface ExposureAwareCrudMethods {

		/**
		 * Returns whether the repository exposes the save method.
		 *
		 * @return
		 */
		boolean exposesSave();

		/**
		 * Returns whether the repository exposes the delete method.
		 *
		 * @return
		 */
		boolean exposesDelete();

		/**
		 * Returns whether the repository exposes the method to find a single object.
		 *
		 * @return
		 */
		boolean exposesFindOne();

		/**
		 * Returns whether the repository exposes the method to find all objects.
		 *
		 * @return
		 */
		boolean exposesFindAll();
	}
}
