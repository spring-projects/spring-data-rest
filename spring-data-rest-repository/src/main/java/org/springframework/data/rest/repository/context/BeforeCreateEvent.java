package org.springframework.data.rest.repository.context;

/**
 * Event emitted before an entity is saved for the first time.
 *
 * @author Jon Brisbin
 */
public class BeforeCreateEvent extends RepositoryEvent {
	public BeforeCreateEvent(Object source) {
		super(source);
	}
}
