package org.springframework.data.rest.core.event;

/**
 * Emitted after a link to a related object is deleted from the parent.
 * 
 * @author Jon Brisbin
 */
public class AfterLinkDeleteEvent extends LinkSaveEvent {

	private static final long serialVersionUID = 3887575011761146290L;

	public AfterLinkDeleteEvent(Object source, Object linked) {
		super(source, linked);
	}
}
