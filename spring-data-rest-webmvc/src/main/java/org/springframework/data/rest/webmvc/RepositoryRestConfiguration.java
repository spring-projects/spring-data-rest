package org.springframework.data.rest.webmvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManagerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.rest.repository.context.ValidatingRepositoryEventListener;
import org.springframework.data.rest.repository.jpa.JpaRepositoryExporter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor;

/**
 * Base configuration for the Spring Data REST Exporter.
 *
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
@Configuration
public class RepositoryRestConfiguration {

  @Autowired
  EntityManagerFactory entityManagerFactory;
  @Autowired(required = false)
  JpaRepositoryExporter jpaRepositoryExporter;
  @Autowired(required = false)
  ConversionService customConversionService;
  ConfigurableConversionService defaultConversionService = new DefaultConversionService();
  @Autowired(required = false)
  List<HttpMessageConverter<?>> httpMessageConverters = new ArrayList<HttpMessageConverter<?>>();
  @Autowired(required = false)
  ValidatingRepositoryEventListener validatingRepositoryEventListener;

  /**
   * Either the user's pre-configured {@link ConversionService} or the {@link DefaultConversionService}.
   *
   * @return
   */
  @Bean ConversionService conversionService() {
    if (null != customConversionService) {
      return customConversionService;
    } else {
      return defaultConversionService;
    }
  }

  /**
   * A list of {@link HttpMessageConverter}s to be used to read incoming data and to write outgoing responses.
   *
   * @return
   */
  @Bean List<HttpMessageConverter<?>> httpMessageConverters() {
    if (httpMessageConverters.isEmpty()) {
      MappingJacksonHttpMessageConverter json = new MappingJacksonHttpMessageConverter();
      json.setSupportedMediaTypes(
          Arrays.asList(MediaType.APPLICATION_JSON, MediaType.valueOf("application/x-spring-data+json"))
      );
      httpMessageConverters.add(json);
    }
    return httpMessageConverters;
  }

  /**
   * Export any JPA {@link org.springframework.data.repository.Repository} implementations we find.
   *
   * @return
   */
  @Bean JpaRepositoryExporter jpaRepositoryExporter() {
    if (null == jpaRepositoryExporter) {
      jpaRepositoryExporter = new JpaRepositoryExporter();
    }
    return jpaRepositoryExporter;
  }

  @Bean PersistenceAnnotationBeanPostProcessor persistenceAnnotationBeanPostProcessor() {
    return new PersistenceAnnotationBeanPostProcessor();
  }

}
