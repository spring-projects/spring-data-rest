package org.springframework.data.rest.core.event;

/**
 * Emitted before a link to a related object is deleted from the parent.
 * 
 * @author Jon Brisbin
 */
public class BeforeLinkDeleteEvent extends LinkSaveEvent {

	private static final long serialVersionUID = -973540913790564962L;

	public BeforeLinkDeleteEvent(Object source, Object linked) {
		super(source, linked);
	}
}
