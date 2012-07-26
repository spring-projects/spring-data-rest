package org.springframework.data.rest.repository.context;

import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.data.rest.repository.RepositoryExporter;
import org.springframework.data.rest.repository.RepositoryExporterSupport;

/**
 * Abstract class that listens for generic {@link RepositoryEvent}s and dispatches them to a specific
 * method based on the event type.
 *
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public abstract class AbstractRepositoryEventListener<T extends AbstractRepositoryEventListener<? super T>>
    extends RepositoryExporterSupport<T>
    implements ApplicationListener<RepositoryEvent>,
               ApplicationContextAware {

  protected ApplicationContext applicationContext;

  @Override public void setApplicationContext(ApplicationContext applicationContext)
      throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Autowired
  public void setRepositoryExporters(List<RepositoryExporter> repositoryExporters) {
    super.setRepositoryExporters(repositoryExporters);
  }

  @Override public final void onApplicationEvent(RepositoryEvent event) {
    if(event instanceof BeforeSaveEvent) {
      onBeforeSave(event.getSource());
    } else if(event instanceof AfterSaveEvent) {
      onAfterSave(event.getSource());
    } else if(event instanceof BeforeLinkSaveEvent) {
      onBeforeLinkSave(event.getSource(), ((BeforeLinkSaveEvent)event).getLinked());
    } else if(event instanceof AfterLinkSaveEvent) {
      onAfterLinkSave(event.getSource(), ((AfterLinkSaveEvent)event).getLinked());
    } else if(event instanceof BeforeDeleteEvent) {
      onBeforeDelete(event.getSource());
    } else if(event instanceof AfterDeleteEvent) {
      onAfterDelete(event.getSource());
    }
  }

  /**
   * Override this method if you are interested in {@literal beforeSave} events.
   *
   * @param entity
   */
  protected void onBeforeSave(Object entity) {
  }

  /**
   * Override this method if you are interested in {@literal afterSave} events.
   *
   * @param entity
   */
  protected void onAfterSave(Object entity) {
  }

  /**
   * Override this method if you are interested in {@literal beforeLinkSave} events.
   *
   * @param parent
   * @param linked
   */
  protected void onBeforeLinkSave(Object parent, Object linked) {
  }

  /**
   * Override this method if you are interested in {@literal afterLinkSave} events.
   *
   * @param parent
   * @param linked
   */
  protected void onAfterLinkSave(Object parent, Object linked) {
  }

  /**
   * Override this method if you are interested in {@literal beforeDelete} events.
   *
   * @param entity
   */
  protected void onBeforeDelete(Object entity) {
  }

  /**
   * Override this method if you are interested in {@literal afterDelete} events.
   *
   * @param entity
   */
  protected void onAfterDelete(Object entity) {
  }

}
