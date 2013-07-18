package org.springframework.data.rest.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.repository.support.DomainClassConverter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.UriDomainClassConverter;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.domain.jpa.ConfiguredPersonRepository;
import org.springframework.data.rest.core.domain.jpa.JpaRepositoryConfig;
import org.springframework.data.rest.core.domain.jpa.Person;
import org.springframework.data.rest.core.domain.jpa.PersonRepository;
import org.springframework.format.support.DefaultFormattingConversionService;

/**
 * @author Jon Brisbin
 */
@Configuration
@Import({ JpaRepositoryConfig.class })
public class RepositoryTestsConfig {

	@Autowired private ApplicationContext appCtx;

	@Bean
	public Repositories repositories() {
		return new Repositories(appCtx);
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
	public DomainClassConverter<?> domainClassConverter() {
		return new DomainClassConverter<DefaultFormattingConversionService>(defaultConversionService());
	}

	@Bean
	public UriDomainClassConverter uriDomainClassConverter() {
		return new UriDomainClassConverter(repositories(), domainClassConverter());
	}
}
