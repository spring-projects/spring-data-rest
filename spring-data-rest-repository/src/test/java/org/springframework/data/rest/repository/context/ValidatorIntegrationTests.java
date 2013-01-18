package org.springframework.data.rest.repository.context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.rest.repository.RepositoryConstraintViolationException;
import org.springframework.data.rest.repository.domain.jpa.Person;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests to check the {@link org.springframework.validation.Validator} integration.
 *
 * @author Jon Brisbin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ValidatorTestsConfig.class)
public class ValidatorIntegrationTests {

  @Autowired
  ApplicationContext appCtx;

  @Test(expected = RepositoryConstraintViolationException.class)
  public void shouldValidateLastName() throws Exception {
    appCtx.publishEvent(new BeforeSaveEvent(new Person()));
  }

}
