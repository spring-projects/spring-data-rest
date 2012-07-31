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
 * Main Spring MVC configuration for the REST exporter. Can be subclassed and any of these methods overridden to
 * provide
 * custom configuration for your environment. More than likely, however, it won't be necessary to do this as most
 * user-configurable properties are defined on the {@link RepositoryRestConfiguration} bean, which you can define in
 * your own <code>ApplicationContext</code> (which can take the form of an XML file in the classpath at location
 * 'META-INF/spring-data-rest/' with a name that ends with '-export.xml').
 *
 * @author Jon Brisbin
 */
@Configuration
@ImportResource("classpath*:META-INF/spring-data-rest/**/*-export.xml")
public class RepositoryRestMvcConfiguration {

  /**
   * {@link org.springframework.data.rest.repository.RepositoryExporter} implementation for exporting JPA repositories.
   */
  @Autowired(required = false)
  JpaRepositoryExporter customJpaRepositoryExporter;

  /**
   * {@link org.springframework.context.ApplicationListener} implementation for invoking {@link
   * org.springframework.validation.Validator} instances assigned to specific domain types.
   */
  @Autowired(required = false)
  ValidatingRepositoryEventListener validatingRepositoryEventListener;

  /**
   * Main configuration for the REST exporter.
   */
  @Autowired(required = false)
  RepositoryRestConfiguration repositoryRestConfig = RepositoryRestConfiguration.DEFAULT;

  /**
   * For getting access to the {@link javax.persistence.EntityManagerFactory}.
   *
   * @return
   */
  @Bean public PersistenceAnnotationBeanPostProcessor persistenceAnnotationBeanPostProcessor() {
    return new PersistenceAnnotationBeanPostProcessor();
  }

  /**
   * Use the pre-defined {@link JpaRepositoryExporter} defined by the user or create a default one.
   *
   * @return
   */
  @Bean public JpaRepositoryExporter jpaRepositoryExporter() {
    if(null == customJpaRepositoryExporter) {
      return new JpaRepositoryExporter();
    } else {
      return customJpaRepositoryExporter;
    }
  }

  /**
   * Use the pre-defined {@link ValidatingRepositoryEventListener} defined by the user or create a default one.
   *
   * @return
   */
  @Bean public ValidatingRepositoryEventListener validatingRepositoryEventListener() {
    if(null == validatingRepositoryEventListener) {
      return new ValidatingRepositoryEventListener();
    } else {
      return validatingRepositoryEventListener;
    }
  }

  /**
   * The main REST exporter Spring MVC controller.
   *
   * @return
   *
   * @throws Exception
   */
  @Bean public RepositoryRestController repositoryRestController()
      throws Exception {
    return new RepositoryRestController();
  }

  /**
   * Special {@link org.springframework.web.servlet.HandlerAdapter} that only recognizes handler methods defined in the
   * {@link RepositoryRestController} class.
   *
   * @return
   */
  @Bean public RepositoryRestHandlerAdapter repositoryExporterHandlerAdapter() {
    return new RepositoryRestHandlerAdapter(repositoryRestConfig);
  }

  /**
   * Special {@link org.springframework.web.servlet.HandlerMapping} that only recognizes handler methods defined in the
   * {@link RepositoryRestController} class.
   *
   * @return
   */
  @Bean public RepositoryRestHandlerMapping repositoryExporterHandlerMapping() {
    return new RepositoryRestHandlerMapping();
  }

  /**
   * Bean for looking up methods annotated with {@link org.springframework.web.bind.annotation.ExceptionHandler}.
   *
   * @return
   */
  @Bean public ExceptionHandlerExceptionResolver exceptionHandlerExceptionResolver() {
    ExceptionHandlerExceptionResolver er = new ExceptionHandlerExceptionResolver();
    er.setCustomArgumentResolvers(
        Arrays.<HandlerMethodArgumentResolver>asList(new ServerHttpRequestMethodArgumentResolver())
    );
    return er;
  }

}
