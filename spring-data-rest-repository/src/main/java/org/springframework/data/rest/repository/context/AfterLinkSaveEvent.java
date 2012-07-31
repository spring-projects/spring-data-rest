package org.springframework.data.rest.repository.context;

/**
 * Emitted after saving a linked object to its parent in the repository.
 *
 * @author Jon Brisbin
 */
public class AfterLinkSaveEvent
    extends LinkSaveEvent {
  public AfterLinkSaveEvent(Object source, Object child) {
    super(source, child);
  }
}
