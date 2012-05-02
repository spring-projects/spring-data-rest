package org.springframework.data.rest.repository.context;

import org.springframework.context.ApplicationEvent;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class BeforeSaveEvent extends RepositoryEvent {
  public BeforeSaveEvent(Object source) {
    super(source);
  }
}
