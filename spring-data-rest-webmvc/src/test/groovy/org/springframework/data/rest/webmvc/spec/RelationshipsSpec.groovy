package org.springframework.data.rest.webmvc.spec

import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional

/**
 * @author Jon Brisbin
 */
class RelationshipsSpec extends BaseSpec {

  @Transactional
  def "saves entity relationship"() {

    given:
    def person = newPerson()
    def persId = person.id
    def addr = newAddress("Smallville")
    def addrId = addr.id
    def request = createUriListRequest(
        "POST",
        "people/$persId/addresses",
        null,
        [baseUri.pathSegment("address", "$addrId").build().toUriString()]
    )

    when:
    def response = controller.updatePropertyOfEntity(request, baseUri, "people", "$persId", "addresses")

    then:
    response.statusCode == HttpStatus.CREATED

    when:
    request = createRequest("GET", "people/$persId/addresses/$addrId", null)
    response = controller.linkedEntity(request, baseUri, "people", "$persId", "addresses", "$addrId")

    then:
    response.statusCode == HttpStatus.OK
    readJson(response).city == "Smallville"

  }

}
