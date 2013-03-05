package org.springframework.data.rest.repository.context;

/**
 * Event that is emitted after a new entity is saved.
 *
 * @author Jon Brisbin
 */
public class AfterCreateEvent extends RepositoryEvent {
	public AfterCreateEvent(Object source) {
		super(source);
	}
}
