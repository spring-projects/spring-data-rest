/*
 * Copyright 2015-2018 original author or authors.
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
import static org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy.RepositoryDetectionStrategies.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy.RepositoryDetectionStrategies;

/**
 * Unit tests for {@link RepositoryDetectionStrategies}.
 *
 * @author Oliver Gierke
 * @soundtrack Katinka - Ausverkauf
 */
@SuppressWarnings("serial")
public class RepositoryDetectionStrategiesUnitTests {

	@Test // DATAREST-473
	public void allExposesAllRepositories() {

		assertExposures(ALL, new HashMap<Class<?>, Boolean>() {
			{
				put(AnnotatedRepository.class, true);
				put(HiddenRepository.class, true);
				put(PublicRepository.class, true);
				put(PackageProtectedRepository.class, true);
			}
		});
	}

	@Test // DATAREST-473
	public void defaultHonorsVisibilityAndAnnotations() {

		assertExposures(DEFAULT, new HashMap<Class<?>, Boolean>() {
			{
				put(AnnotatedRepository.class, true);
				put(HiddenRepository.class, false);
				put(PublicRepository.class, true);
				put(PackageProtectedRepository.class, false);
			}
		});
	}

	@Test // DATAREST-473
	public void visibilityHonorsTypeVisibilityOnly() {

		assertExposures(VISIBILITY, new HashMap<Class<?>, Boolean>() {
			{
				put(AnnotatedRepository.class, false);
				put(HiddenRepository.class, true);
				put(PublicRepository.class, true);
				put(PackageProtectedRepository.class, false);
			}
		});
	}

	@Test // DATAREST-473
	public void annotatedHonorsAnnotationsOnly() {

		assertExposures(ANNOTATED, new HashMap<Class<?>, Boolean>() {
			{
				put(AnnotatedRepository.class, true);
				put(HiddenRepository.class, false);
				put(PublicRepository.class, false);
				put(PackageProtectedRepository.class, false);
			}
		});
	}

	@Test // DATAREST-1176
	public void onlyExplicitAnnotatedMethodsAreExposed() {

		assertExposures(EXPLICIT_METHOD_ANNOTATED, new HashMap<Class<?>, Boolean>() {
			{
				put(AnnotatedRepository.class, true);
				put(HiddenRepository.class, false);
				put(PublicRepository.class, false);
				put(PackageProtectedRepository.class, false);
			}
		});
	}

	private static void assertExposures(RepositoryDetectionStrategy strategy, Map<Class<?>, Boolean> expected) {

		for (Entry<Class<?>, Boolean> entry : expected.entrySet()) {
			assertThat(strategy.isExported(new DefaultRepositoryMetadata(entry.getKey()))).isEqualTo(entry.getValue());
		}
	}

	interface PackageProtectedRepository extends Repository<Object, Long> {}

	public interface PublicRepository extends Repository<Object, Long> {}

	@RepositoryRestResource
	interface AnnotatedRepository extends Repository<Object, Long> {}

	@RepositoryRestResource(exported = false)
	public interface HiddenRepository extends Repository<Object, Long> {}
}
