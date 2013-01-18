package org.springframework.data.rest.repository.domain.jpa;

import org.springframework.data.rest.repository.annotation.HandleAfterDelete;
import org.springframework.data.rest.repository.annotation.HandleAfterLinkDelete;
import org.springframework.data.rest.repository.annotation.HandleAfterLinkSave;
import org.springframework.data.rest.repository.annotation.HandleAfterSave;
import org.springframework.data.rest.repository.annotation.HandleBeforeDelete;
import org.springframework.data.rest.repository.annotation.HandleBeforeLinkDelete;
import org.springframework.data.rest.repository.annotation.HandleBeforeLinkSave;
import org.springframework.data.rest.repository.annotation.HandleBeforeSave;
import org.springframework.data.rest.repository.annotation.RepositoryEventHandler;

/**
 * @author Jon Brisbin
 */
@RepositoryEventHandler(Person.class)
public class AnnotatedPersonEventHandler {
  @HandleAfterDelete
  @HandleAfterSave
  public void handleAfter(Person p) {
    throw new RuntimeException();
  }

  @HandleAfterLinkDelete
  @HandleAfterLinkSave
  public void handleAfterLink(Person p, Object o) {
    throw new RuntimeException();
  }

  @HandleBeforeDelete
  @HandleBeforeSave
  public void handleBefore(Person p) {
    throw new RuntimeException();
  }

  @HandleBeforeLinkDelete
  @HandleBeforeLinkSave
  public void handleBeforeLink(Person p, Object o) {
    throw new RuntimeException();
  }
}
