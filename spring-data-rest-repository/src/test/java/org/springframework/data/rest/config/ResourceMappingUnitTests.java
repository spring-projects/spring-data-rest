package org.springframework.data.rest.config;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import org.junit.Test;
import org.springframework.data.rest.repository.domain.jpa.AnnotatedPersonRepository;
import org.springframework.data.rest.repository.domain.jpa.PlainPersonRepository;
import org.springframework.data.rest.repository.mapping.ResourceMapping;
import org.springframework.data.rest.repository.mapping.ResourceMappingFactory;
import org.springframework.data.rest.repository.support.SimpleRelProvider;

/**
 * Ensure the {@link ResourceMapping} components convey the correct information.
 *
 * @author Jon Brisbin
 */
public class ResourceMappingUnitTests {
	
	ResourceMappingFactory factory = new ResourceMappingFactory(new SimpleRelProvider());

  @Test
  public void shouldDetectDefaultRelAndPath() throws Exception {
  	
  	ResourceMapping newMapping = factory.getMappingForType(PlainPersonRepository.class);
  	
  	assertThat(newMapping.getRel(), is("person"));
    assertThat(newMapping.getPath(), is("person"));
    assertThat(newMapping.isExported(), is(true));
  }

  @Test
  public void shouldDetectAnnotatedRelAndPath() throws Exception {
  	
  	ResourceMapping newMapping = factory.getMappingForType(AnnotatedPersonRepository.class);
  	
  	assertThat(newMapping.getRel(), is("people"));
  	assertThat(newMapping.getPath(), is("person"));
  	assertThat(newMapping.isExported(), is(false));
  }
}
