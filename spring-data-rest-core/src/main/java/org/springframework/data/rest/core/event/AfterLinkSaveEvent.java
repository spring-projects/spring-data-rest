package org.springframework.data.rest.core.event;

/**
 * Emitted after saving a linked object to its parent in the repository.
 * 
 * @author Jon Brisbin
 */
public class AfterLinkSaveEvent extends LinkSaveEvent {

	private static final long serialVersionUID = 261522353893713633L;

	public AfterLinkSaveEvent(Object source, Object child) {
		super(source, child);
	}
}
