/*
 * Copyright 2013 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.data.repository.Repository;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.data.rest.core.mapping.CollectionResourceMapping;
import org.springframework.data.rest.core.mapping.RepositoryCollectionResourceMapping;
import org.springframework.data.rest.core.mapping.ResourceMapping;

/**
 * Unit tests for {@link RepositoryCollectionResourceMapping}.
 * 
 * @author Oliver Gierke
 */
public class RepositoryCollectionResourceMappingUnitTests {

	@Test
	public void buildsDefaultMappingForRepository() {

		CollectionResourceMapping mapping = new RepositoryCollectionResourceMapping(PersonRepository.class);

		assertThat(mapping.getPath(), is(new Path("persons")));
		assertThat(mapping.getRel(), is("persons"));
		assertThat(mapping.getSingleResourceRel(), is("person"));
		assertThat(mapping.isExported(), is(true));
	}

	@Test
	public void honorsAnnotatedsMapping() {

		CollectionResourceMapping mapping = new RepositoryCollectionResourceMapping(AnnotatedPersonRepository.class);

		assertThat(mapping.getPath(), is(new Path("bar")));
		assertThat(mapping.getRel(), is("foo"));
		assertThat(mapping.getSingleResourceRel(), is("annotatedPerson"));
		assertThat(mapping.isExported(), is(false));
	}

	@Test
	public void repositoryAnnotationTrumpsDomainTypeMapping() {

		CollectionResourceMapping mapping = new RepositoryCollectionResourceMapping(
				AnnotatedAnnotatedPersonRepository.class);

		assertThat(mapping.getPath(), is(new Path("/trumpsAll")));
		assertThat(mapping.getRel(), is("foo"));
		assertThat(mapping.getSingleResourceRel(), is("annotatedPerson"));
		assertThat(mapping.isExported(), is(true));
	}

	@Test
	public void doesNotExposeRepositoryForPublicDomainTypeIfRepoIsPackageProtected() {

		ResourceMapping mapping = new RepositoryCollectionResourceMapping(PackageProtectedRepository.class);
		assertThat(mapping.isExported(), is(false));
	}

	public static class Person {}

	@RestResource(path = "bar", rel = "foo", exported = false)
	static class AnnotatedPerson {}

	public interface PersonRepository extends Repository<Person, Long> {}

	interface AnnotatedPersonRepository extends Repository<AnnotatedPerson, Long> {}

	@RestResource(path = "trumpsAll")
	interface AnnotatedAnnotatedPersonRepository extends Repository<AnnotatedPerson, Long> {}

	public static class PublicClass {}

	static interface PackageProtectedRepository extends Repository<PublicClass, Long> {}
}
