package org.springframework.data.rest.repository;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.config.ResourceMapping;
import org.springframework.data.rest.repository.domain.jpa.ConfiguredPersonRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests to check that {@link ResourceMapping}s are handled correctly.
 *
 * @author Jon Brisbin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RepositoryTestsConfig.class)
public class RepositoryRestConfigurationIntegrationTests {

  @Autowired
  RepositoryRestConfiguration config;

  @Test
  public void shouldProvideResourceMappingForConfiguredRepository() throws Exception {
    ResourceMapping mapping = config.getResourceMappingForRepository(ConfiguredPersonRepository.class);

    assertThat(mapping, notNullValue());
    assertThat(mapping.getRel(), is("people"));
    assertThat(mapping.getPath(), is("people"));
    assertThat(mapping.isExported(), is(false));
  }

}
