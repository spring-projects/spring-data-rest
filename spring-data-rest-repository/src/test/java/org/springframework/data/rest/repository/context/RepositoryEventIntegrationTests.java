package org.springframework.data.rest.repository.context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.rest.repository.domain.jpa.Person;
import org.springframework.data.rest.repository.domain.jpa.PersonRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests around the {@link org.springframework.context.ApplicationEvent} handling abstractions.
 *
 * @author Jon Brisbin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RepositoryEventTestsConfig.class)
public class RepositoryEventIntegrationTests {

  @Autowired
  ApplicationContext appCtx;
  @Autowired
  PersonRepository   people;
  Person person;

  @Before
  public void setup() {
    person = people.save(new Person("Jane", "Doe"));
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
