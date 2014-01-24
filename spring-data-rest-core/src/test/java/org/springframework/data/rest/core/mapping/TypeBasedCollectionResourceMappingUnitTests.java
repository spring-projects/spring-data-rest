/*
 * Copyright 2013-2014 the original author or authors.
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
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.annotation.RestResource;

/**
 * Unit tests for {@link TypeBasedCollectionResourceMapping}.
 * 
 * @author Oliver Gierke
 */
public class TypeBasedCollectionResourceMappingUnitTests {

	@Test
	public void defaultsMappingsByType() {

		CollectionResourceMapping mapping = new TypeBasedCollectionResourceMapping(Sample.class);

		assertThat(mapping.getPath(), is(new Path("sample")));
		assertThat(mapping.getRel(), is("samples"));
		assertThat(mapping.getItemResourceRel(), is("sample"));
		assertThat(mapping.isExported(), is(true));
	}

	@Test
	public void usesCustomizedRel() {

		CollectionResourceMapping mapping = new TypeBasedCollectionResourceMapping(CustomizedSample.class);

		assertThat(mapping.getPath(), is(new Path("customizedSample")));
		assertThat(mapping.getRel(), is("myRel"));
		assertThat(mapping.getItemResourceRel(), is("customizedSample"));
		assertThat(mapping.isExported(), is(true));
	}

	/**
	 * @see DATAREST-99
	 */
	@Test
	public void doesNotExportNonPublicTypesByDefault() {

		CollectionResourceMapping mapping = new TypeBasedCollectionResourceMapping(HiddenSample.class);

		assertThat(mapping.isExported(), is(false));
	}

	/**
	 * @see
	 */
	@Test
	public void usesDefaultDescriptionIfNoAnnotationPresent() {

		CollectionResourceMapping mapping = new TypeBasedCollectionResourceMapping(Sample.class);
		ResourceDescription description = mapping.getDescription();

		assertThat(description.isDefault(), is(true));
		assertThat(description.getMessage(), is("rest.description.samples"));

		ResourceDescription itemDescription = mapping.getItemResourceDescription();

		assertThat(itemDescription.isDefault(), is(true));
		assertThat(itemDescription.getMessage(), is("rest.description.sample"));
	}

	public interface Sample {}

	interface HiddenSample {}

	@RestResource(rel = "myRel")
	interface CustomizedSample {

	}
}
