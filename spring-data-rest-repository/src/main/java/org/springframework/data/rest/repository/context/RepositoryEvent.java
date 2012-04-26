package org.springframework.data.rest.repository.context;

import org.springframework.context.ApplicationEvent;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public abstract class RepositoryEvent extends ApplicationEvent {
  protected RepositoryEvent(Object source) {
    super(source);
  }
}
