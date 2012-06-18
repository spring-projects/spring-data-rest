package org.springframework.data.rest.webmvc.spec

import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.map.ser.CustomSerializerFactory
import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.data.domain.PageRequest
import org.springframework.data.rest.core.SimpleLink
import org.springframework.data.rest.core.util.FluentBeanSerializer
import org.springframework.data.rest.test.webmvc.Address
import org.springframework.data.rest.webmvc.PagingAndSorting
import org.springframework.data.rest.webmvc.RepositoryRestController
import org.springframework.data.rest.webmvc.RepositoryRestMvcConfiguration
import org.springframework.http.HttpStatus
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockServletConfig
import org.springframework.mock.web.MockServletContext
import org.springframework.orm.jpa.EntityManagerHolder
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.ui.ExtendedModelMap
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext
import org.springframework.web.util.UriComponentsBuilder
import spock.lang.Shared
import spock.lang.Specification

import javax.persistence.EntityManagerFactory

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
  PagingAndSorting pageSort
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
    def servletConfig = new MockServletConfig()
    def servletContext = new MockServletContext()

    def parentCtx = new ClassPathXmlApplicationContext("classpath*:META-INF/spring-data-rest/**/*-export.xml")

    def webAppCtx = new AnnotationConfigWebApplicationContext()
    webAppCtx.servletConfig = servletConfig
    webAppCtx.servletContext = servletContext
    webAppCtx.configLocations = [RepositoryRestMvcConfiguration.name] as String[]
    webAppCtx.parent = parentCtx
    webAppCtx.refresh()

    emf = webAppCtx.getBean(EntityManagerFactory)
    controller = webAppCtx.getBean(RepositoryRestController)
    pageSort = new PagingAndSorting("page", "limit", "sort", new PageRequest(0, 1000))
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

  def "API Test"() {

    given:
    def model = new ExtendedModelMap()

    when: "listing available repositories"
    def mv = controller.listRepositories(uriBuilder)
    def reposLinks = mv.model.resource?.links

    then:
    mv.model.status == HttpStatus.OK
    reposLinks?.size() == 4

    when: "adding an entity"
    model.clear()
    def req = createRequest("POST", "people")
    def data = mapper.writeValueAsBytes([name: "John Doe"])
    req.content = data
    mv = controller.create(new ServletServerHttpRequest(req), req, uriBuilder, "people")

    then:
    mv.model.status == HttpStatus.CREATED

    when: "getting a specific entity"
    model.clear()
    req = createRequest("GET", "people/1")
    mv = controller.entity(new ServletServerHttpRequest(req), uriBuilder, "people", "1")

    then:
    mv.model.resource?.name == "John Doe"

    when: "updating an entity"
    mv.model.clear()
    req = createRequest("PUT", "people/1")
    data = mapper.writeValueAsBytes([name: "Johnnie Doe", version: 0])
    req.content = data
    mv = controller.createOrUpdate(new ServletServerHttpRequest(req), uriBuilder, "people", "1")

    then:
    mv.model.status == HttpStatus.NO_CONTENT

    when: "listing available entities"
    mv.model.clear()
    mv = controller.listEntities(pageSort, uriBuilder, "people")
    def peopleLinks = mv.model.resource?.links

    then:
    mv.model.status == HttpStatus.OK
    peopleLinks[0].href().toString() == "http://localhost:8080/data/people/1"

    when: "creating a child entity"
    mv.model.clear()
    req = createRequest("POST", "address")
    data = mapper.writeValueAsBytes(new Address(["1 W. 1st St."] as String[], "Univille", "ST", "12345"))
    req.content = data
    mv = controller.create(new ServletServerHttpRequest(req), req, uriBuilder, "address")

    then:
    mv.model.status == HttpStatus.CREATED

    when: "linking child to parent entity"
    mv.model.clear()
    req = createRequest("POST", "people/1/addresses")
    req.contentType = "text/uri-list"
    data = "http://localhost:8080/data/address/1".bytes
    req.content = data
    mv = controller.updatePropertyOfEntity(new ServletServerHttpRequest(req), uriBuilder, "people", "1", "addresses")

    then:
    mv.model.status == HttpStatus.CREATED

    when: "getting property of an entity"
    mv.model.clear()
    mv = controller.propertyOfEntity(uriBuilder, "people", "1", "addresses")
    def addrLinks = mv.model.resource?.links

    then:
    null != addrLinks
    addrLinks.size() == 1

  }

}
