package org.springframework.data.rest.core;

import java.net.URI;

/**
 * Implementations of this interface are responsible for turning {@link URI}s into real objects.
 *
 * @author Jon Brisbin
 */
public interface UriResolver<T> {

  /**
   * Take a {@link URI} and resolve it to an actual object.
   *
   * @param baseUri
   *     The base URI that this resource is relative to.
   * @param uri
   *     The URI id of the resource.
   *
   * @return The resolved object or {@literal null} if not found.
   */
  T resolve(URI baseUri, URI uri);

}
