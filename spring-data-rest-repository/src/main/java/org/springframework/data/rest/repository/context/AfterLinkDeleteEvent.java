package org.springframework.data.rest.repository.context;

/**
 * Emitted after a link to a related object is deleted from the parent.
 *
 * @author Jon Brisbin
 */
public class AfterLinkDeleteEvent extends LinkSaveEvent {
  public AfterLinkDeleteEvent(Object source, Object linked) {
    super(source, linked);
  }
}
