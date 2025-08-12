package org.springframework.data.rest.core.event;

import java.io.Serial;

/**
 * Emitted after saving a linked object to its parent in the repository.
 *
 * @author Jon Brisbin
 */
public class AfterLinkSaveEvent extends LinkedEntityEvent {

	private static final @Serial long serialVersionUID = 261522353893713633L;

	public AfterLinkSaveEvent(Object source, Object child) {
		super(source, child);
	}
}
