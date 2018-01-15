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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.data.rest.core.mapping.ResourceType.*;
import static org.springframework.http.HttpMethod.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.annotation.Reference;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentEntity;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
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

	@Mock RepositoryResourceMappings mappings;

	@Before
	public void setUp() {
		when(mappings.exposeMethodsByDefault()).thenReturn(true);
	}

	@Test // DATACMNS-589, DATAREST-409
	public void doesNotSupportAnyHttpMethodForEmptyRepository() {

		SupportedHttpMethods supportedMethods = getSupportedHttpMethodsFor(RawRepository.class);

		assertMethodsSupported(supportedMethods, COLLECTION, true, OPTIONS);
		assertMethodsSupported(supportedMethods, COLLECTION, false, GET, PUT, POST, PATCH, DELETE, HEAD);

		assertMethodsSupported(supportedMethods, ITEM, true, OPTIONS);
		assertMethodsSupported(supportedMethods, ITEM, false, GET, PUT, POST, PATCH, DELETE, HEAD);
	}

	@Test // DATAREST-217, DATAREST-330, DATACMNS-589, DATAREST-409
	public void defaultsSupportedHttpMethodsForItemResource() {

		SupportedHttpMethods supportedHttpMethods = getSupportedHttpMethodsFor(SampleRepository.class);

		assertMethodsSupported(supportedHttpMethods, ITEM, true, GET, PUT, PATCH, DELETE, OPTIONS, HEAD);
		assertMethodsSupported(supportedHttpMethods, ITEM, false, POST);
	}

	@Test // DATAREST-217, DATAREST-330, DATACMNS-589, DATAREST-409
	public void defaultsSupportedHttpMethodsForCollectionResource() {

		SupportedHttpMethods supportedHttpMethods = getSupportedHttpMethodsFor(SampleRepository.class);

		assertMethodsSupported(supportedHttpMethods, COLLECTION, true, GET, POST, OPTIONS, HEAD);
		assertMethodsSupported(supportedHttpMethods, COLLECTION, false, PUT, PATCH, DELETE);
	}

	@Test // DATACMNS-589, DATAREST-409
	public void doesNotSupportDeleteIfDeleteMethodIsNotExported() {

		SupportedHttpMethods supportedHttpMethods = getSupportedHttpMethodsFor(HidesDelete.class);

		assertMethodsSupported(supportedHttpMethods, ITEM, false, DELETE);
	}

	@Test // DATAREST-523
	public void exposesMethodsForProperties() {

		KeyValueMappingContext<?, ?> context = new KeyValueMappingContext<>();
		KeyValuePersistentEntity<?, ?> entity = context.getRequiredPersistentEntity(Entity.class);

		SupportedHttpMethods methods = getSupportedHttpMethodsFor(EntityRepository.class);

		assertThat(methods.getMethodsFor(entity.getRequiredPersistentProperty("embedded"))).isEmpty();
		assertThat(methods.getMethodsFor(entity.getRequiredPersistentProperty("embeddedCollection"))).isEmpty();

		assertThat(methods.getMethodsFor(entity.getRequiredPersistentProperty("related")))//
				.contains(GET, DELETE, PATCH, PUT)//
				.doesNotContain(POST);

		assertThat(methods.getMethodsFor(entity.getRequiredPersistentProperty("relatedCollection")))//
				.contains(GET, DELETE, PATCH, PUT, POST);

		assertThat(methods.getMethodsFor(entity.getRequiredPersistentProperty("readOnlyReference")))//
				.contains(GET)//
				.doesNotContain(DELETE, PATCH, PUT, POST);
	}

	@Test // DATAREST-825
	public void supportsDeleteIfFindOneIsHidden() {
		assertMethodsSupported(getSupportedHttpMethodsFor(HidesFindOne.class), ITEM, true, DELETE, PATCH, PUT, OPTIONS);
	}

	@Test // DATAREST-825
	public void doesNotSupportDeleteIfNoFindOneAvailable() {
		assertMethodsSupported(getSupportedHttpMethodsFor(NoFindOne.class), ITEM, false, DELETE);
	}

	@Test // DATAREST-1176
	public void onlyExposesExplicitlyAnnotatedMethodsIfConfigured() {

		reset(mappings);
		when(mappings.exposeMethodsByDefault()).thenReturn(false);

		assertMethodsSupported(getSupportedHttpMethodsFor(MethodsExplicitlyExportedRepository.class), COLLECTION, true,
				POST, OPTIONS);
		assertMethodsSupported(getSupportedHttpMethodsFor(MethodsExplicitlyExportedRepository.class), ITEM, true, OPTIONS,
				PUT, PATCH);
	}

	private SupportedHttpMethods getSupportedHttpMethodsFor(Class<?> repositoryInterface) {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(repositoryInterface);
		CrudMethods crudMethods = new DefaultCrudMethods(metadata);

		return new CrudMethodsSupportedHttpMethods(crudMethods, mappings.exposeMethodsByDefault());
	}

	private static void assertMethodsSupported(SupportedHttpMethods methods, ResourceType type, boolean supported,
			HttpMethod... httpMethods) {

		Set<HttpMethod> result = methods.getMethodsFor(type);

		if (supported) {
			assertThat(result).containsExactlyInAnyOrder(httpMethods);
		} else {
			assertThat(result).doesNotContain(httpMethods);
		}
	}

	interface RawRepository extends Repository<Object, Long> {}

	interface SampleRepository extends CrudRepository<Object, Long> {}

	interface HidesDelete extends CrudRepository<Object, Long> {

		@RestResource(exported = false)
		void delete(Object entity);
	}

	interface HidesFindOne extends CrudRepository<Object, Long> {

		@Override
		@RestResource(exported = false)
		Optional<Object> findById(Long id);
	}

	interface NoFindOne extends Repository<Object, Long> {
		void delete(Object entity);
	}

	interface EntityRepository extends CrudRepository<Entity, Long> {}

	interface MethodsExplicitlyExportedRepository extends Repository<Object, Long> {

		@RestResource
		<S extends Object> S save(S entity);

		@RestResource
		void delete(Object entity);
	}

	class Entity {

		Entity embedded;
		@Reference Entity related;
		List<Entity> embeddedCollection;
		@Reference List<Entity> relatedCollection;
		@ReadOnlyProperty @Reference Entity readOnlyReference;
	}
}
