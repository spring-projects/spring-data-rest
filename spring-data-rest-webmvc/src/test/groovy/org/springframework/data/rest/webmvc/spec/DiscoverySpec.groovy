package org.springframework.data.rest.webmvc.spec

import org.springframework.data.domain.PageRequest
import org.springframework.data.rest.webmvc.PagingAndSorting
import org.springframework.data.rest.webmvc.RepositoryRestConfiguration
import org.springframework.http.HttpStatus

/**
 * @author Jon Brisbin
 */
class DiscoverySpec extends BaseSpec {

  def "exposes configured repositories for discovery"() {

    given:
    def request = createRequest("GET", "", null)

    when:
    def response = controller.listRepositories(request, baseUri)

    then:
    response.statusCode == HttpStatus.OK

    when:
    def links = readJson(response)._links

    then:
    links.size() == 4

  }

  def "lists entities for discovery"() {

    given:
    (1..20).each { newPerson() }
    def pageSort = new PagingAndSorting(RepositoryRestConfiguration.DEFAULT, new PageRequest(0, 10))
    def request = createRequest("GET", "people", null)

    when:
    def response = controller.listEntities(request, pageSort, baseUri, "people")

    then:
    response.statusCode == HttpStatus.OK

  }

}
