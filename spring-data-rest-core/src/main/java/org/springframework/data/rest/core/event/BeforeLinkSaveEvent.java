package org.springframework.data.rest.core.event;

import java.io.Serial;

/**
 * Emitted before a linked object is saved to the repository.
 *
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class BeforeLinkSaveEvent extends LinkedEntityEvent {

	private static final @Serial long serialVersionUID = 4836932640633578985L;

	public BeforeLinkSaveEvent(Object source, Object linked) {
		super(source, linked);
	}
}
