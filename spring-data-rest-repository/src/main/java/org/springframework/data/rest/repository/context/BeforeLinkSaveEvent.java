package org.springframework.data.rest.repository.context;

/**
 * Emitted before a linked object is saved to the repository.
 *
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class BeforeLinkSaveEvent
    extends LinkSaveEvent {
  public BeforeLinkSaveEvent(Object source, Object linked) {
    super(source, linked);
  }
}
