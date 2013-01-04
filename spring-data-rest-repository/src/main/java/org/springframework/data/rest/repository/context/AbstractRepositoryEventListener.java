package org.springframework.data.rest.repository.context;

import static org.springframework.core.GenericTypeResolver.*;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;

/**
 * Abstract class that listens for generic {@link RepositoryEvent}s and dispatches them to a specific
 * method based on the event type.
 *
 * @author Jon Brisbin
 */
public abstract class AbstractRepositoryEventListener<T> implements ApplicationListener<RepositoryEvent>,
                                                                    ApplicationContextAware {

  private final Class<?> INTERESTED_TYPE = resolveTypeArgument(getClass(), AbstractRepositoryEventListener.class);
  protected ApplicationContext applicationContext;

  @Override public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @SuppressWarnings({"unchecked"})
  @Override public final void onApplicationEvent(RepositoryEvent event) {
    Class<?> srcType = event.getSource().getClass();
    if(null != INTERESTED_TYPE && !INTERESTED_TYPE.isAssignableFrom(srcType)) {
      return;
    }

    if(event instanceof BeforeSaveEvent) {
      onBeforeSave((T)event.getSource());
    } else if(event instanceof AfterSaveEvent) {
      onAfterSave((T)event.getSource());
    } else if(event instanceof BeforeLinkSaveEvent) {
      onBeforeLinkSave((T)event.getSource(), ((BeforeLinkSaveEvent)event).getLinked());
    } else if(event instanceof AfterLinkSaveEvent) {
      onAfterLinkSave((T)event.getSource(), ((AfterLinkSaveEvent)event).getLinked());
    } else if(event instanceof BeforeLinkDeleteEvent) {
      onBeforeLinkDelete((T)event.getSource(), ((BeforeLinkDeleteEvent)event).getLinked());
    } else if(event instanceof AfterLinkDeleteEvent) {
      onAfterLinkDelete((T)event.getSource(), ((AfterLinkDeleteEvent)event).getLinked());
    } else if(event instanceof BeforeDeleteEvent) {
      onBeforeDelete((T)event.getSource());
    } else if(event instanceof AfterDeleteEvent) {
      onAfterDelete((T)event.getSource());
    }
  }

  /**
   * Override this method if you are interested in {@literal beforeSave} events.
   *
   * @param entity
   *     The entity being saved.
   */
  protected void onBeforeSave(T entity) {
  }

  /**
   * Override this method if you are interested in {@literal afterSave} events.
   *
   * @param entity
   *     The entity that was just saved.
   */
  protected void onAfterSave(T entity) {
  }

  /**
   * Override this method if you are interested in {@literal beforeLinkSave} events.
   *
   * @param parent
   *     The parent entity to which the child object is linked.
   * @param linked
   *     The linked, child entity.
   */
  protected void onBeforeLinkSave(T parent, Object linked) {
  }

  /**
   * Override this method if you are interested in {@literal afterLinkSave} events.
   *
   * @param parent
   *     The parent entity to which the child object is linked.
   * @param linked
   *     The linked, child entity.
   */
  protected void onAfterLinkSave(T parent, Object linked) {
  }

  /**
   * Override this method if you are interested in {@literal beforeLinkDelete} events.
   *
   * @param parent
   *     The parent entity to which the child object is linked.
   * @param linked
   *     The linked, child entity.
   */
  protected void onBeforeLinkDelete(T parent, Object linked) {
  }

  /**
   * Override this method if you are interested in {@literal afterLinkDelete} events.
   *
   * @param parent
   *     The parent entity to which the child object is linked.
   * @param linked
   *     The linked, child entity.
   */
  protected void onAfterLinkDelete(T parent, Object linked) {
  }

  /**
   * Override this method if you are interested in {@literal beforeDelete} events.
   *
   * @param entity
   *     The entity that is being deleted.
   */
  protected void onBeforeDelete(T entity) {
  }

  /**
   * Override this method if you are interested in {@literal afterDelete} events.
   *
   * @param entity
   *     The entity that was just deleted.
   */
  protected void onAfterDelete(T entity) {
  }

}
