package org.springframework.data.rest.core.spec

import org.springframework.data.rest.core.util.UriUtils
import spock.lang.Specification

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
class UriUtilsSpec extends Specification {

  def "merges URIs correctly"() {

    given:
    // (absolute) URI of the base resource
    def baseUri = new URI("http://localhost:8080/baseUrl")
    // (relative) URI of the top-level Resource
    def uri2 = new URI("resource")
    // (relative) URI of the second-level Resource
    def uri3 = new URI("1")
    // (fragment) URI of the bottom-level Resource
    def uri4 = new URI("count")

    when:
    def uri5 = UriUtils.merge(baseUri, uri2, uri3, uri4)

    then:
    uri5.toString() == "http://localhost:8080/baseUrl/resource/1/count"

  }

  def "explodes URIs correctly"() {

    given:
    // (absolute) URI of the base resource
    def baseUri = new URI("http://localhost:8080/baseUrl")
    // (absolute) URI of the full resource to get a path to
    def resourceUri = new URI("http://localhost:8080/baseUrl/resource/1/property")

    when:
    def uris = UriUtils.explode(baseUri, resourceUri)

    then:
    uris.size() == 3
    uris[2].path == "property"

  }

}
