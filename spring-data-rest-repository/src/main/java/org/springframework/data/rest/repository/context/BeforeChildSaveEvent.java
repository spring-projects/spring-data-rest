package org.springframework.data.rest.repository.context;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class BeforeChildSaveEvent extends ChildSaveEvent {
  public BeforeChildSaveEvent(Object source, Object child) {
    super(source, child);
  }
}
