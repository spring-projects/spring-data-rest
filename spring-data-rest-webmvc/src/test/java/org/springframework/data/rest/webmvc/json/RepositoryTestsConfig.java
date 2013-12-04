package org.springframework.data.rest.webmvc.json;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.repository.support.DomainClassConverter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.UriDomainClassConverter;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.jpa.JpaRepositoryConfig;
import org.springframework.data.rest.webmvc.jpa.Person;
import org.springframework.data.rest.webmvc.jpa.PersonRepository;
import org.springframework.format.support.DefaultFormattingConversionService;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Jon Brisbin
 */
@Configuration
@Import({ JpaRepositoryConfig.class })
@SuppressWarnings("deprecation")
public class RepositoryTestsConfig {

	@Autowired private ApplicationContext appCtx;

	@Bean
	public Repositories repositories() {
		return new Repositories(appCtx);
	}

	@Bean
	public RepositoryRestConfiguration config() {
		RepositoryRestConfiguration config = new RepositoryRestConfiguration();

		config.setResourceMappingForDomainType(Person.class).setRel("person");

		// config.setResourceMappingForRepository(ConfiguredPersonRepository.class)
		// .setRel("people")
		// .setPath("people")
		// .setExported(false);

		config.setResourceMappingForRepository(PersonRepository.class).setRel("people").setPath("people")
				.addResourceMappingFor("findByFirstName").setRel("firstname").setPath("firstname");

		config.setBaseUri(URI.create("http://localhost:8080"));

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

	@Bean
	public Module persistentEntityModule() {
		return new PersistentEntityJackson2Module(new ResourceMappings(config(), repositories()),
				defaultConversionService());
	}

	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(persistentEntityModule());
		return mapper;
	}
}
