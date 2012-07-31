package org.springframework.data.rest.repository.context;

/**
 * Emitted after the entity is deleted from the repository.
 *
 * @author Jon Brisbin
 */
public class AfterDeleteEvent
    extends RepositoryEvent {
  public AfterDeleteEvent(Object source) {
    super(source);
  }
}
