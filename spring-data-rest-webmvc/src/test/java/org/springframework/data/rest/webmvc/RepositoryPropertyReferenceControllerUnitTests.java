/*
 * Copyright 2016-2022 original author or authors.
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentEntity;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.data.rest.core.mapping.ConfigurableHttpMethods;
import org.springframework.data.rest.core.mapping.HttpMethods;
import org.springframework.data.rest.core.mapping.PersistentEntitiesResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.core.mapping.ResourceType;
import org.springframework.data.rest.core.mapping.SupportedHttpMethods;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for {@link RepositoryPropertyReferenceController}.
 *
 * @author Oliver Gierke
 */
@ExtendWith(MockitoExtension.class)
class RepositoryPropertyReferenceControllerUnitTests {

	@Mock Repositories repositories;
	@Mock RepositoryInvokerFactory invokerFactory;
	@Mock RepositoryInvoker invoker;
	@Mock ApplicationEventPublisher publisher;

	KeyValueMappingContext<?, ?> mappingContext = new KeyValueMappingContext<>();

	@Test // DATAREST-791
	void usesRepositoryInvokerToLookupRelatedInstance() throws Exception {

		KeyValuePersistentEntity<?, ?> entity = mappingContext.getRequiredPersistentEntity(Sample.class);

		ResourceMappings mappings = new PersistentEntitiesResourceMappings(
				new PersistentEntities(Collections.singleton(mappingContext)));
		ResourceMetadata metadata = spy(mappings.getMetadataFor(Sample.class));
		when(metadata.getSupportedHttpMethods()).thenReturn(AllSupportedHttpMethods.INSTANCE);

		RepositoryPropertyReferenceController controller = new RepositoryPropertyReferenceController(repositories,
				invokerFactory);
		controller.setApplicationEventPublisher(publisher);

		doReturn(invoker).when(invokerFactory).getInvokerFor(Reference.class);
		doReturn(Optional.of(new Sample())).when(invoker).invokeFindById(4711);
		doReturn(Optional.of(new Reference())).when(invoker).invokeFindById("some-id");
		doReturn(new Sample()).when(invoker).invokeSave(any(Object.class));

		RootResourceInformation information = new RootResourceInformation(metadata, entity, invoker);
		CollectionModel<Object> request = CollectionModel.empty(Link.of("/reference/some-id"));

		controller.createPropertyReference(information, HttpMethod.POST, request, 4711, "references");

		verify(invokerFactory).getInvokerFor(Reference.class);
		verify(invoker).invokeFindById("some-id");
	}

    @Test // DATAREST-2495
    void rejectsEmptyLinksForAssociationUpdate() throws Exception {

        KeyValuePersistentEntity<?, ?> entity = mappingContext.getRequiredPersistentEntity(Sample.class);

        ResourceMappings mappings = new PersistentEntitiesResourceMappings(
                new PersistentEntities(Collections.singleton(mappingContext)));
        ResourceMetadata metadata = spy(mappings.getMetadataFor(Sample.class));
        when(metadata.getSupportedHttpMethods()).thenReturn(AllSupportedHttpMethods.INSTANCE);

        RepositoryPropertyReferenceController controller = new RepositoryPropertyReferenceController(repositories,
                invokerFactory);
        controller.setApplicationEventPublisher(publisher);

        doReturn(Optional.of(new Sample())).when(invoker).invokeFindById(4711);

        RootResourceInformation information = new RootResourceInformation(metadata, entity, invoker);

        assertThatExceptionOfType(HttpMessageNotReadableException.class)
                .isThrownBy(() -> controller.createPropertyReference(information, HttpMethod.POST, null,  4711,
                        "references"));
    }


    @RestResource
	static class Sample {
		@org.springframework.data.annotation.Reference List<Reference> references = new ArrayList<Reference>();
	}

	@RestResource
	static class Reference {}

	static enum AllSupportedHttpMethods implements SupportedHttpMethods {

		INSTANCE;

		@Override
		public HttpMethods getMethodsFor(PersistentProperty<?> property) {
			return ConfigurableHttpMethods.ALL;
		}

		@Override
		public HttpMethods getMethodsFor(ResourceType type) {
			return ConfigurableHttpMethods.ALL;
		}
	}
}
