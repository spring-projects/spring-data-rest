package org.springframework.data.rest.core.util;

import java.net.URI;
import java.util.List;
import java.util.Stack;

import com.google.common.base.Function;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Helper methods for dealing with URIs.
 *
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public abstract class UriUtils {

  private UriUtils() {
  }

  /**
   * Is the given {@link URI} based on the "base" {@link URI}?
   * <p>e.g. given a base URI of {@literal http://localhost:8080/data} and a URI of {@code
   * http://localhost:8080/data/person}, this method would report the baseUri being a valid base of the given URI.
   * </p>
   *
   * @param baseUri
   *     {@link URI} to check.
   * @param uri
   *     {@link URI} against which to compare the base.
   *
   * @return {@literal true} if the baseUri is valid against the given {@link URI}, {@literal false} otherwise.
   */
  public static boolean validBaseUri(URI baseUri, URI uri) {
    String path = UriUtils.path(baseUri.relativize(uri));
    return !StringUtils.hasText(path) || path.charAt(0) != '/';
  }

  /**
   * Execute the given {@link Function} for each segment in the {@link URI}.
   * <p>e.g. given a URI of {@literal http://localhost:8080/data/person/1} and a base URI of {@code
   * http://localhost:8080/data}, this method will explode the URI into it's components, as compared to the base URI.
   * The result would be: the given handler gets called twice, once passing a relative {@link URI} of "person" and a
   * second time passing a relative {@link URI} of "1".
   * </p>
   *
   * @param baseUri
   *     base {@link URI}
   * @param uri
   *     {@link URI} to explode and iterate over.
   * @param handler
   *     {@link Function} to call for each segment of the URI's path.
   * @param <V>
   *     Return type of the handler.
   *
   * @return Handler return value, or possibly {@literal null}.
   */
  public static <V> V foreach(URI baseUri, URI uri, Function<URI, V> handler) {
    List<URI> uris = explode(baseUri, uri);
    V v = null;
    for(URI u : uris) {
      v = handler.apply(u);
    }
    return v;
  }

  /**
   * Explode the given {@link URI} into its component parts, as compared to the base {@link URI}.
   * <p>Given a base URI of {@literal http://localhost:8080/data}, exploding the URI {@code
   * http://localhost:8080/data/person/1} strips the first part of the URI, leaving {@literal person/1}. This results
   * in
   * a {@link Stack} of relative {@link URI}s of size 2--one for "person" and one for "1".</p>
   *
   * @param baseUri
   *     base {@link URI}
   * @param uri
   *     {@link URI} to explode
   *
   * @return {@link Stack} of relative {@link URI}s.
   */
  public static Stack<URI> explode(URI baseUri, URI uri) {
    Stack<URI> uris = new Stack<URI>();
    if(StringUtils.hasText(uri.getPath())) {
      URI relativeUri = baseUri.relativize(uri);
      if(StringUtils.hasText(relativeUri.getPath())) {
        for(String part : relativeUri.getPath().split("/")) {
          uris.add(URI.create(part + (StringUtils.hasText(uri.getQuery()) ? "?" + uri.getQuery() : "")));
        }
      }
    }
    return uris;
  }

  /**
   * Merge the components of these {@link URI}s into a single URI. Useful for combining a relative URI with a base URI
   * and coming up with a full absolute URI.
   * <p>e.g. merging base URI {@literal http://localhost:8080/data} and relative uri {@literal person/1?name=John+Doe}
   * would result in an absolute URI of {@literal http://localhost:8080/data/person/1?name=John+Doe}</p>
   *
   * @param baseUri
   *     base {@link URI}
   * @param uris
   *     {@link URI}s to merge
   *
   * @return {@link URI} that is the combination of all the given (possibly relative, possibly absolute) URIs.
   */
  public static URI merge(URI baseUri, URI... uris) {
    StringBuilder query = new StringBuilder();

    UriComponentsBuilder ub = UriComponentsBuilder.fromUri(baseUri);
    for(URI uri : uris) {
      String s = uri.getScheme();
      if(null != s) {
        ub.scheme(s);
      }

      s = uri.getUserInfo();
      if(null != s) {
        ub.userInfo(s);
      }

      s = uri.getHost();
      if(null != s) {
        ub.host(s);
      }

      int i = uri.getPort();
      if(i > 0) {
        ub.port(i);
      }

      s = uri.getPath();
      if(null != s) {
        if(!uri.isAbsolute() && StringUtils.hasText(s)) {
          ub.pathSegment(s);
        } else {
          ub.path(s);
        }
      }

      s = uri.getQuery();
      if(null != s) {
        if(query.length() > 0) {
          query.append("&");
        }
        query.append(s);
      }

      s = uri.getFragment();
      if(null != s) {
        ub.fragment(s);
      }
    }

    if(query.length() > 0) {
      ub.query(query.toString());
    }

    return ub.build().toUri();
  }

  /**
   * Just the path portion of the {@link URI}, but with any trailing slash "/" removed.
   *
   * @param uri
   *     path URI
   *
   * @return the path portion of the URI, but with any trailing slash removed
   */
  public static String path(URI uri) {
    if(null == uri) {
      return null;
    }
    String s = uri.getPath();
    if(s.endsWith("/")) {
      return s.substring(0, s.length() - 1);
    } else {
      return s;
    }
  }

  /**
   * The very last segment of the {@link URI}.
   *
   * @param baseUri
   *     base {@link URI}
   * @param uri
   *     {@link URI} to explode
   *
   * @return Relative {@link URI} that is the last segment of the path for the given URI.
   */
  public static URI tail(URI baseUri, URI uri) {
    Stack<URI> uris = explode(baseUri, uri);
    return uris.size() > 0 ? uris.get(Math.max(uris.size() - 1, 0)) : null;
  }

  /**
   * Create a new {@link URI} out of the components.
   *
   * @param baseUri
   *     The base URI these path segments are relative to.
   * @param pathSegments
   *     The path segments to add to the given base URI.
   *
   * @return A new URI built from the given base URI and additional path segments.
   */
  public static URI buildUri(URI baseUri, String... pathSegments) {
    return UriComponentsBuilder.fromUri(baseUri).pathSegment(pathSegments).build().toUri();
  }

}
