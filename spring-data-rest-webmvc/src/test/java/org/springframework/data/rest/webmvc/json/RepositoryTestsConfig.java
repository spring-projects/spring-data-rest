/*
 * Copyright 2012-2015 the original author or authors.
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
package org.springframework.data.rest.webmvc.json;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.support.DomainClassConverter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.UriToEntityConverter;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.RepositoryResourceMappings;
import org.springframework.data.rest.webmvc.jpa.JpaRepositoryConfig;
import org.springframework.data.rest.webmvc.jpa.Person;
import org.springframework.data.rest.webmvc.jpa.PersonRepository;
import org.springframework.data.rest.webmvc.mongodb.MongoDbRepositoryConfig;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.hateoas.RelProvider;
import org.springframework.hateoas.core.EvoInflectorRelProvider;
import org.springframework.hateoas.hal.Jackson2HalModule;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Jon Brisbin
 * @author Greg Trunquist
 * @author Oliver Gierke
 */
@Configuration
@Import({ JpaRepositoryConfig.class, MongoDbRepositoryConfig.class })
@SuppressWarnings("deprecation")
public class RepositoryTestsConfig {

	@Autowired ApplicationContext appCtx;
	@Autowired(required = false) List<MappingContext<?, ?>> mappingContexts = Collections.emptyList();

	@Bean
	public Repositories repositories() {
		return new Repositories(appCtx);
	}

	@Bean
	public RepositoryRestConfiguration config() {
		RepositoryRestConfiguration config = new RepositoryRestConfiguration();

		config.setResourceMappingForDomainType(Person.class).setRel("person");

		config.setResourceMappingForRepository(PersonRepository.class).setRel("people").setPath("people")
				.addResourceMappingFor("findByFirstName").setRel("firstname").setPath("firstname");

		return config;
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
		return new PersistentEntityJackson2Module(new RepositoryResourceMappings(config(), repositories()),
				persistentEntities(), config(), new UriToEntityConverter(persistentEntities(), defaultConversionService()));
	}

	@Bean
	public ObjectMapper objectMapper() {

		RelProvider relProvider = new EvoInflectorRelProvider();
		ObjectMapper mapper = new ObjectMapper();

		mapper.registerModule(new Jackson2HalModule());
		mapper.registerModule(persistentEntityModule());
		mapper.setHandlerInstantiator(new Jackson2HalModule.HalHandlerInstantiator(relProvider, null));
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setSerializationInclusion(Include.NON_EMPTY);

		return mapper;
	}
}
