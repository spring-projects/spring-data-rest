package org.springframework.data.rest.core.util;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.google.common.base.Function;
import org.junit.Test;

/**
 * Tests to verify that {@link UriUtils} can manipulate {@link URI}s.
 *
 * @author Jon Brisbin
 */
public class UriUtilsUnitTests {

  private static final String BASE_URI_STR = "http://localhost:8080/data";
  private static final URI    BASE_URI     = URI.create(BASE_URI_STR);

  private static final String PERSON_2LVL_STR = BASE_URI_STR + "/person/1";
  private static final URI    PERSON_2LVL_URI = URI.create(PERSON_2LVL_STR);

  @Test
  public void shouldValidateBaseURI() throws Exception {
    URI uri = new URI(BASE_URI + "/person/1");

    assertThat(UriUtils.validBaseUri(BASE_URI, uri), is(true));
  }

  @Test
  public void shouldIterateOverPathElements() throws Exception {
    final List<String> paths = new ArrayList<String>();
    Function<URI, Void> fn = new Function<URI, Void>() {
      @Override public Void apply(URI uri) {
        paths.add(uri.getPath());
        return null;
      }
    };

    UriUtils.foreach(BASE_URI, PERSON_2LVL_URI, fn);

    assertThat(paths, hasSize(2));
    assertThat(paths, contains("person", "1"));
  }

  @Test
  public void shouldExplodeRelativeURI() throws Exception {
    Stack<URI> uris = UriUtils.explode(BASE_URI, PERSON_2LVL_URI);

    assertThat(uris, hasSize(2));
    assertThat(uris, contains(URI.create("person"), URI.create("1")));
  }

  @Test
  public void shouldMergeDifferentURIsIntoOne() throws Exception {
    String qrystr = "?queryParam=testValue";

    URI uriWithQuery = URI.create(qrystr);
    URI uriWithPath = URI.create("person/1");

    URI uri = UriUtils.merge(BASE_URI, uriWithPath, uriWithQuery);

    assertThat(uri.toString(), is(PERSON_2LVL_STR + qrystr));
  }

  @Test
  public void shouldStripTrailingSlashFromPath() throws Exception {
    URI uri = URI.create("person/");

    String path = UriUtils.path(uri);

    assertThat(path, is("person"));
  }

  @Test
  public void shouldStripTheLastPathSegmentFromAURI() throws Exception {
    URI uri = UriUtils.tail(BASE_URI, PERSON_2LVL_URI);

    assertThat(uri, is(URI.create("1")));
  }

  @Test
  public void shouldBuildURIFromPathSegments() throws Exception {
    URI uri = UriUtils.buildUri(BASE_URI, "person", "1");

    assertThat(uri, is(PERSON_2LVL_URI));
  }

}
