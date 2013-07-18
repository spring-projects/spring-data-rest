package org.springframework.data.rest.core.event;

/**
 * Emitted before a linked object is saved to the repository.
 * 
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class BeforeLinkSaveEvent extends LinkSaveEvent {

	private static final long serialVersionUID = 4836932640633578985L;

	public BeforeLinkSaveEvent(Object source, Object linked) {
		super(source, linked);
	}
}
