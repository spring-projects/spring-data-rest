package org.springframework.data.rest.repository.context;

/**
 * @author Jon Brisbin
 */
public class AfterLinkDeleteEvent extends LinkSaveEvent {
  public AfterLinkDeleteEvent(Object source, Object linked) {
    super(source, linked);
  }
}
