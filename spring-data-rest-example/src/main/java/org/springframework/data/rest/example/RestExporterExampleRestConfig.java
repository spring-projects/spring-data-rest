package org.springframework.data.rest.example;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.example.jpa.Person;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;

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

	@Bean public ResourceProcessor<Resource<Person>> personResourceProcessor() {
		return new ResourceProcessor<Resource<Person>>() {
			RepositoryRestConfiguration config = config();

			@Override public Resource<Person> process(Resource<Person> resource) {
				System.out.println("processing " + resource);
				System.out.println("url: " + config.getBaseUri().toString());
				resource.add(new Link("http://host:port/path", "myrel"));
				return resource;
			}
		};
	}

}
