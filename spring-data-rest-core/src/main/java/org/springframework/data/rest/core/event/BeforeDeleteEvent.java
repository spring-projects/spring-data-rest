package org.springframework.data.rest.core.event;

import java.io.Serial;

/**
 * Emitted before an entity is deleted from the repository.
 *
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class BeforeDeleteEvent extends RepositoryEvent {

	private static final @Serial long serialVersionUID = 9150212393209433211L;

	public BeforeDeleteEvent(Object source) {
		super(source);
	}
}
