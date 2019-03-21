/*
 * Copyright 2013-2018 the original author or authors.
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

		assertThat(mapping.getPath()).isEqualTo(new Path("sample"));
		assertThat(mapping.getRel()).isEqualTo("samples");
		assertThat(mapping.getItemResourceRel()).isEqualTo("sample");
		assertThat(mapping.isExported()).isTrue();
	}

	@Test
	public void usesCustomizedRel() {

		CollectionResourceMapping mapping = new TypeBasedCollectionResourceMapping(CustomizedSample.class);

		assertThat(mapping.getPath()).isEqualTo(new Path("customizedSample"));
		assertThat(mapping.getRel()).isEqualTo("myRel");
		assertThat(mapping.getItemResourceRel()).isEqualTo("customizedSample");
		assertThat(mapping.isExported()).isTrue();
	}

	@Test // DATAREST-99
	public void doesNotExportNonPublicTypesByDefault() {

		CollectionResourceMapping mapping = new TypeBasedCollectionResourceMapping(HiddenSample.class);

		assertThat(mapping.isExported()).isFalse();
	}

	/**
	 * @see
	 */
	@Test
	public void usesDefaultDescriptionIfNoAnnotationPresent() {

		CollectionResourceMapping mapping = new TypeBasedCollectionResourceMapping(Sample.class);
		ResourceDescription description = mapping.getDescription();

		assertThat(description.isDefault()).isTrue();
		assertThat(description.getMessage()).isEqualTo("rest.description.samples");

		ResourceDescription itemDescription = mapping.getItemResourceDescription();

		assertThat(itemDescription.isDefault()).isTrue();
		assertThat(itemDescription.getMessage()).isEqualTo("rest.description.sample");
	}

	public interface Sample {}

	interface HiddenSample {}

	@RestResource(rel = "myRel")
	interface CustomizedSample {

	}
}
