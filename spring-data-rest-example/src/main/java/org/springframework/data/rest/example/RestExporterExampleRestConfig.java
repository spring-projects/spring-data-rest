package org.springframework.data.rest.example;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.example.jpa.Person;
import org.springframework.data.rest.example.jpa.PersonValidator;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;

/**
 * @author Jon Brisbin
 */
@SuppressWarnings({"rawtypes"})
@Configuration
//@ImportResource("classpath:META-INF/spring/security-config.xml")
public class RestExporterExampleRestConfig extends RepositoryRestMvcConfiguration {

	@Bean
	public MessageSource messageSource() {
		ReloadableResourceBundleMessageSource msgsrc = new ReloadableResourceBundleMessageSource();
		msgsrc.setBasename("/WEB-INF/classes/ValidationMessages");
		msgsrc.setFallbackToSystemLocale(false);
		return msgsrc;
	}

	@Bean
	public PersonValidator beforeCreatePersonValidator() {
		return new PersonValidator();
	}

	@Override
	protected void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
		config.setResourceMappingForDomainType(Person.class)
					.addResourceMappingFor("lastName")
					.setPath("surname");
	}

	@Bean
	public ResourceProcessor<Resource<Person>> personResourceProcessor() {
		return new ResourceProcessor<Resource<Person>>() {
			RepositoryRestConfiguration config = config();

			@Override
			public Resource<Person> process(Resource<Person> resource) {
				System.out.println("processing " + resource);
				System.out.println("url: " + config.getBaseUri().toString());
				resource.add(new Link("http://host:port/path", "myrel"));
				return resource;
			}
		};
	}

	@Bean
	public ResourceProcessor<RepositoryLinksResource> rootLinksResourceProcessor() {
		return new ResourceProcessor<RepositoryLinksResource>() {
			@Override
			public RepositoryLinksResource process(RepositoryLinksResource resource) {
				resource.add(new Link("href", "rel"));
				return resource;
			}
		};
	}

}
