package org.springframework.data.rest.core;

import java.net.URI;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public interface Link {

  String rel();

  URI href();

}
