package org.springframework.data.rest.repository.context;

/**
 * Emitted before an entity is saved into the repository.
 */
public class BeforeSaveEvent extends RepositoryEvent {
	public BeforeSaveEvent(Object source) {
		super(source);
	}
}
