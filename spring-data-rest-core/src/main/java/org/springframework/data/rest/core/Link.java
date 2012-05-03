package org.springframework.data.rest.core;

import java.net.URI;

/**
 * A simple bean representing a URI link.
 *
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public interface Link {

  /**
   * The text used in the {@literal rel} attribute.
   *
   * @return {@literal rel} attribute text.
   */
  String rel();

  /**
   * The {@link URI} this link is referencing.
   *
   * @return {@link URI} of this link. Should not be null.
   */
  URI href();

}
