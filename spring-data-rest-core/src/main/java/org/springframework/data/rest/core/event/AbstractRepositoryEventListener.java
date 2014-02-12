/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.core.event;

import static org.springframework.core.GenericTypeResolver.*;

import org.springframework.context.ApplicationListener;

/**
 * Abstract class that listens for generic {@link RepositoryEvent}s and dispatches them to a specific method based on
 * the event type.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public abstract class AbstractRepositoryEventListener<T> implements ApplicationListener<RepositoryEvent> {

	private final Class<?> INTERESTED_TYPE = resolveTypeArgument(getClass(), AbstractRepositoryEventListener.class);

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
	 */
	@Override
	@SuppressWarnings({ "unchecked" })
	public final void onApplicationEvent(RepositoryEvent event) {

		Class<?> srcType = event.getSource().getClass();

		if (null != INTERESTED_TYPE && !INTERESTED_TYPE.isAssignableFrom(srcType)) {
			return;
		}

		if (event instanceof BeforeSaveEvent) {
			onBeforeSave((T) event.getSource());
		} else if (event instanceof BeforeCreateEvent) {
			onBeforeCreate((T) event.getSource());
		} else if (event instanceof AfterCreateEvent) {
			onAfterCreate((T) event.getSource());
		} else if (event instanceof AfterSaveEvent) {
			onAfterSave((T) event.getSource());
		} else if (event instanceof BeforeLinkSaveEvent) {
			onBeforeLinkSave((T) event.getSource(), ((BeforeLinkSaveEvent) event).getLinked());
		} else if (event instanceof AfterLinkSaveEvent) {
			onAfterLinkSave((T) event.getSource(), ((AfterLinkSaveEvent) event).getLinked());
		} else if (event instanceof BeforeLinkDeleteEvent) {
			onBeforeLinkDelete((T) event.getSource(), ((BeforeLinkDeleteEvent) event).getLinked());
		} else if (event instanceof AfterLinkDeleteEvent) {
			onAfterLinkDelete((T) event.getSource(), ((AfterLinkDeleteEvent) event).getLinked());
		} else if (event instanceof BeforeDeleteEvent) {
			onBeforeDelete((T) event.getSource());
		} else if (event instanceof AfterDeleteEvent) {
			onAfterDelete((T) event.getSource());
		}
	}

	/**
	 * Override this method if you are interested in {@literal beforeCreate} events.
	 * 
	 * @param entity The entity being created.
	 */
	protected void onBeforeCreate(T entity) {}

	/**
	 * Override this method if you are interested in {@literal afterCreate} events.
	 * 
	 * @param entity The entity that was created.
	 */
	protected void onAfterCreate(T entity) {}

	/**
	 * Override this method if you are interested in {@literal beforeSave} events.
	 * 
	 * @param entity The entity being saved.
	 */
	protected void onBeforeSave(T entity) {}

	/**
	 * Override this method if you are interested in {@literal afterSave} events.
	 * 
	 * @param entity The entity that was just saved.
	 */
	protected void onAfterSave(T entity) {}

	/**
	 * Override this method if you are interested in {@literal beforeLinkSave} events.
	 * 
	 * @param parent The parent entity to which the child object is linked.
	 * @param linked The linked, child entity.
	 */
	protected void onBeforeLinkSave(T parent, Object linked) {}

	/**
	 * Override this method if you are interested in {@literal afterLinkSave} events.
	 * 
	 * @param parent The parent entity to which the child object is linked.
	 * @param linked The linked, child entity.
	 */
	protected void onAfterLinkSave(T parent, Object linked) {}

	/**
	 * Override this method if you are interested in {@literal beforeLinkDelete} events.
	 * 
	 * @param parent The parent entity to which the child object is linked.
	 * @param linked The linked, child entity.
	 */
	protected void onBeforeLinkDelete(T parent, Object linked) {}

	/**
	 * Override this method if you are interested in {@literal afterLinkDelete} events.
	 * 
	 * @param parent The parent entity to which the child object is linked.
	 * @param linked The linked, child entity.
	 */
	protected void onAfterLinkDelete(T parent, Object linked) {}

	/**
	 * Override this method if you are interested in {@literal beforeDelete} events.
	 * 
	 * @param entity The entity that is being deleted.
	 */
	protected void onBeforeDelete(T entity) {}

	/**
	 * Override this method if you are interested in {@literal afterDelete} events.
	 * 
	 * @param entity The entity that was just deleted.
	 */
	protected void onAfterDelete(T entity) {}

}
