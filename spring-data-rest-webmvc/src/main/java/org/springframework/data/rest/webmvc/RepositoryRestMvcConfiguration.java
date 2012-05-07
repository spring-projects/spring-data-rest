package org.springframework.data.rest.webmvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManagerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.rest.repository.RepositoryExporter;
import org.springframework.data.rest.repository.context.ValidatingRepositoryEventListener;
import org.springframework.data.rest.repository.jpa.JpaRepositoryExporter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor;
import org.springframework.util.Assert;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
@Configuration
@ImportResource("classpath*:META-INF/spring-data-rest/**/*-export.xml")
public class RepositoryRestMvcConfiguration {

  ContentNegotiatingViewResolver viewResolver;
  RepositoryRestController repositoryRestController;

  @Autowired
  EntityManagerFactory entityManagerFactory;

  @Autowired(required = false)
  JpaRepositoryExporter customJpaRepositoryExporter;

  @Autowired(required = false)
  ConversionService customConversionService;

  @Autowired(required = false)
  List<HttpMessageConverter<?>> httpMessageConverters = new ArrayList<HttpMessageConverter<?>>();

  @Autowired(required = false)
  ValidatingRepositoryEventListener validatingRepositoryEventListener;

  @Bean List<HttpMessageConverter<?>> httpMessageConverters() {
    Assert.notNull(httpMessageConverters);

    if (httpMessageConverters.isEmpty()) {
      MappingJacksonHttpMessageConverter json = new MappingJacksonHttpMessageConverter();
      json.setSupportedMediaTypes(
          Arrays.asList(MediaType.APPLICATION_JSON, MediaType.valueOf("application/x-spring-data+json"))
      );
      httpMessageConverters.add(json);
    }

    return httpMessageConverters;
  }

  @Bean PersistenceAnnotationBeanPostProcessor persistenceAnnotationBeanPostProcessor() {
    return new PersistenceAnnotationBeanPostProcessor();
  }

  @Bean OpenEntityManagerInViewInterceptor openEntityManagerInViewInterceptor() {
    OpenEntityManagerInViewInterceptor oemiv = new OpenEntityManagerInViewInterceptor();
    oemiv.setEntityManagerFactory(entityManagerFactory);
    return oemiv;
  }

  @Bean JpaRepositoryExporter jpaRepositoryExporter() {
    if (null == customJpaRepositoryExporter) {
      return new JpaRepositoryExporter();
    } else {
      return customJpaRepositoryExporter;
    }
  }

  @Bean ValidatingRepositoryEventListener validatingRepositoryEventListener() {
    if (null == validatingRepositoryEventListener) {
      return new ValidatingRepositoryEventListener();
    } else {
      return validatingRepositoryEventListener;
    }
  }

  @Bean ContentNegotiatingViewResolver contentNegotiatingViewResolver() {
    if (null == viewResolver) {
      viewResolver = new ContentNegotiatingViewResolver();
      Map<String, String> jsonTypes = new HashMap<String, String>() {{
        put("json", "application/json");
        put("urilist", "text/uri-list");
      }};

      viewResolver.setMediaTypes(jsonTypes);
      viewResolver.setDefaultViews(
          Arrays.asList((View) new JsonView("application/json"),
                        (View) new UriListView())
      );
    }
    return viewResolver;
  }

  @Bean RepositoryRestController repositoryRestController() throws Exception {
    if (null == repositoryRestController) {
      this.repositoryRestController = new RepositoryRestController()
          .repositoryExporters(Arrays.<RepositoryExporter>asList(jpaRepositoryExporter()))
          .httpMessageConverters(httpMessageConverters())
          .jsonMediaType("application/json");
      if (null != customConversionService) {
        repositoryRestController.conversionService(customConversionService);
      }
    }
    return repositoryRestController;
  }

  @Bean RequestMappingHandlerMapping handlerMapping() {
    return new RequestMappingHandlerMapping();
  }

  @Bean RequestMappingHandlerAdapter handlerAdapter() {
    RequestMappingHandlerAdapter handlerAdapter = new RequestMappingHandlerAdapter();
    handlerAdapter.setCustomArgumentResolvers(
        Arrays.asList((HandlerMethodArgumentResolver) new ServerHttpRequestMethodArgumentResolver())
    );
    return handlerAdapter;
  }

  @Bean ExceptionHandlerExceptionResolver exceptionHandlerExceptionResolver() {
    return new ExceptionHandlerExceptionResolver();
  }

  @Bean DefaultHandlerExceptionResolver handlerExceptionResolver() {
    return new DefaultHandlerExceptionResolver();
  }

  @Bean ResponseStatusExceptionResolver responseStatusExceptionResolver() {
    return new ResponseStatusExceptionResolver();
  }

}
