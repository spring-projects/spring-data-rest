package org.springframework.data.rest.repository.context;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class AfterChildSaveEvent extends ChildSaveEvent {
  public AfterChildSaveEvent(Object source, Object child) {
    super(source, child);
  }
}
