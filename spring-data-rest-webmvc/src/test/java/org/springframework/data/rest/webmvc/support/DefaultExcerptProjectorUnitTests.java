/*
 * Copyright 2019-2020 the original author or authors.
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
package org.springframework.data.rest.webmvc.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.Test;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;

/**
 * Unit test for {@link DefaultExcerptProjector}.
 *
 * @author Oliver Drotbohm
 */
public class DefaultExcerptProjectorUnitTests {

	ProjectionFactory factory = mock(ProjectionFactory.class);
	ResourceMappings mappings = mock(ResourceMappings.class);
	ResourceMetadata metadata = mock(ResourceMetadata.class);

	@Test // DATAREST-1446
	public void doesNotHaveExcerptProjectionIfMetadataReturnsNone() {

		when(mappings.getMetadataFor(Object.class)).thenReturn(metadata);
		when(metadata.getExcerptProjection()).thenReturn(Optional.empty());

		DefaultExcerptProjector projector = new DefaultExcerptProjector(factory, mappings);

		assertThat(projector.hasExcerptProjection(Object.class)).isFalse();
	}
}
