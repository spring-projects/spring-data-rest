package org.springframework.data.rest.repository.context;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class AfterDeleteEvent extends RepositoryEvent {
  public AfterDeleteEvent(Object source) {
    super(source);
  }
}
