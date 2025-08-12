package org.springframework.data.rest.core.event;

import java.io.Serial;

/**
 * Emitted before a link to a related object is deleted from the parent.
 *
 * @author Jon Brisbin
 */
public class BeforeLinkDeleteEvent extends LinkedEntityEvent {

	private static final @Serial long serialVersionUID = -973540913790564962L;

	public BeforeLinkDeleteEvent(Object source, Object linked) {
		super(source, linked);
	}
}
