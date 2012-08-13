package org.springframework.data.rest.repository.context;

import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.data.rest.core.Resource;
import org.springframework.data.rest.core.ResourceSet;
import org.springframework.data.rest.repository.RepositoryExporter;
import org.springframework.data.rest.repository.RepositoryExporterSupport;
import org.springframework.data.rest.repository.RepositoryMetadata;
import org.springframework.http.server.ServerHttpRequest;

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
    } else if(event instanceof BeforeLinkDeleteEvent) {
      onBeforeLinkDelete(event.getSource(), ((BeforeLinkDeleteEvent)event).getLinked());
    } else if(event instanceof AfterLinkDeleteEvent) {
      onAfterLinkDelete(event.getSource(), ((AfterLinkDeleteEvent)event).getLinked());
    } else if(event instanceof BeforeDeleteEvent) {
      onBeforeDelete(event.getSource());
    } else if(event instanceof AfterDeleteEvent) {
      onAfterDelete(event.getSource());
    } else if(event instanceof RenderEvent) {
      RenderEvent ev = (RenderEvent)event;
      if(ev.isTopLevelResource()) {
        onBeforeRenderResources(ev.getRequest(), ev.getRepositoryMetadata(), ev.getResources());
      } else {
        onBeforeRenderResource(ev.getRequest(), ev.getRepositoryMetadata(), ev.getResource());
      }
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
   * Override this method if you are interested in {@literal beforeLinkDelete} events.
   *
   * @param parent
   * @param linked
   */
  protected void onBeforeLinkDelete(Object parent, Object linked) {
  }

  /**
   * Override this method if you are interested in {@literal afterLinkDelete} events.
   *
   * @param parent
   * @param linked
   */
  protected void onAfterLinkDelete(Object parent, Object linked) {
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

  /**
   * Override this method if you are interested in {@literal beforeRender} for top-level events. These are events
   * triggered by the exporter before sending out a wrapped, top-level response for queries, entity lists, and results
   * that are pagable.
   *
   * @param request
   * @param repositoryMetadata
   * @param resources
   */
  protected void onBeforeRenderResources(ServerHttpRequest request,
                                         RepositoryMetadata repositoryMetadata,
                                         ResourceSet resources) {
  }

  /**
   * Override this method if you are interested in {@literal beforeRender} for entity events. These are events emitted
   * by the exporter before sending out an entity representation to the client. These events are triggered when
   * requesting individual entities and specific properties of an entity.
   *
   * @param request
   * @param repositoryMetadata
   * @param resource
   */
  protected void onBeforeRenderResource(ServerHttpRequest request,
                                        RepositoryMetadata repositoryMetadata,
                                        Resource resource) {
  }

}
