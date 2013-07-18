package org.springframework.data.rest.core.event;

/**
 * Emitted before an entity is saved into the repository.
 */
public class BeforeSaveEvent extends RepositoryEvent {

	private static final long serialVersionUID = -1404580942928384726L;

	public BeforeSaveEvent(Object source) {
		super(source);
	}
}
