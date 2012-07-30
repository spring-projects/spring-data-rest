package org.springframework.data.rest.repository.context;

/**
 * @author Jon Brisbin
 */
public class BeforeLinkDeleteEvent extends LinkSaveEvent{
  public BeforeLinkDeleteEvent(Object source, Object linked) {
    super(source, linked);
  }
}
