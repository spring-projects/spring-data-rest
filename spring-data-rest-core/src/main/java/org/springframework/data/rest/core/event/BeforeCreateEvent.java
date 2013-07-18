package org.springframework.data.rest.core.event;

/**
 * Event emitted before an entity is saved for the first time.
 * 
 * @author Jon Brisbin
 */
public class BeforeCreateEvent extends RepositoryEvent {

	private static final long serialVersionUID = -1642841708537223975L;

	public BeforeCreateEvent(Object source) {
		super(source);
	}
}
