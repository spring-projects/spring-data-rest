/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.core.context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.data.rest.core.RepositoryTestsConfig;
import org.springframework.data.rest.core.domain.JpaRepositoryConfig;
import org.springframework.data.rest.core.domain.Person;
import org.springframework.data.rest.core.domain.PersonNameValidator;
import org.springframework.data.rest.core.event.BeforeSaveEvent;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests to check the {@link org.springframework.validation.Validator} integration.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ValidatorIntegrationTests {

	@Configuration
	@Import({ RepositoryTestsConfig.class, JpaRepositoryConfig.class })
	static class ValidatorTestsConfig {

		@Bean
		public ValidatingRepositoryEventListener validatingListener(ObjectFactory<PersistentEntities> persistentEntities) {

			ValidatingRepositoryEventListener listener = new ValidatingRepositoryEventListener(persistentEntities);
			listener.addValidator("beforeSave", new PersonNameValidator());

			return listener;
		}
	}

	@Autowired ConfigurableApplicationContext context;
	@Autowired KeyValueMappingContext mappingContext;

	@Test(expected = RepositoryConstraintViolationException.class)
	public void shouldValidateLastName() throws Exception {

		mappingContext.getPersistentEntity(Person.class);

		// Empty name should be rejected by PersonNameValidator
		context.publishEvent(new BeforeSaveEvent(new Person("Dave", "")));
	}
}
