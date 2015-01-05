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
package org.springframework.data.rest.core;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.domain.jpa.ConfiguredPersonRepository;
import org.springframework.data.rest.core.domain.jpa.JpaRepositoryConfig;
import org.springframework.data.rest.core.domain.jpa.Person;
import org.springframework.data.rest.core.domain.jpa.PersonRepository;
import org.springframework.format.support.DefaultFormattingConversionService;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
@Configuration
@Import({ JpaRepositoryConfig.class })
public class RepositoryTestsConfig {

	@Autowired private ApplicationContext context;
	@Autowired(required = false) List<MappingContext<?, ?>> mappingContexts = Collections.emptyList();

	@Bean
	public Repositories repositories() {
		return new Repositories(context);
	}

	@SuppressWarnings("deprecation")
	@Bean
	public RepositoryRestConfiguration config() {
		RepositoryRestConfiguration config = new RepositoryRestConfiguration();

		config.setResourceMappingForDomainType(Person.class).setRel("person");

		config.setResourceMappingForRepository(ConfiguredPersonRepository.class).setRel("people").setPath("people")
				.setExported(false);

		config.setResourceMappingForRepository(PersonRepository.class).setRel("people").setPath("people")
				.addResourceMappingFor("findByFirstName").setRel("firstname").setPath("firstname");

		return config;
	}

	@Bean
	public DefaultFormattingConversionService defaultConversionService() {
		return new DefaultFormattingConversionService();
	}

	@Bean
	public PersistentEntities persistentEntities() {
		return new PersistentEntities(mappingContexts);
	}
}
