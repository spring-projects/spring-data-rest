package org.springframework.data.rest.repository.context;

import org.springframework.context.ApplicationEvent;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class AfterSaveEvent extends RepositoryEvent {
  public AfterSaveEvent(Object source) {
    super(source);
  }
}
