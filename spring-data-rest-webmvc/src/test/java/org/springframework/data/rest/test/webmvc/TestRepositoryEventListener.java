package org.springframework.data.rest.test.webmvc;

import java.util.ArrayList;
import java.util.List;

import groovy.lang.Closure;
import org.springframework.data.rest.repository.context.AbstractRepositoryEventListener;

/**
 * @author Jon Brisbin
 */
public class TestRepositoryEventListener extends AbstractRepositoryEventListener<TestRepositoryEventListener> {

  private List<Closure> handlers = new ArrayList<Closure>();

  public List<Closure> getHandlers() {
    return handlers;
  }

  @Override protected void onBeforeSave(Object entity) {
    for(Closure cl : handlers) {
      cl.call("beforeSave", entity);
    }
  }

  @Override protected void onAfterSave(Object entity) {
    for(Closure cl : handlers) {
      cl.call("afterSave", entity);
    }
  }

}
