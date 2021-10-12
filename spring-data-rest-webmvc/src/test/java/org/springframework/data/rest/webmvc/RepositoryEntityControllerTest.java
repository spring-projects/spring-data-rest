/*
 * Copyright 2017-2021 the original author or authors.
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
package org.springframework.data.rest.webmvc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentEntity;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.PersistentEntitiesResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.data.web.PagedResourcesAssembler;

/**
 * Unit tests for {@link RepositoryEntityController}
 *
 * @author Jeroen Reijn
 */
@ExtendWith(MockitoExtension.class)
class RepositoryEntityControllerTest {

	@Mock Repositories repositories;
	@Mock RepositoryRestConfiguration restConfiguration;
	@Mock RepositoryEntityLinks repositoryEntityLinks;
	@Mock HttpHeadersPreparer httpHeadersPreparer;
	@Mock RepositoryInvoker invoker;
	@Mock PagedResourcesAssembler<Object> assembler;

	KeyValueMappingContext<?, ?> mappingContext = new KeyValueMappingContext<>();

	@Test // DATAREST-1143
	void testUnknownItemThrowsResourceNotFound() throws Exception {

		KeyValuePersistentEntity<?, ?> entity = mappingContext
				.getRequiredPersistentEntity(RepositoryPropertyReferenceControllerUnitTests.Sample.class);

		ResourceMappings mappings = new PersistentEntitiesResourceMappings(
				new PersistentEntities(Collections.singleton(mappingContext)));

		ResourceMetadata metadata = spy(
				mappings.getMetadataFor(RepositoryPropertyReferenceControllerUnitTests.Sample.class));

		when(metadata.getSupportedHttpMethods())
				.thenReturn(RepositoryPropertyReferenceControllerUnitTests.AllSupportedHttpMethods.INSTANCE);

		RootResourceInformation information = new RootResourceInformation(metadata, entity, invoker);
		RepositoryEntityController repositoryEntityController = new RepositoryEntityController(repositories,
				restConfiguration, repositoryEntityLinks, assembler, httpHeadersPreparer);

		assertThatExceptionOfType(ResourceNotFoundException.class) //
				.isThrownBy(() -> repositoryEntityController.getItemResource(information, "1", null, null));
	}
}
