/*
 * Copyright 2014-2015 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.rest.core.mapping.ResourceType.*;
import static org.springframework.http.HttpMethod.*;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.annotation.Reference;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.CrudMethods;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultCrudMethods;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.http.HttpMethod;

/**
 * Unit tests for {@link CrudMethodsSupportedHttpMethods}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class CrudMethodsSupportedHttpMethodsUnitTests {

	/**
	 * @see DATACMNS-589, DATAREST-409
	 */
	@Test
	public void doesNotSupportAnyHttpMethodForEmptyRepository() {

		SupportedHttpMethods supportedMethods = getSupportedHttpMethodsFor(RawRepository.class);

		assertMethodsSupported(supportedMethods, COLLECTION, true, OPTIONS);
		assertMethodsSupported(supportedMethods, COLLECTION, false, GET, PUT, POST, PATCH, DELETE, HEAD);

		assertMethodsSupported(supportedMethods, ITEM, true, OPTIONS);
		assertMethodsSupported(supportedMethods, ITEM, false, GET, PUT, POST, PATCH, DELETE, HEAD);
	}

	/**
	 * @see DATAREST-217, DATAREST-330, DATACMNS-589, DATAREST-409
	 */
	@Test
	public void defaultsSupportedHttpMethodsForItemResource() {

		SupportedHttpMethods supportedHttpMethods = getSupportedHttpMethodsFor(SampleRepository.class);

		assertMethodsSupported(supportedHttpMethods, ITEM, true, GET, PUT, PATCH, DELETE, OPTIONS, HEAD);
		assertMethodsSupported(supportedHttpMethods, ITEM, false, POST);
	}

	/**
	 * @see DATAREST-217, DATAREST-330, DATACMNS-589, DATAREST-409
	 */
	@Test
	public void defaultsSupportedHttpMethodsForCollectionResource() {

		SupportedHttpMethods supportedHttpMethods = getSupportedHttpMethodsFor(SampleRepository.class);

		assertMethodsSupported(supportedHttpMethods, COLLECTION, true, GET, POST, OPTIONS, HEAD);
		assertMethodsSupported(supportedHttpMethods, COLLECTION, false, PUT, PATCH, DELETE);
	}

	/**
	 * @see DATACMNS-589, DATAREST-409
	 */
	@Test
	public void doesNotSupportDeleteIfDeleteMethodIsNotExported() {

		SupportedHttpMethods supportedHttpMethods = getSupportedHttpMethodsFor(HidesDelete.class);

		assertMethodsSupported(supportedHttpMethods, ITEM, false, DELETE);
	}

	/**
	 * @see DATAREST-523
	 */
	@Test
	public void exposesMethodsForProperties() {

		MongoMappingContext context = new MongoMappingContext();
		MongoPersistentEntity<?> entity = context.getPersistentEntity(Entity.class);

		SupportedHttpMethods methods = getSupportedHttpMethodsFor(EntityRepository.class);

		assertThat(methods.getMethodsFor(entity.getPersistentProperty("embedded")), is(empty()));
		assertThat(methods.getMethodsFor(entity.getPersistentProperty("embeddedCollection")), is(empty()));

		assertThat(methods.getMethodsFor(entity.getPersistentProperty("related")),
				allOf(hasItems(GET, DELETE, PATCH, PUT), not(hasItem(POST))));

		assertThat(methods.getMethodsFor(entity.getPersistentProperty("relatedCollection")),
				hasItems(GET, DELETE, PATCH, PUT, POST));

		assertThat(methods.getMethodsFor(entity.getPersistentProperty("readOnlyReference")),
				allOf(hasItem(GET), not(hasItems(DELETE, PATCH, PUT, POST))));
	}

	private static SupportedHttpMethods getSupportedHttpMethodsFor(Class<?> repositoryInterface) {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(repositoryInterface);
		CrudMethods crudMethods = new DefaultCrudMethods(metadata);

		return new CrudMethodsSupportedHttpMethods(crudMethods);
	}

	private static void assertMethodsSupported(SupportedHttpMethods methods, ResourceType type, boolean supported,
			HttpMethod... httpMethods) {

		Set<HttpMethod> result = methods.getMethodsFor(type);

		assertThat(result, supported ? hasItems(httpMethods) : not(hasItems(httpMethods)));

		if (supported) {
			assertThat(result, hasSize(httpMethods.length));
		}
	}

	interface RawRepository extends Repository<Object, Long> {}

	interface SampleRepository extends CrudRepository<Object, Long> {}

	interface HidesDelete extends CrudRepository<Object, Long> {

		@RestResource(exported = false)
		void delete(Object id);
	}

	interface EntityRepository extends CrudRepository<Entity, Long> {}

	class Entity {

		Entity embedded;
		@Reference Entity related;
		List<Entity> embeddedCollection;
		@Reference List<Entity> relatedCollection;
		@ReadOnlyProperty @Reference Entity readOnlyReference;
	}
}
