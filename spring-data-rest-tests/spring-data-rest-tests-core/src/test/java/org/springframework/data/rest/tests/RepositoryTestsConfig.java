/*
 * Copyright 2012-2025 the original author or authors.
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
package org.springframework.data.rest.tests;

import static org.mockito.Mockito.*;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.support.DefaultRepositoryInvokerFactory;
import org.springframework.data.repository.support.DomainClassConverter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.core.UriToEntityConverter;
import org.springframework.data.rest.core.config.EnumTranslationConfiguration;
import org.springframework.data.rest.core.config.MetadataConfiguration;
import org.springframework.data.rest.core.config.ProjectionDefinitionConfiguration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.RepositoryResourceMappings;
import org.springframework.data.rest.core.support.DefaultSelfLinkProvider;
import org.springframework.data.rest.core.support.EntityLookup;
import org.springframework.data.rest.core.support.SelfLinkProvider;
import org.springframework.data.rest.webmvc.EmbeddedResourcesAssembler;
import org.springframework.data.rest.webmvc.json.PersistentEntityJacksonModule;
import org.springframework.data.rest.webmvc.json.PersistentEntityJacksonModule.LookupObjectSerializer;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.data.rest.webmvc.mapping.DefaultLinkCollector;
import org.springframework.data.rest.webmvc.mapping.LinkCollector;
import org.springframework.data.rest.webmvc.spi.BackendIdConverter.DefaultIdConverter;
import org.springframework.data.rest.webmvc.support.ExcerptProjector;
import org.springframework.data.rest.webmvc.support.PagingAndSortingTemplateVariables;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.hateoas.mediatype.MessageResolver;
import org.springframework.hateoas.mediatype.hal.DefaultCurieProvider;
import org.springframework.hateoas.mediatype.hal.HalJacksonModule;
import org.springframework.hateoas.mediatype.hal.HalJacksonModule.HalHandlerInstantiator;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.LinkRelationProvider;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.hateoas.server.core.EvoInflectorLinkRelationProvider;
import org.springframework.hateoas.server.mvc.RepresentationModelProcessorInvoker;
import org.springframework.plugin.core.PluginRegistry;

/**
 * @author Jon Brisbin
 * @author Greg Turnquist
 * @author Oliver Gierke
 */
@Configuration
public class RepositoryTestsConfig {

	@Autowired ApplicationContext appCtx;
	@Autowired(required = false) List<MappingContext<?, ?>> mappingContexts = Collections.emptyList();

	@Bean
	public Repositories repositories() {
		return new Repositories(appCtx);
	}

	@Bean
	public RepositoryRestConfiguration config() {

		return new RepositoryRestConfiguration(new ProjectionDefinitionConfiguration(), new MetadataConfiguration(),
				mock(EnumTranslationConfiguration.class));
	}

	@Bean
	public DefaultFormattingConversionService defaultConversionService() {

		DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();

		DomainClassConverter<FormattingConversionService> converter = new DomainClassConverter<FormattingConversionService>(
				conversionService);
		converter.setApplicationContext(appCtx);

		return conversionService;
	}

	@Bean
	public PersistentEntities persistentEntities() {
		return new PersistentEntities(mappingContexts);
	}

	@Bean
	public SimpleModule persistentEntityModule() {

		var conversionService = new DefaultConversionService();

		RepositoryResourceMappings mappings = new RepositoryResourceMappings(repositories(), persistentEntities(),
				config());
		EntityLinks entityLinks = new RepositoryEntityLinks(repositories(), mappings, config(),
				mock(PagingAndSortingTemplateVariables.class), PluginRegistry.of(DefaultIdConverter.INSTANCE));
		SelfLinkProvider selfLinkProvider = new DefaultSelfLinkProvider(persistentEntities(), entityLinks,
				Collections.<EntityLookup<?>> emptyList(), conversionService);

		RepositoryInvokerFactory invokerFactory = new DefaultRepositoryInvokerFactory(repositories());
		UriToEntityConverter uriToEntityConverter = new UriToEntityConverter(persistentEntities(), invokerFactory,
				() -> conversionService);

		Associations associations = new Associations(mappings, config());
		LinkCollector collector = new DefaultLinkCollector(persistentEntities(), selfLinkProvider, associations);

		return new PersistentEntityJacksonModule(associations, persistentEntities(), uriToEntityConverter, collector,
				invokerFactory, mock(LookupObjectSerializer.class),
				new RepresentationModelProcessorInvoker(Collections.<RepresentationModelProcessor<?>> emptyList()),
				new EmbeddedResourcesAssembler(persistentEntities(), associations, mock(ExcerptProjector.class)));
	}

	@Bean
	public ObjectMapper objectMapper() {

		LinkRelationProvider relProvider = new EvoInflectorLinkRelationProvider();

		return JsonMapper.builder()
				.addModule(new HalJacksonModule())
				.addModule(persistentEntityModule())
				.handlerInstantiator(new HalHandlerInstantiator(relProvider,
						new DefaultCurieProvider(Collections.emptyMap()), MessageResolver.DEFAULTS_ONLY))
				.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.build();
	}
}
