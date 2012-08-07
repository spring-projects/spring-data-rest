package org.springframework.data.rest.repository.spec

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.rest.core.Resource
import org.springframework.data.rest.core.Resources
import org.springframework.data.rest.core.SimpleLink
import org.springframework.data.rest.repository.RepositoryExporter
import org.springframework.data.rest.repository.RepositoryMetadata
import org.springframework.data.rest.repository.annotation.HandleAfterDelete
import org.springframework.data.rest.repository.annotation.HandleAfterLinkSave
import org.springframework.data.rest.repository.annotation.HandleAfterSave
import org.springframework.data.rest.repository.annotation.HandleBeforeDelete
import org.springframework.data.rest.repository.annotation.HandleBeforeLinkSave
import org.springframework.data.rest.repository.annotation.HandleBeforeRenderResource
import org.springframework.data.rest.repository.annotation.HandleBeforeRenderResources
import org.springframework.data.rest.repository.annotation.HandleBeforeSave
import org.springframework.data.rest.repository.annotation.RepositoryEventHandler
import org.springframework.data.rest.repository.context.AfterDeleteEvent
import org.springframework.data.rest.repository.context.AfterLinkSaveEvent
import org.springframework.data.rest.repository.context.AfterSaveEvent
import org.springframework.data.rest.repository.context.AnnotatedHandlerRepositoryEventListener
import org.springframework.data.rest.repository.context.BeforeDeleteEvent
import org.springframework.data.rest.repository.context.BeforeLinkSaveEvent
import org.springframework.data.rest.repository.context.BeforeRenderResourceEvent
import org.springframework.data.rest.repository.context.BeforeRenderResourcesEvent
import org.springframework.data.rest.repository.context.BeforeSaveEvent
import org.springframework.data.rest.repository.test.ApplicationConfig
import org.springframework.data.rest.repository.test.Person
import org.springframework.http.server.ServerHttpRequest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
@ContextConfiguration(classes = [ApplicationConfig, EventsApplicationConfig])
class ExtensionsSpec extends Specification {

  @Autowired
  ApplicationContext appCtx
  @Autowired
  PersonEventHandler handler
  @Autowired
  RepositoryExporter exporter

  def "responds to ApplicationEvents in annotated handlers"() {

    given:
    def p = new Person("John Doe")

    when:
    appCtx.publishEvent(new BeforeSaveEvent(p))
    appCtx.publishEvent(new AfterSaveEvent(p))
    appCtx.publishEvent(new BeforeLinkSaveEvent(p, new Object()))
    appCtx.publishEvent(new AfterLinkSaveEvent(p, new Object()))
    appCtx.publishEvent(new BeforeDeleteEvent(p))
    appCtx.publishEvent(new AfterDeleteEvent(p))

    then:
    handler.beforeSave
    handler.afterSave
    handler.beforeChildSave
    handler.afterChildSave
    handler.beforeDelete
    handler.afterDelete

  }

  def "responds to render events"() {

    given:
    def repoMeta = exporter.repositoryMetadataFor(Person)
    def request = Mock(ServerHttpRequest)

    def p = new Person("John Doe")
    def selfLink = new SimpleLink("self", new URI("http://localhost:8080/people/1"))
    def resources = new Resources()
    resources.links << selfLink
    def resource = new Resource(p)
    resource.links << selfLink

    when:
    appCtx.publishEvent(new BeforeRenderResourcesEvent(request, repoMeta, resources))
    appCtx.publishEvent(new BeforeRenderResourceEvent(request, repoMeta, resource))

    then:
    resources.links.size() == 2
    null != resources.links.find { it.rel() == "linkAddedByHandler" }
    resource.links.size() == 2
    null != resources.links.find { it.rel() == "linkAddedByHandler" }

  }

}

@Configuration
class EventsApplicationConfig {

  @Bean AnnotatedHandlerRepositoryEventListener repositoryEventListener() {
    new AnnotatedHandlerRepositoryEventListener("org.springframework.data.rest.repository.spec");
  }

  @Bean PersonEventHandler personEventHandler() {
    new PersonEventHandler()
  }

  @Bean PersonRenderHandler personRenderHandler() {
    new PersonRenderHandler()
  }

}

@RepositoryEventHandler(Person)
class PersonEventHandler {

  def beforeSave = false
  def afterSave = false
  def beforeChildSave = false
  def afterChildSave = false
  def beforeDelete = false
  def afterDelete = false

  @HandleBeforeSave void handleBeforeSave(Person p) {
    beforeSave = true
  }

  @HandleAfterSave void handleAfterSave(Person p) {
    afterSave = true
  }

  @HandleBeforeLinkSave void handleBeforeChildSave(Person p, Object child) {
    beforeChildSave = true
  }

  @HandleAfterLinkSave void handleAfterChildSave(Person p, Object child) {
    afterChildSave = true
  }

  @HandleBeforeDelete void handleBeforeDelete(Person p) {
    beforeDelete = true
  }

  @HandleAfterDelete void handleAfterDelete(Person p) {
    afterDelete = true
  }

}

@RepositoryEventHandler(Person)
class PersonRenderHandler {

  @HandleBeforeRenderResources void handleBeforeRenderResources(ServerHttpRequest request,
                                                                RepositoryMetadata repoMeta,
                                                                Resources resources) {
    resources.links << new SimpleLink("linkAddedByHandler", new URI("http://localhost:8080/linkAddedByHandler"))
  }

  @HandleBeforeRenderResource void handleBeforeRenderResource(ServerHttpRequest request,
                                                              RepositoryMetadata repoMeta,
                                                              Resource resource) {
    resource.links << new SimpleLink("linkAddedByHandler", new URI("http://localhost:8080/linkAddedByHandler"))
  }

}
