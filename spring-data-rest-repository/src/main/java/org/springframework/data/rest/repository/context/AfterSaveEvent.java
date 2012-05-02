package org.springframework.data.rest.repository.context;

/**
 * Emitted immediately after a save to the repository.
 *
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class AfterSaveEvent extends RepositoryEvent {
  public AfterSaveEvent(Object source) {
    super(source);
  }
}
