package org.springframework.data.rest.repository.context;

/**
 * Emitted before an entity is deleted from the repository.
 *
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class BeforeDeleteEvent
    extends RepositoryEvent {
  public BeforeDeleteEvent(Object source) {
    super(source);
  }
}
