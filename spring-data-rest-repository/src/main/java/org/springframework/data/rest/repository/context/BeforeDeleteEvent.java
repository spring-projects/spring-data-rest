package org.springframework.data.rest.repository.context;

import org.springframework.context.ApplicationEvent;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class BeforeDeleteEvent extends RepositoryEvent {
  public BeforeDeleteEvent(Object source) {
    super(source);
  }
}
