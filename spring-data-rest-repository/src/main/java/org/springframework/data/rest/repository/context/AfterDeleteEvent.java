package org.springframework.data.rest.repository.context;

/**
 * Emitted after the entity is delete from the repository.
 *
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class AfterDeleteEvent extends RepositoryEvent {
  public AfterDeleteEvent(Object source) {
    super(source);
  }
}
