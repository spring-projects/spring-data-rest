package org.springframework.data.rest.core.event;

import java.io.Serial;

/**
 * Emitted before an entity is saved into the repository.
 */
public class BeforeSaveEvent extends RepositoryEvent {

	private static final @Serial long serialVersionUID = -1404580942928384726L;

	public BeforeSaveEvent(Object source) {
		super(source);
	}
}
