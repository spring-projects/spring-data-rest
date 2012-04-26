package org.springframework.data.rest.repository.context;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class ChildSaveEvent extends RepositoryEvent{

  private final Object child;

  public ChildSaveEvent(Object source, Object child) {
    super(source);
    this.child = child;
  }

  public Object getChild() {
    return child;
  }

}
