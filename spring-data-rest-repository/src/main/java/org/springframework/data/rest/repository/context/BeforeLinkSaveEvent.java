package org.springframework.data.rest.repository.context;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class BeforeLinkSaveEvent extends LinkSaveEvent {
  public BeforeLinkSaveEvent(Object source, Object linked) {
    super(source, linked);
  }
}
