/*
 * Copyright 2020-2022 the original author or authors.
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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.RepositoryResourceMappings;
import org.springframework.data.rest.webmvc.alps.AlpsController;
import org.springframework.data.rest.webmvc.json.JsonSchema;
import org.springframework.data.rest.webmvc.json.PersistentEntityToJsonSchemaConverter;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.server.EntityLinks;

/**
 * Configuration class registering required {@link org.springframework.stereotype.Component components} that declare
 * request mappings as {@link Bean beans}.
 *
 * @author Christoph Strobl
 * @since 3.4
 */
@Configuration(proxyBeanMethods = false)
public class RestControllerConfiguration {

	private final RepositoryRestConfiguration restConfiguration;
	private final RepositoryResourceMappings resourceMappings;
	private final PagedResourcesAssembler<Object> resourcesAssembler;
	private final Repositories repositories;

	RestControllerConfiguration(RepositoryRestConfiguration restConfiguration,
			RepositoryResourceMappings resourceMappings, PagedResourcesAssembler<Object> resourcesAssembler,
			Repositories repositories) {

		this.restConfiguration = restConfiguration;
		this.resourceMappings = resourceMappings;
		this.resourcesAssembler = resourcesAssembler;
		this.repositories = repositories;
	}

	/**
	 * The controller for the root resource exposing links to the repository resources.
	 *
	 * @param entityLinks the accessor to links pointing to controllers backing an entity type. Must not be
	 *          {@literal null}.
	 * @return never {@literal null}.
	 */
	@Bean
	RepositoryController repositoryController(EntityLinks entityLinks) {
		return new RepositoryController(resourcesAssembler, repositories, entityLinks, resourceMappings);
	}

	/**
	 * The root controller for entities reachable via {@code /{repository}}.
	 *
	 * @param entityLinks the accessor to links pointing to controllers backing an entity type. Must not be *
	 *          {@literal null}.
	 * @param headersPreparer must not be {@literal null}.
	 * @return never {@literal null}.
	 */
	@Bean
	RepositoryEntityController repositoryEntityController(RepositoryEntityLinks entityLinks,
			HttpHeadersPreparer headersPreparer) {
		return new RepositoryEntityController(repositories, restConfiguration, entityLinks, resourcesAssembler,
				headersPreparer);
	}

	/**
	 * The controller to access referenced properties via {@code /{repository}/{id}/{property}}.
	 *
	 * @param repositoryInvokerFactory must not be {@literal null}.
	 * @return never {@literal null}.
	 */
	@Bean
	RepositoryPropertyReferenceController repositoryPropertyReferenceController(
			RepositoryInvokerFactory repositoryInvokerFactory) {
		return new RepositoryPropertyReferenceController(repositories, repositoryInvokerFactory, resourcesAssembler);
	}

	/**
	 * The controller that performs lookups and executes searches.
	 *
	 * @param entityLinks he accessor to links pointing to controllers backing an entity type. Must not be *
	 *          {@literal null}.
	 * @param headersPreparer must not be {@literal null}.
	 * @return never {@literal null}.
	 */
	@Bean
	RepositorySearchController repositorySearchController(RepositoryEntityLinks entityLinks,
			HttpHeadersPreparer headersPreparer) {
		return new RepositorySearchController(resourcesAssembler, entityLinks, resourceMappings, headersPreparer);
	}

	/**
	 * The controller that exposes the JSON schema via {@code /repository/schema}.
	 *
	 * @param jsonSchemaConverter the converter to create the {@link JsonSchema}. Must not be {@literal null}.
	 * @return never {@literal null}.
	 */
	@Bean
	RepositorySchemaController repositorySchemaController(
			PersistentEntityToJsonSchemaConverter jsonSchemaConverter) {
		return new RepositorySchemaController(jsonSchemaConverter);
	}

	/**
	 * The controller that exposes semantic documentation in the <a href="http://alps.io/">ALPS</a> (Application Level
	 * Profile Semantics) format.
	 *
	 * @return never {@literal null}.
	 */
	@Bean
	AlpsController alpsController() {
		return new AlpsController(restConfiguration);
	}

	/**
	 * Profile-based controller exposing multiple forms of metadata via {@code /profile}.
	 *
	 * @return never {@literal null}.
	 */
	@Bean
	ProfileController profileController() {
		return new ProfileController(restConfiguration, resourceMappings, repositories);
	}
}
