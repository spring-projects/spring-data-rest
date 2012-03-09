package org.springframework.data.rest.mvc.spec

import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.map.ser.CustomSerializerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.rest.core.SimpleLink
import org.springframework.data.rest.core.util.FluentBeanSerializer
import org.springframework.data.rest.mvc.RepositoryRestConfiguration
import org.springframework.data.rest.mvc.RepositoryRestController
import org.springframework.data.rest.mvc.RepositoryRestMvcConfiguration
import org.springframework.http.HttpStatus
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import org.springframework.ui.ExtendedModelMap
import spock.lang.Shared
import spock.lang.Specification

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
@ContextConfiguration(classes = [RepositoryRestConfiguration, RepositoryRestMvcConfiguration])
class RepositoryRestControllerSpec extends Specification {

  @Shared
  ObjectMapper mapper = new ObjectMapper()
  @Autowired
  URI baseUri
  @Autowired
  RepositoryRestController controller

  ServletServerHttpRequest createRequest(String method, String path) {
    return new ServletServerHttpRequest(new MockHttpServletRequest(
        serverPort: 8080,
        requestURI: "/data/$path",
        method: method
    ))
  }

  Map GET(String path) {
    def request = createRequest("GET", path)
    def model = new ExtendedModelMap()
    controller.get(request, model)
    return model
  }

  def setupSpec() {
    def customSerializerFactory = new CustomSerializerFactory()
    customSerializerFactory.addSpecificMapping(SimpleLink, new FluentBeanSerializer(SimpleLink))
    mapper.setSerializerFactory(customSerializerFactory)
  }

  @Transactional
  def "responds to GET"() {

    when:
    def repos = GET("")
    def reposLinks = repos.resource?._links

    then:
    repos.status == HttpStatus.OK
    reposLinks?.size() == 3

    when:
    def persons = GET("person")
    def personsLinks = persons.resource?._links

    then:
    persons.status == HttpStatus.OK
    personsLinks[0].href().toString() == "http://localhost:8080/data/person/1"

    when:
    def person = GET("person/1")

    then:
    person?.resource.name == "John Doe"

    when:
    def profiles = GET("person/1/profiles")
    def profilesLinks = profiles?.resource.profiles

    then:
    profilesLinks.size() == 2

  }

}
