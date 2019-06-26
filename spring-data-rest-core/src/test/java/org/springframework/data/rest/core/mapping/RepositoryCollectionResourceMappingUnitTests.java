/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.core.mapping;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy.RepositoryDetectionStrategies;
import org.springframework.hateoas.LinkRelation;

/**
 * Unit tests for {@link RepositoryCollectionResourceMapping}.
 *
 * @author Oliver Gierke
 */
public class RepositoryCollectionResourceMappingUnitTests {

	@Test
	public void buildsDefaultMappingForRepository() {

		CollectionResourceMapping mapping = getResourceMappingFor(PersonRepository.class);

		assertThat(mapping.getPath()).isEqualTo(new Path("persons"));
		assertThat(mapping.getRel()).isEqualTo(LinkRelation.of("persons"));
		assertThat(mapping.getItemResourceRel()).isEqualTo(LinkRelation.of("person"));
		assertThat(mapping.isExported()).isTrue();
	}

	@Test
	public void honorsAnnotatedsMapping() {

		CollectionResourceMapping mapping = getResourceMappingFor(AnnotatedPersonRepository.class);

		assertThat(mapping.getPath()).isEqualTo(new Path("bar"));
		assertThat(mapping.getRel()).isEqualTo(LinkRelation.of("foo"));
		assertThat(mapping.getItemResourceRel()).isEqualTo(LinkRelation.of("annotatedPerson"));
		assertThat(mapping.isExported()).isFalse();
	}

	@Test
	public void repositoryAnnotationTrumpsDomainTypeMapping() {

		CollectionResourceMapping mapping = getResourceMappingFor(AnnotatedAnnotatedPersonRepository.class);

		assertThat(mapping.getPath()).isEqualTo(new Path("/trumpsAll"));
		assertThat(mapping.getRel()).isEqualTo(LinkRelation.of("foo"));
		assertThat(mapping.getItemResourceRel()).isEqualTo(LinkRelation.of("annotatedPerson"));
		assertThat(mapping.isExported()).isTrue();
	}

	@Test
	public void doesNotExposeRepositoryForPublicDomainTypeIfRepoIsPackageProtected() {

		ResourceMapping mapping = getResourceMappingFor(PackageProtectedRepository.class);
		assertThat(mapping.isExported()).isFalse();
	}

	@Test // DATAREST-229
	public void detectsPagingRepository() {
		assertThat(getResourceMappingFor(PersonRepository.class).isPagingResource()).isTrue();
	}

	@Test
	public void discoversCustomizationsUsingRestRepositoryResource() {

		CollectionResourceMapping mapping = getResourceMappingFor(RepositoryAnnotatedRepository.class);
		assertThat(mapping.getRel()).isEqualTo(LinkRelation.of("foo"));
		assertThat(mapping.getItemResourceRel()).isEqualTo(LinkRelation.of("bar"));
	}

	@Test // DATAREST-445
	public void usesDomainTypeFromRepositoryMetadata() {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(PersonRepository.class) {

			@Override
			public Class<?> getDomainType() {
				return Object.class;
			}
		};

		RepositoryCollectionResourceMapping mapping = new RepositoryCollectionResourceMapping(metadata,
				RepositoryDetectionStrategies.DEFAULT);

		assertThat(mapping.getPath()).isEqualTo(new Path("/objects"));
	}

	@Test // DATAREST-1401
	public void rejectsPathsWithMultipleSegments() {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(InvalidPath.class);

		assertThatExceptionOfType(IllegalStateException.class)
				.isThrownBy(() -> new RepositoryCollectionResourceMapping(metadata, RepositoryDetectionStrategies.DEFAULT)) //
				.withMessageContaining("first/second") //
				.withMessageContaining(InvalidPath.class.getName()) //
				.withMessageContaining("path segment");
	}

	@Test // DATAREST-1401
	public void exposesProjectionTypeIfConfigured() {

		assertThat(getResourceMappingFor(WithProjection.class).getExcerptProjection()).hasValue(Object.class);
		assertThat(getResourceMappingFor(WithoutProjection.class).getExcerptProjection()).isEmpty();
	}

	private static CollectionResourceMapping getResourceMappingFor(Class<?> repositoryInterface) {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(repositoryInterface);
		return new RepositoryCollectionResourceMapping(metadata, RepositoryDetectionStrategies.DEFAULT);
	}

	public static class Person {}

	@RestResource(path = "bar", rel = "foo", exported = false)
	static class AnnotatedPerson {}

	public interface PersonRepository extends Repository<Person, Long> {

		Page<Person> findAll(Pageable pageable);
	}

	interface AnnotatedPersonRepository extends Repository<AnnotatedPerson, Long> {}

	@RestResource(path = "trumpsAll")
	interface AnnotatedAnnotatedPersonRepository extends Repository<AnnotatedPerson, Long> {}

	public static class PublicClass {}

	interface PackageProtectedRepository extends Repository<PublicClass, Long> {}

	@RepositoryRestResource(collectionResourceRel = "foo", itemResourceRel = "bar")
	interface RepositoryAnnotatedRepository extends Repository<Person, Long> {}

	@RepositoryRestResource(path = "first/second")
	interface InvalidPath extends Repository<Person, Long> {}

	@RepositoryRestResource(excerptProjection = Object.class)
	interface WithProjection extends Repository<Person, Long> {}

	interface WithoutProjection extends Repository<Person, Long> {}
}
