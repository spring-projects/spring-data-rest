package org.springframework.data.rest.webmvc.spec

import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import spock.lang.Shared

/**
 * @author Jon Brisbin
 */
class RelationshipsSpec extends BaseSpec {

  @Shared
  Long persId
  @Shared
  Long addrId

  def setup() {
    def person = newPerson()
    persId = person.id
    addrId = person.addresses[0].id
  }

  @Transactional
  def "saves entity relationship"() {

    given:
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
    readJson(response).city == "Univille"

  }

  @Transactional
  def "cannot delete a required relationship"() {

    when:
    def request = createRequest("DELETE", "address/$addrId/person/$persId", null)
    def response = controller.deleteLink(request, "address", "$addrId", "person", "$persId")

    then:
    response.statusCode == HttpStatus.METHOD_NOT_ALLOWED

  }

}
