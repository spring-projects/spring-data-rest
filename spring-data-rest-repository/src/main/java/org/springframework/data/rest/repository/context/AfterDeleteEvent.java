package org.springframework.data.rest.repository.context;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class AfterDeleteEvent extends RepositoryEvent {
  public AfterDeleteEvent(Object source) {
    super(source);
  }
}
