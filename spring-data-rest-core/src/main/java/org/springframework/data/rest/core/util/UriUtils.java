package org.springframework.data.rest.core.util;

import java.net.URI;
import java.util.List;
import java.util.Stack;

import org.springframework.data.rest.core.Handler;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public abstract class UriUtils {

  private UriUtils() {
  }

  public static boolean validBaseUri(URI baseUri, URI uri) {
    String path = UriUtils.path(baseUri.relativize(uri));
    return !StringUtils.hasText(path) || path.charAt(0) != '/';
  }

  public static <V> V foreach(URI baseUri, URI uri, Handler<URI, V> handler) {
    List<URI> uris = explode(baseUri, uri);
    V v = null;
    for (URI u : uris) {
      v = handler.handle(u);
    }
    return v;
  }

  public static Stack<URI> explode(URI baseUri, URI uri) {
    Stack<URI> uris = new Stack<URI>();
    if (StringUtils.hasText(uri.getPath())) {
      URI relativeUri = baseUri.relativize(uri);
      if (StringUtils.hasText(relativeUri.getPath())) {
        for (String part : relativeUri.getPath().split("/")) {
          uris.add(URI.create(part + (StringUtils.hasText(uri.getQuery()) ? "?" + uri.getQuery() : "")));
        }
      }
    }
    return uris;
  }

  public static URI merge(URI baseUri, URI... uris) {
    StringBuilder query = new StringBuilder();

    UriComponentsBuilder ub = UriComponentsBuilder.fromUri(baseUri);
    for (URI uri : uris) {
      String s = uri.getScheme();
      if (null != s) {
        ub.scheme(s);
      }

      s = uri.getUserInfo();
      if (null != s) {
        ub.userInfo(s);
      }

      s = uri.getHost();
      if (null != s) {
        ub.host(s);
      }

      int i = uri.getPort();
      if (i > 0) {
        ub.port(i);
      }

      s = uri.getPath();
      if (null != s) {
        if (!uri.isAbsolute() && StringUtils.hasText(s)) {
          ub.pathSegment(s);
        } else {
          ub.path(s);
        }
      }

      s = uri.getQuery();
      if (null != s) {
        if (query.length() > 0) {
          query.append("&");
        }
        query.append(s);
      }

      s = uri.getFragment();
      if (null != s) {
        ub.fragment(s);
      }
    }

    if (query.length() > 0) {
      ub.query(query.toString());
    }

    return ub.build().toUri();
  }

  public static String path(URI uri) {
    if (null == uri) {
      return null;
    }
    String s = uri.getPath();
    if (s.endsWith("/")) {
      return s.substring(0, s.length() - 1);
    } else {
      return s;
    }
  }

  public static URI tail(URI baseUri, URI uri) {
    Stack<URI> uris = explode(baseUri, uri);
    return uris.size() > 0 ? uris.get(Math.max(uris.size() - 1, 0)) : null;
  }

}
