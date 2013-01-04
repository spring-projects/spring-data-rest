package org.springframework.data.rest.example;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.example.jpa.Person;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;

/**
 * @author Jon Brisbin
 */
@Configuration
@ImportResource("classpath:META-INF/spring/security-config.xml")
public class RestExporterExampleRestConfig extends RepositoryRestMvcConfiguration {

  @Override protected void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
    config.addResourceMappingForDomainType(Person.class)
          .addResourceMappingFor("lastName")
          .setPath("surname");
    config.addResourceMappingForDomainType(Person.class)
          .addResourceMappingFor("siblings")
          .setRel("siblings")
          .setPath("siblings");
  }

  //  @Bean public ResourceProcessor<Resource<?>> globalResourceProcessor() {
  //    return new ResourceProcessor<Resource<?>>() {
  //      @Override public Resource<?> process(Resource<?> resource) {
  //        resource.add(new Link("href", "rel"));
  //        return resource;
  //      }
  //    };
  //  }

}
