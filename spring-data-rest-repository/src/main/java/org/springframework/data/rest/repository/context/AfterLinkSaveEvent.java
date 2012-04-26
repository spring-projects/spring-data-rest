package org.springframework.data.rest.repository.context;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class AfterLinkSaveEvent extends LinkSaveEvent {
  public AfterLinkSaveEvent(Object source, Object child) {
    super(source, child);
  }
}
