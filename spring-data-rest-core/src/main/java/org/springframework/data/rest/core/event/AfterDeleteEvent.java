package org.springframework.data.rest.core.event;

/**
 * Emitted after the entity is deleted from the repository.
 * 
 * @author Jon Brisbin
 */
public class AfterDeleteEvent extends RepositoryEvent {

	private static final long serialVersionUID = -6090615345948638970L;

	public AfterDeleteEvent(Object source) {
		super(source);
	}
}
