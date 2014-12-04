/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.core.context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.core.RepositoryTestsConfig;
import org.springframework.data.rest.core.domain.jpa.AnnotatedPersonEventHandler;
import org.springframework.data.rest.core.domain.jpa.EventHandlerInvokedException;
import org.springframework.data.rest.core.domain.jpa.Person;
import org.springframework.data.rest.core.domain.jpa.PersonBeforeSaveHandler;
import org.springframework.data.rest.core.domain.jpa.PersonRepository;
import org.springframework.data.rest.core.event.AfterCreateEvent;
import org.springframework.data.rest.core.event.AfterDeleteEvent;
import org.springframework.data.rest.core.event.AfterLinkDeleteEvent;
import org.springframework.data.rest.core.event.AfterLinkSaveEvent;
import org.springframework.data.rest.core.event.AfterSaveEvent;
import org.springframework.data.rest.core.event.AnnotatedHandlerBeanPostProcessor;
import org.springframework.data.rest.core.event.BeforeCreateEvent;
import org.springframework.data.rest.core.event.BeforeDeleteEvent;
import org.springframework.data.rest.core.event.BeforeLinkDeleteEvent;
import org.springframework.data.rest.core.event.BeforeLinkSaveEvent;
import org.springframework.data.rest.core.event.BeforeSaveEvent;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests around the {@link org.springframework.context.ApplicationEvent} handling abstractions.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
public class RepositoryEventIntegrationTests {

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
		public static AnnotatedHandlerBeanPostProcessor annotatedHandlerBeanPostProcessor() {
			return new AnnotatedHandlerBeanPostProcessor();
		}
	}

	@Autowired ApplicationContext appCtx;
	@Autowired PersonRepository people;
	Person person;

	@Before
	public void setup() {
		person = people.save(new Person("Jane", "Doe"));
	}

	/**
	 * @see DATAREST-388
	 */
	@Test(expected = EventHandlerInvokedException.class)
	public void shouldDispatchBeforeCreate() throws Exception {
		appCtx.publishEvent(new BeforeCreateEvent(person));
	}

	/**
	 * @see DATAREST-388
	 */
	@Test(expected = EventHandlerInvokedException.class)
	public void shouldDispatchAfterCreate() throws Exception {
		appCtx.publishEvent(new AfterCreateEvent(person));
	}

	/**
	 * @see DATAREST-388
	 */
	@Test(expected = EventHandlerInvokedException.class)
	public void shouldDispatchBeforeSave() throws Exception {
		appCtx.publishEvent(new BeforeSaveEvent(person));
	}

	/**
	 * @see DATAREST-388
	 */
	@Test(expected = EventHandlerInvokedException.class)
	public void shouldDispatchAfterSave() throws Exception {
		appCtx.publishEvent(new AfterSaveEvent(person));
	}

	/**
	 * @see DATAREST-388
	 */
	@Test(expected = EventHandlerInvokedException.class)
	public void shouldDispatchBeforeDelete() throws Exception {
		appCtx.publishEvent(new BeforeDeleteEvent(person));
	}

	/**
	 * @see DATAREST-388
	 */
	@Test(expected = EventHandlerInvokedException.class)
	public void shouldDispatchAfterDelete() throws Exception {
		appCtx.publishEvent(new AfterDeleteEvent(person));
	}

	/**
	 * @see DATAREST-388
	 */
	@Test(expected = EventHandlerInvokedException.class)
	public void shouldDispatchBeforeLinkSave() throws Exception {
		appCtx.publishEvent(new BeforeLinkSaveEvent(person, new Object()));
	}

	/**
	 * @see DATAREST-388
	 */
	@Test(expected = EventHandlerInvokedException.class)
	public void shouldDispatchAfterLinkSave() throws Exception {
		appCtx.publishEvent(new AfterLinkSaveEvent(person, new Object()));
	}

	/**
	 * @see DATAREST-388
	 */
	@Test(expected = EventHandlerInvokedException.class)
	public void shouldDispatchBeforeLinkDelete() throws Exception {
		appCtx.publishEvent(new BeforeLinkDeleteEvent(person, new Object()));
	}

	/**
	 * @see DATAREST-388
	 */
	@Test(expected = EventHandlerInvokedException.class)
	public void shouldDispatchAfterLinkDelete() throws Exception {
		appCtx.publishEvent(new AfterLinkDeleteEvent(person, new Object()));
	}
}
