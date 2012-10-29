package org.springframework.data.rest.test.webmvc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.rest.repository.annotation.HandleAfterSave;
import org.springframework.data.rest.repository.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

/**
 * @author Jon Brisbin
 */
@Component
@RepositoryEventHandler(Person.class)
public class AfterSavePersonHandler {

  private final static Logger LOG = LoggerFactory.getLogger(AfterSavePersonHandler.class);

  @HandleAfterSave
  public void handleAfterSave(Person person) {
    LOG.info("saved person: " + person);
  }

}
