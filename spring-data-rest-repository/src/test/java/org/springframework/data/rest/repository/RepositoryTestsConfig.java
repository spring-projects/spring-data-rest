package org.springframework.data.rest.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.repository.domain.jpa.ConfiguredPersonRepository;
import org.springframework.data.rest.repository.domain.jpa.JpaRepositoryConfig;
import org.springframework.data.rest.repository.domain.jpa.Person;
import org.springframework.data.rest.repository.domain.jpa.PersonRepository;

/**
 * @author Jon Brisbin
 */
@Configuration
@Import({JpaRepositoryConfig.class})
public class RepositoryTestsConfig {

  @Autowired
  private ApplicationContext appCtx;

  @Bean public Repositories repositories() {
    return new Repositories(appCtx);
  }

  @Bean public RepositoryRestConfiguration config() {
    RepositoryRestConfiguration config = new RepositoryRestConfiguration();

    config.addResourceMappingForDomainType(Person.class)
          .setRel("person");

    config.setResourceMappingForRepository(ConfiguredPersonRepository.class)
          .setRel("people")
          .setPath("people")
          .setExported(false);

    config.setResourceMappingForRepository(PersonRepository.class)
          .setRel("people")
          .setPath("people")
          .addResourceMappingFor("findByFirstName")
          .setRel("firstname")
          .setPath("firstname");

    return config;
  }

}
