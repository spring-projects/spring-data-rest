package org.springframework.data.rest.repository.context;

import org.springframework.context.ApplicationEvent;

/**
 * Abstract base class for events emitted by the REST exporter.
 *
 * @author Jon Brisbin
 */
public abstract class RepositoryEvent extends ApplicationEvent {
  protected RepositoryEvent(Object source) {
    super(source);
  }
}
