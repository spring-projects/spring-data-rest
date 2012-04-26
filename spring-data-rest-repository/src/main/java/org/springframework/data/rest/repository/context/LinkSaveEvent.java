package org.springframework.data.rest.repository.context;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class LinkSaveEvent extends RepositoryEvent {

  private final Object linked;

  public LinkSaveEvent(Object source, Object linked) {
    super(source);
    this.linked = linked;
  }

  public Object getLinked() {
    return linked;
  }

}
