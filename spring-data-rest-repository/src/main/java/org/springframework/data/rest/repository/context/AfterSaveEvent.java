package org.springframework.data.rest.repository.context;

import org.springframework.context.ApplicationEvent;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class AfterSaveEvent extends RepositoryEvent {
  public AfterSaveEvent(Object source) {
    super(source);
  }
}
