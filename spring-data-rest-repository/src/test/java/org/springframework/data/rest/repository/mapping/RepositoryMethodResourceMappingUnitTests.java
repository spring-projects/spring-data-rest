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
package org.springframework.data.rest.repository.mapping;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.lang.reflect.Method;

import org.junit.Test;
import org.springframework.data.repository.Repository;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.repository.annotation.RestResource;

/**
 * @author Oliver Gierke
 */
public class RepositoryMethodResourceMappingUnitTests {

	RepositoryCollectionResourceMapping resourceMapping = new RepositoryCollectionResourceMapping(
			PersonRepository.class);
	
	@Test
	public void foo() throws Exception {

		Method method = PersonRepository.class.getMethod("findByLastname", String.class);
		ResourceMapping mapping = new RepositoryMethodResourceMapping(method, resourceMapping);

		assertThat(mapping.getPath(), is(new Path("person/findByLastname")));
	}
	
	@Test
	public void usesConfiguredNameWithLeadingSlash() throws Exception {

		Method method = PersonRepository.class.getMethod("findByFirstname", String.class);
		ResourceMapping mapping = new RepositoryMethodResourceMapping(method, resourceMapping);

		assertThat(mapping.getPath(), is(new Path("person/bar")));
	}

	static class Person {}

	interface PersonRepository extends Repository<Person, Long> {

		Iterable<Person> findByLastname(String lastname);
		
		@RestResource(path = "/bar")
		Iterable<Person> findByFirstname(String firstname);
		
		@RestResource(path = "foo")
		Iterable<Person> findByEmailAddress(String email);
	}
}
