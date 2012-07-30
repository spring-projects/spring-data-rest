package org.springframework.data.rest.webmvc;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.rest.repository.context.ValidatingRepositoryEventListener;
import org.springframework.data.rest.repository.jpa.JpaRepositoryExporter;
import org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
@Configuration
@ImportResource("classpath*:META-INF/spring-data-rest/**/*-export.xml")
public class RepositoryRestMvcConfiguration {

  @Autowired(required = false)
  JpaRepositoryExporter customJpaRepositoryExporter;

  @Autowired(required = false)
  ValidatingRepositoryEventListener validatingRepositoryEventListener;

  @Autowired(required = false)
  RepositoryRestConfiguration repositoryRestConfig = RepositoryRestConfiguration.DEFAULT;

  @Bean public PersistenceAnnotationBeanPostProcessor persistenceAnnotationBeanPostProcessor() {
    return new PersistenceAnnotationBeanPostProcessor();
  }

  @Bean public JpaRepositoryExporter jpaRepositoryExporter() {
    if(null == customJpaRepositoryExporter) {
      return new JpaRepositoryExporter();
    } else {
      return customJpaRepositoryExporter;
    }
  }

  @Bean public ValidatingRepositoryEventListener validatingRepositoryEventListener() {
    if(null == validatingRepositoryEventListener) {
      return new ValidatingRepositoryEventListener();
    } else {
      return validatingRepositoryEventListener;
    }
  }

  @Bean public RepositoryRestController repositoryRestController()
      throws Exception {
    return new RepositoryRestController();
  }

  @Bean public RepositoryRestHandlerAdapter repositoryExporterHandlerAdapter() {
    return new RepositoryRestHandlerAdapter(repositoryRestConfig);
  }

  @Bean public RepositoryRestHandlerMapping repositoryExporterHandlerMapping() {
    return new RepositoryRestHandlerMapping();
  }

  @Bean public ExceptionHandlerExceptionResolver exceptionHandlerExceptionResolver() {
    ExceptionHandlerExceptionResolver er = new ExceptionHandlerExceptionResolver();
    er.setCustomArgumentResolvers(
        Arrays.<HandlerMethodArgumentResolver>asList(new ServerHttpRequestMethodArgumentResolver())
    );
    return er;
  }

}
