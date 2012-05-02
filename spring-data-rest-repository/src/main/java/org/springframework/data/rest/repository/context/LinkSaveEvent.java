package org.springframework.data.rest.repository.context;

/**
 * Base class for {@link RepositoryEvent}s that deal with saving a linked object.
 *
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public abstract class LinkSaveEvent extends RepositoryEvent {

  private final Object linked;

  public LinkSaveEvent(Object source, Object linked) {
    super(source);
    this.linked = linked;
  }

  /**
   * Get the linked object.
   *
   * @return
   */
  public Object getLinked() {
    return linked;
  }

}
