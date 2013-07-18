package org.springframework.data.rest.core.context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.data.rest.core.RepositoryTestsConfig;
import org.springframework.data.rest.core.domain.jpa.Person;
import org.springframework.data.rest.core.event.BeforeSaveEvent;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests to check the {@link org.springframework.validation.Validator} integration.
 * 
 * @author Jon Brisbin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
public class ValidatorIntegrationTests {

	@Configuration
	@Import({ RepositoryTestsConfig.class })
	static class ValidatorTestsConfig {

		@Bean
		public ValidatingRepositoryEventListener validatingListener() {
			return new ValidatingRepositoryEventListener();
		}
	}

	@Autowired ApplicationContext appCtx;

	@Test(expected = RepositoryConstraintViolationException.class)
	public void shouldValidateLastName() throws Exception {
		appCtx.publishEvent(new BeforeSaveEvent(new Person()));
	}

}
