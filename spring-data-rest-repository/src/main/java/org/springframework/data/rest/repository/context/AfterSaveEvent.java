package org.springframework.data.rest.repository.context;

/**
 * Emitted after a save to the repository.
 *
 * @author Jon Brisbin
 */
public class AfterSaveEvent extends RepositoryEvent {
  public AfterSaveEvent(Object source) {
    super(source);
  }
}
