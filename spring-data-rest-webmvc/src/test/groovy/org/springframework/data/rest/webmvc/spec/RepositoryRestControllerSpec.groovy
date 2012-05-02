package org.springframework.data.rest.webmvc.spec

import javax.persistence.EntityManagerFactory
import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.map.ser.CustomSerializerFactory
import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.data.rest.core.SimpleLink
import org.springframework.data.rest.core.util.FluentBeanSerializer
import org.springframework.data.rest.test.webmvc.Address
import org.springframework.data.rest.webmvc.RepositoryRestConfiguration
import org.springframework.data.rest.webmvc.RepositoryRestController
import org.springframework.data.rest.webmvc.RepositoryRestMvcConfiguration
import org.springframework.http.HttpStatus
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockServletConfig
import org.springframework.mock.web.MockServletContext
import org.springframework.orm.jpa.EntityManagerHolder
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.ui.ExtendedModelMap
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext
import org.springframework.web.util.UriComponentsBuilder
import spock.lang.Shared
import spock.lang.Specification

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
class RepositoryRestControllerSpec extends Specification {

  @Shared
  UriComponentsBuilder uriBuilder
  @Shared
  ObjectMapper mapper = new ObjectMapper()
  @Shared
  RepositoryRestController controller
  @Shared
  EntityManagerFactory emf

  MockHttpServletRequest createRequest(String method, String path) {
    return new MockHttpServletRequest(
        serverPort: 8080,
        requestURI: "/data/$path",
        method: method
    )
  }

  /**
   * Try to set up things similarly to how they get loaded in the webapp.
   */
  def setupSpec() {
    def appCtx = new ClassPathXmlApplicationContext("classpath*:META-INF/spring-data-rest/**/*-export.xml")
    emf = appCtx.getBean(EntityManagerFactory)

    def webAppCtx = new AnnotationConfigWebApplicationContext()
    webAppCtx.setServletConfig(new MockServletConfig())
    webAppCtx.setServletContext(new MockServletContext())
    webAppCtx.setConfigLocations([RepositoryRestConfiguration.name, RepositoryRestMvcConfiguration.name] as String[])
    webAppCtx.setParent(appCtx)
    webAppCtx.afterPropertiesSet()

    controller = webAppCtx.getBean(RepositoryRestController)

    uriBuilder = UriComponentsBuilder.fromUriString("http://localhost:8080/data")

    def customSerializerFactory = new CustomSerializerFactory()
    customSerializerFactory.addSpecificMapping(SimpleLink, new FluentBeanSerializer(SimpleLink))
    mapper.setSerializerFactory(customSerializerFactory)
  }

  def setup() {
    if (!TransactionSynchronizationManager.hasResource(emf)) {
      TransactionSynchronizationManager.bindResource(emf, new EntityManagerHolder(emf.createEntityManager()))
    }
  }

  @Transactional
  def "API Test"() {

    given:
    def model = new ExtendedModelMap()

    when: "listing available repositories"
    controller.listRepositories(uriBuilder, model)
    def reposLinks = model.resource?.links

    then:
    model.status == HttpStatus.OK
    reposLinks?.size() == 3

    when: "adding an entity"
    model.clear()
    def req = createRequest("POST", "people")
    def data = mapper.writeValueAsBytes([name: "John Doe"])
    req.content = data
    controller.create(new ServletServerHttpRequest(req), uriBuilder, "people", model)

    then:
    model.status == HttpStatus.CREATED

    when: "getting specific entity"
    model.clear()
    req = createRequest("GET", "people/1")
    controller.entity(new ServletServerHttpRequest(req), uriBuilder, "people", "1", model)

    then:
    model.resource?.name == "John Doe"

    when: "updating an entity"
    model.clear()
    req = createRequest("PUT", "people/1")
    data = mapper.writeValueAsBytes([name: "Johnnie Doe", version: 0])
    req.content = data
    controller.createOrUpdate(new ServletServerHttpRequest(req), uriBuilder, "people", "1", model)

    then:
    model.status == HttpStatus.NO_CONTENT

    when: "listing available entities"
    model.clear()
    controller.listEntities(uriBuilder, "people", model)
    def peopleLinks = model.resource?.links

    then:
    model.status == HttpStatus.OK
    peopleLinks[0].href().toString() == "http://localhost:8080/data/people/1"

    when: "creating child entity"
    model.clear()
    req = createRequest("POST", "address")
    data = mapper.writeValueAsBytes(new Address(["1 W. 1st St."] as String[], "Univille", "ST", "12345"))
    req.content = data
    controller.create(new ServletServerHttpRequest(req), uriBuilder, "address", model)

    then:
    model.status == HttpStatus.CREATED

    when: "linking child to parent entity"
    model.clear()
    req = createRequest("POST", "people/1/addresses")
    req.contentType = "text/uri-list"
    data = "http://localhost:8080/data/address/1".bytes
    req.content = data
    controller.updatePropertyOfEntity(new ServletServerHttpRequest(req), uriBuilder, "people", "1", "addresses", model)

    then:
    model.status == HttpStatus.CREATED

    when: "getting property of entity"
    model.clear()
    controller.propertyOfEntity(uriBuilder, "people", "1", "addresses", model)
    def addrLinks = model.resource?.links

    then:
    null != addrLinks
    addrLinks.size() == 1

  }

}
