package org.springframework.data.rest.repository.context;

import org.springframework.context.ApplicationEvent;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class BeforeSaveEvent extends RepositoryEvent {
  public BeforeSaveEvent(Object source) {
    super(source);
  }
}
