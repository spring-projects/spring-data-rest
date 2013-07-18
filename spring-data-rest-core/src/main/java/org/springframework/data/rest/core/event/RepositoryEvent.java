package org.springframework.data.rest.core.event;

import org.springframework.context.ApplicationEvent;

/**
 * Abstract base class for events emitted by the REST exporter.
 * 
 * @author Jon Brisbin
 */
public abstract class RepositoryEvent extends ApplicationEvent {

	private static final long serialVersionUID = -966689410815418259L;

	protected RepositoryEvent(Object source) {
		super(source);
	}
}
