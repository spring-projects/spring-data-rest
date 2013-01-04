package org.springframework.data.rest.repository.context;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.repository.RepositoryTestsConfig;
import org.springframework.data.rest.repository.domain.jpa.AnnotatedPersonEventHandler;
import org.springframework.data.rest.repository.domain.jpa.PersonBeforeSaveHandler;

/**
 * @author Jon Brisbin
 */
@Configuration
@Import({RepositoryTestsConfig.class})
public class RepositoryEventTestsConfig {

  @Bean public PersonBeforeSaveHandler personBeforeSaveHandler() {
    return new PersonBeforeSaveHandler();
  }

  @Bean public AnnotatedPersonEventHandler beforeSaveHandler() {
    return new AnnotatedPersonEventHandler();
  }

  @Bean public AnnotatedHandlerBeanPostProcessor annotatedHandlerBeanPostProcessor() {
    return new AnnotatedHandlerBeanPostProcessor();
  }

}
