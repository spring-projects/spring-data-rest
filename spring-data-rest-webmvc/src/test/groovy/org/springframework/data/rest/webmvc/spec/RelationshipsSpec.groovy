package org.springframework.data.rest.webmvc.spec

import org.springframework.data.rest.test.webmvc.Person
import org.springframework.data.rest.test.webmvc.Profile
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.util.UriComponentsBuilder
import spock.lang.Shared

/**
 * @author Jon Brisbin
 */
class RelationshipsSpec extends BaseSpec {

  @Shared
  Long persId
  @Shared
  Long addrId
  @Shared
  Long profileId

  def setup() {
    def person = people.save(new Person(name: "John Doe"))
    persId = person.id
    def addr = newAddress("Uniontown")
    addrId = addr.id
    def profile = profiles.save(new Profile(type: "socialmedia", url: "http://socialmedia.com", person: person))
    person.profiles = ["socialmedia": profile]
    people.save(person)
    profileId = profile.id
  }

  @Transactional
  def "saves entity relationship"() {

    given:
    def request = createUriListRequest(
        "POST",
        "people/$persId/addresses",
        null,
        [UriComponentsBuilder.fromUri(baseUri).pathSegment("address", "$addrId").build().toUriString()]
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
    readJson(response).city == "Uniontown"

  }

  @Transactional
  def "cannot delete a required relationship"() {

    when:
    def request = createRequest("DELETE", "profile/$profileId/person", null)
    def response = controller.clearLinks(request, "profile", "$profileId", "person")

    then:
    response.statusCode == HttpStatus.METHOD_NOT_ALLOWED

  }

}
