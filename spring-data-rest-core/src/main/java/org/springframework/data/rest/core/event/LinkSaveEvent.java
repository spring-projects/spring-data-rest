package org.springframework.data.rest.core.event;

/**
 * Base class for {@link RepositoryEvent}s that deal with saving/updating or deleting a linked object.
 * 
 * @author Jon Brisbin
 */
public abstract class LinkSaveEvent extends RepositoryEvent {

	private static final long serialVersionUID = -9071648572128698903L;
	private final Object linked;

	public LinkSaveEvent(Object source, Object linked) {
		super(source);
		this.linked = linked;
	}

	/**
	 * Get the linked object.
	 * 
	 * @return The entity representing the right-hand side of this relationship.
	 */
	public Object getLinked() {
		return linked;
	}

}
