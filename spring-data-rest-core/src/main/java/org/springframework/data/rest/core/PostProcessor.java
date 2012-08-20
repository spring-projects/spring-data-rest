package org.springframework.data.rest.core;

import org.springframework.http.server.ServerHttpRequest;

/**
 * Implementations of this interface will post-process objects to mutate them in ways meaningful to the context in
 * which they are called.
 *
 * @author Jon Brisbin
 */
public interface PostProcessor<T> {

  /**
   * Possibly perform some mutation on the object as a post-processing step in a flow.
   *
   * @param obj
   */
  T postProcess(ServerHttpRequest request, T obj);

}
