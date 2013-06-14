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

import org.junit.Test;
import org.springframework.data.rest.repository.annotation.RestResource;
import org.springframework.data.rest.repository.support.SimpleRelProvider;

/**
 *
 * @author Oliver Gierke
 */
public class RepositoryAwareResourceMappingFactoryUnitTests {

	ResourceMappingFactory factory = new ResourceMappingFactory(new SimpleRelProvider());
	
	@Test
	public void foo() {
		
		CollectionResourceMapping mapping = factory.getMappingForType(Person.class);
		assertThat(mapping.getPath(), is("person"));
		assertThat(mapping.getRel(), is("person"));
		assertThat(mapping.getSingleResourceRel(), is("person.person"));
	}
	
	@Test
	public void honorsAnnotatedMapping() {
		
		CollectionResourceMapping mapping = factory.getMappingForType(AnnotatedPerson.class);
		assertThat(mapping.getPath(), is("bar"));
		assertThat(mapping.getRel(), is("foo"));
		assertThat(mapping.getSingleResourceRel(), is("foo.foo"));
		assertThat(mapping.isExported(), is(false));
	}
	
	static class Person {
		
	}
	
	@RestResource(path = "bar", rel = "foo", exported = false)
	static class AnnotatedPerson {
		
	}
}
