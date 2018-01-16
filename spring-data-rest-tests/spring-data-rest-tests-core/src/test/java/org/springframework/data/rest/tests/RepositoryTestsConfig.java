/*
 * Copyright 2012-2018 the original author or authors.
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
package org.springframework.data.rest.tests;

import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.support.DefaultRepositoryInvokerFactory;
import org.springframework.data.repository.support.DomainClassConverter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.UriToEntityConverter;
import org.springframework.data.rest.core.config.EnumTranslationConfiguration;
import org.springframework.data.rest.core.config.MetadataConfiguration;
import org.springframework.data.rest.core.config.ProjectionDefinitionConfiguration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.RepositoryResourceMappings;
import org.springframework.data.rest.core.support.DefaultSelfLinkProvider;
import org.springframework.data.rest.core.support.EntityLookup;
import org.springframework.data.rest.core.support.SelfLinkProvider;
import org.springframework.data.rest.core.util.Java8PluginRegistry;
import org.springframework.data.rest.webmvc.EmbeddedResourcesAssembler;
import org.springframework.data.rest.webmvc.json.PersistentEntityJackson2Module;
import org.springframework.data.rest.webmvc.json.PersistentEntityJackson2Module.LookupObjectSerializer;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.data.rest.webmvc.mapping.LinkCollector;
import org.springframework.data.rest.webmvc.spi.BackendIdConverter.DefaultIdConverter;
import org.springframework.data.rest.webmvc.support.ExcerptProjector;
import org.springframework.data.rest.webmvc.support.PagingAndSortingTemplateVariables;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.RelProvider;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.hateoas.core.EvoInflectorRelProvider;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.hateoas.mvc.ResourceProcessorInvoker;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;

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
	public Module persistentEntityModule() {

		RepositoryResourceMappings mappings = new RepositoryResourceMappings(repositories(), persistentEntities(),
				config());
		EntityLinks entityLinks = new RepositoryEntityLinks(repositories(), mappings, config(),
				mock(PagingAndSortingTemplateVariables.class),
				Java8PluginRegistry.of(Arrays.asList(DefaultIdConverter.INSTANCE)));
		SelfLinkProvider selfLinkProvider = new DefaultSelfLinkProvider(persistentEntities(), entityLinks,
				Collections.<EntityLookup<?>> emptyList());

		DefaultRepositoryInvokerFactory invokerFactory = new DefaultRepositoryInvokerFactory(repositories());
		UriToEntityConverter uriToEntityConverter = new UriToEntityConverter(persistentEntities(), invokerFactory,
				repositories());

		Associations associations = new Associations(mappings, config());
		LinkCollector collector = new LinkCollector(persistentEntities(), selfLinkProvider, associations);

		return new PersistentEntityJackson2Module(associations, persistentEntities(), uriToEntityConverter, collector,
				invokerFactory, mock(LookupObjectSerializer.class),
				new ResourceProcessorInvoker(Collections.<ResourceProcessor<?>> emptyList()),
				new EmbeddedResourcesAssembler(persistentEntities(), associations, mock(ExcerptProjector.class)));
	}

	@Bean
	public ObjectMapper objectMapper() {

		RelProvider relProvider = new EvoInflectorRelProvider();
		ObjectMapper mapper = new ObjectMapper();

		mapper.registerModule(new Jackson2HalModule());
		mapper.registerModule(persistentEntityModule());
		mapper.setHandlerInstantiator(new Jackson2HalModule.HalHandlerInstantiator(relProvider, null, null));
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setSerializationInclusion(Include.NON_EMPTY);

		return mapper;
	}
}
