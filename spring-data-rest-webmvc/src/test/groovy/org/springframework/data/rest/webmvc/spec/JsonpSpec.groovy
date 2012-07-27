package org.springframework.data.rest.webmvc.spec

import org.springframework.http.HttpStatus

/**
 * @author Jon Brisbin
 */
class JsonpSpec extends BaseSpec {

  def "wraps response with JSONP"() {

    given:
    def person = newPerson()
    def request = createRequest("GET", "people/${person.id}", ["callback": "jsonp_callback"])

    when:
    def response = controller.entity(request, baseUri, "people", "${person.id}")
    def body = new String(response.body)

    then:
    response.statusCode == HttpStatus.OK
    body?.startsWith("jsonp_callback")

  }

}
