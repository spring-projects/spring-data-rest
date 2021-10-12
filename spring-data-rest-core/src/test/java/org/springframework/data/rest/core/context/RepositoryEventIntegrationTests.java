/*
 * Copyright 2014-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.core.RepositoryTestsConfig;
import org.springframework.data.rest.core.domain.AnnotatedPersonEventHandler;
import org.springframework.data.rest.core.domain.EventHandlerInvokedException;
import org.springframework.data.rest.core.domain.Person;
import org.springframework.data.rest.core.domain.PersonBeforeSaveHandler;
import org.springframework.data.rest.core.domain.PersonRepository;
import org.springframework.data.rest.core.event.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Tests around the {@link org.springframework.context.ApplicationEvent} handling abstractions.
 *
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
class RepositoryEventIntegrationTests {

	@Configuration
	@Import({ RepositoryTestsConfig.class })
	static class RepositoryEventTestsConfig {

		@Bean
		public PersonBeforeSaveHandler personBeforeSaveHandler() {
			return new PersonBeforeSaveHandler();
		}

		@Bean
		public AnnotatedPersonEventHandler beforeSaveHandler() {
			return new AnnotatedPersonEventHandler();
		}

		@Bean
		public static AnnotatedEventHandlerInvoker annotatedEventHandlerInvoker() {
			return new AnnotatedEventHandlerInvoker();
		}
	}

	@Autowired ApplicationContext appCtx;
	@Autowired PersonRepository people;
	Person person;

	@BeforeEach
	void setup() {
		person = people.save(new Person("Jane", "Doe"));
	}

	@Test // DATAREST-388
	void shouldDispatchBeforeCreate() throws Exception {

		assertThatExceptionOfType(EventHandlerInvokedException.class) //
				.isThrownBy(() -> appCtx.publishEvent(new BeforeCreateEvent(person)));
	}

	@Test // DATAREST-388
	void shouldDispatchAfterCreate() throws Exception {

		assertThatExceptionOfType(EventHandlerInvokedException.class) //
				.isThrownBy(() -> appCtx.publishEvent(new AfterCreateEvent(person)));
	}

	@Test // DATAREST-388
	void shouldDispatchBeforeSave() throws Exception {

		assertThatExceptionOfType(EventHandlerInvokedException.class) //
				.isThrownBy(() -> appCtx.publishEvent(new BeforeSaveEvent(person)));
	}

	@Test // DATAREST-388
	void shouldDispatchAfterSave() throws Exception {

		assertThatExceptionOfType(EventHandlerInvokedException.class) //
				.isThrownBy(() -> appCtx.publishEvent(new AfterSaveEvent(person)));
	}

	@Test // DATAREST-388
	void shouldDispatchBeforeDelete() throws Exception {

		assertThatExceptionOfType(EventHandlerInvokedException.class) //
				.isThrownBy(() -> appCtx.publishEvent(new BeforeDeleteEvent(person)));
	}

	@Test // DATAREST-388
	void shouldDispatchAfterDelete() throws Exception {

		assertThatExceptionOfType(EventHandlerInvokedException.class) //
				.isThrownBy(() -> appCtx.publishEvent(new AfterDeleteEvent(person)));
	}

	@Test // DATAREST-388
	void shouldDispatchBeforeLinkSave() throws Exception {

		assertThatExceptionOfType(EventHandlerInvokedException.class) //
				.isThrownBy(() -> appCtx.publishEvent(new BeforeLinkSaveEvent(person, new Object())));
	}

	@Test // DATAREST-388
	void shouldDispatchAfterLinkSave() throws Exception {

		assertThatExceptionOfType(EventHandlerInvokedException.class) //
				.isThrownBy(() -> appCtx.publishEvent(new AfterLinkSaveEvent(person, new Object())));
	}

	@Test // DATAREST-388
	void shouldDispatchBeforeLinkDelete() throws Exception {

		assertThatExceptionOfType(EventHandlerInvokedException.class) //
				.isThrownBy(() -> appCtx.publishEvent(new BeforeLinkDeleteEvent(person, new Object())));
	}

	@Test // DATAREST-388
	void shouldDispatchAfterLinkDelete() throws Exception {

		assertThatExceptionOfType(EventHandlerInvokedException.class) //
				.isThrownBy(() -> appCtx.publishEvent(new AfterLinkDeleteEvent(person, new Object())));
	}
}
