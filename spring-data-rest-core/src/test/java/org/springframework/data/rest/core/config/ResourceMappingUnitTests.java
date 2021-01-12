/*
 * Copyright 2012-2021 the original author or authors.
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
package org.springframework.data.rest.core.config;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.rest.core.support.ResourceMappingUtils.*;

import java.lang.reflect.Method;

import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.data.rest.core.domain.Person;
import org.springframework.data.rest.core.mapping.ResourceMapping;
import org.springframework.hateoas.server.LinkRelationProvider;
import org.springframework.hateoas.server.core.EvoInflectorLinkRelationProvider;

/**
 * Ensure the {@link ResourceMapping} components convey the correct information.
 *
 * @author Jon Brisbin
 */
@SuppressWarnings("deprecation")
public class ResourceMappingUnitTests {

	LinkRelationProvider relProvider = new EvoInflectorLinkRelationProvider();

	@Test
	public void shouldDetectPathAndRemoveLeadingSlashIfAny() {

		org.springframework.data.rest.core.config.ResourceMapping mapping = new org.springframework.data.rest.core.config.ResourceMapping(
				findRel(AnnotatedWithLeadingSlashPersonRepository.class),
				findPath(AnnotatedWithLeadingSlashPersonRepository.class),
				findExported(AnnotatedWithLeadingSlashPersonRepository.class));

		// The rel attribute defaults to class name
		assertThat(mapping.getRel()).isEqualTo("annotatedWithLeadingSlashPerson");
		assertThat(mapping.getPath()).isEqualTo("people");
		// The exported defaults to true
		assertThat(mapping.isExported()).isTrue();
	}

	@Test
	public void shouldDetectPathAndRemoveLeadingSlashIfAnyOnMethod() throws Exception {
		Method method = AnnotatedWithLeadingSlashPersonRepository.class.getMethod("findByFirstName", String.class,
				Pageable.class);
		org.springframework.data.rest.core.config.ResourceMapping mapping = new org.springframework.data.rest.core.config.ResourceMapping(
				findRel(method), findPath(method), findExported(method));

		// The rel attribute defaults to class name
		assertThat(mapping.getRel()).isEqualTo("findByFirstName");
		assertThat(mapping.getPath()).isEqualTo("firstname");
		// The exported defaults to true
		assertThat(mapping.isExported()).isTrue();
	}

	@Test
	public void shouldReturnDefaultIfPathContainsOnlySlashTextOnMethod() throws Exception {
		Method method = AnnotatedWithLeadingSlashPersonRepository.class.getMethod("findByLastName", String.class,
				Pageable.class);
		org.springframework.data.rest.core.config.ResourceMapping mapping = new org.springframework.data.rest.core.config.ResourceMapping(
				findRel(method), findPath(method), findExported(method));

		// The rel defaults to method name
		assertThat(mapping.getRel()).isEqualTo("findByLastName");
		// The path contains only a leading slash therefore defaults to method name
		assertThat(mapping.getPath()).isEqualTo("findByLastName");
		// The exported defaults to true
		assertThat(mapping.isExported()).isTrue();
	}

	@RestResource(path = "/people")
	interface AnnotatedWithLeadingSlashPersonRepository {

		@RestResource(path = "/firstname")
		Page<Person> findByFirstName(@Param("firstName") String firstName, Pageable pageable);

		@RestResource(path = " / ")
		Page<Person> findByLastName(@Param("lastName") String firstName, Pageable pageable);
	}
}
