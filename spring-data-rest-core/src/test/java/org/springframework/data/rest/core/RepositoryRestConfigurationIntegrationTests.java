package org.springframework.data.rest.core;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.config.ResourceMapping;
import org.springframework.data.rest.core.domain.jpa.ConfiguredPersonRepository;

/**
 * Tests to check that {@link ResourceMapping}s are handled correctly.
 * 
 * @author Jon Brisbin
 */
@SuppressWarnings("deprecation")
public class RepositoryRestConfigurationIntegrationTests extends AbstractIntegrationTests {

	@Autowired RepositoryRestConfiguration config;

	@Test
	public void shouldProvideResourceMappingForConfiguredRepository() throws Exception {
		ResourceMapping mapping = config.getResourceMappingForRepository(ConfiguredPersonRepository.class);

		assertThat(mapping, notNullValue());
		assertThat(mapping.getRel(), is("people"));
		assertThat(mapping.getPath(), is("people"));
		assertThat(mapping.isExported(), is(false));
	}

}
