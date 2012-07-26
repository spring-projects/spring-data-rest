package org.springframework.data.rest.webmvc;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import org.springframework.http.MediaType;

/**
 * @author Jon Brisbin
 */
public abstract class MediaTypes {

  private MediaTypes() {
  }

  public static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");

  public static final List<MediaType> ACCEPT_ALL_TYPES       = Collections.singletonList(MediaType.ALL);
  public static final MediaType       COMPACT_JSON           = new MediaType("application",
                                                                             "x-spring-data-compact+json",
                                                                             ISO_8859_1);
  public static final MediaType       VERBOSE_JSON           = new MediaType("application",
                                                                             "x-spring-data-verbose+json",
                                                                             ISO_8859_1);
  public static final MediaType       APPLICATION_JAVASCRIPT = new MediaType("application",
                                                                             "javascript",
                                                                             ISO_8859_1);
  public static final MediaType       URI_LIST               = new MediaType("text",
                                                                             "uri-list",
                                                                             ISO_8859_1);

}
