package org.springframework.data.rest.core.event;

import java.io.Serial;

/**
 * Emitted after a save to the repository.
 *
 * @author Jon Brisbin
 */
public class AfterSaveEvent extends RepositoryEvent {

	private static final @Serial long serialVersionUID = 8568843338617401903L;

	public AfterSaveEvent(Object source) {
		super(source);
	}
}
