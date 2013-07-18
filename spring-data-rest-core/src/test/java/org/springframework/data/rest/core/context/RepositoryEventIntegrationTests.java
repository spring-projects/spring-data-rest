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
		public AnnotatedHandlerBeanPostProcessor annotatedHandlerBeanPostProcessor() {
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

	@Test(expected = RuntimeException.class)
	public void shouldDispatchBeforeCreate() throws Exception {
		appCtx.publishEvent(new BeforeCreateEvent(person));
	}

	@Test(expected = RuntimeException.class)
	public void shouldDispatchAfterCreate() throws Exception {
		appCtx.publishEvent(new AfterCreateEvent(person));
	}

	@Test(expected = RuntimeException.class)
	public void shouldDispatchBeforeSave() throws Exception {
		appCtx.publishEvent(new BeforeSaveEvent(person));
	}

	@Test(expected = RuntimeException.class)
	public void shouldDispatchAfterSave() throws Exception {
		appCtx.publishEvent(new AfterSaveEvent(person));
	}

	@Test(expected = RuntimeException.class)
	public void shouldDispatchBeforeDelete() throws Exception {
		appCtx.publishEvent(new BeforeDeleteEvent(person));
	}

	@Test(expected = RuntimeException.class)
	public void shouldDispatchAfterDelete() throws Exception {
		appCtx.publishEvent(new AfterDeleteEvent(person));
	}

	@Test(expected = RuntimeException.class)
	public void shouldDispatchBeforeLinkSave() throws Exception {
		appCtx.publishEvent(new BeforeLinkSaveEvent(person, new Object()));
	}

	@Test(expected = RuntimeException.class)
	public void shouldDispatchAfterLinkSave() throws Exception {
		appCtx.publishEvent(new AfterLinkSaveEvent(person, new Object()));
	}

	@Test(expected = RuntimeException.class)
	public void shouldDispatchBeforeLinkDelete() throws Exception {
		appCtx.publishEvent(new BeforeLinkDeleteEvent(person, new Object()));
	}

	@Test(expected = RuntimeException.class)
	public void shouldDispatchAfterLinkDelete() throws Exception {
		appCtx.publishEvent(new AfterLinkDeleteEvent(person, new Object()));
	}

}
