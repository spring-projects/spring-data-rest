package org.springframework.data.rest.core.event;

import java.io.Serial;

/**
 * Event emitted before an entity is saved for the first time.
 *
 * @author Jon Brisbin
 */
public class BeforeCreateEvent extends RepositoryEvent {

	private static final @Serial long serialVersionUID = -1642841708537223975L;

	public BeforeCreateEvent(Object source) {
		super(source);
	}
}
