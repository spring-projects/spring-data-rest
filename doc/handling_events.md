# Handling ApplicationEvents in the REST Exporter

There are eight different events that the REST exporter emits throughout the process of working with an entity. Those are:

* BeforeSaveEvent
* AfterSaveEvent
* BeforeLinkSaveEvent
* AfterLinkSaveEvent
* BeforeDeleteEvent
* AfterDeleteEvent
* BeforeRenderResourcesEvent
* BeforeRenderResourceEvent

### ApplicationListener

There is an abstract class you can subclass which listens for these kinds of events and calls
the appropriate method based on the event type. You just override the methods for
the events you're interested in.

    public class BeforeSaveEventListener extends AbstractRepositoryEventListener {

      @Override public void onBeforeSave(Object entity) {
        ... logic to handle inspecting the entity before the Repository saves it
      }

      @Override public void onAfterDelete(Object entity) {
        ... send a message that this entity has been deleted
      }

    }

One thing to note with this approach, however, is that it makes no distinction based on
the type of the entity. You'll have to inspect that yourself.

### Annotated Handler

Another approach is to use an annotated handler, which does filter events based on domain type.

To declare a handler, create a POJO and put the `@RepositoryEventHandler` annotation on it.
This tells the classpath scanner that this class needs to be inspected for handler methods.

Once it finds a class with this annotation, it iterates over the exposed methods and looks for
annotations that correspond to the event you're interested in. For example, to handle BeforeSaveEvents
in an annotated POJO for different kinds of domain types, you'd define your class like this:

    @RepositoryEventHandler
    public class PersonEventHandler {

      @HandleBeforeSave(Person.class) public void handlePersonSave(Person p) {
        ... you can now deal with Person in a type-safe way
      }

      @HandleBeforeSave(Profile.class) public void handleProfileSave(Profile p) {
        ... you can now deal with Profile in a type-safe way
      }

    }

You can also declare the domain type at the class level:

    @RepositoryEventHandler(Person.class)
    public class PersonEventHandler {

      @HandleBeforeSave public void handleBeforeSave(Person p) {
        ...
      }

      @HandleAfterDelete public void handleAfterDelete(Person p) {
        ...
      }

    }

To actually get your handler invoked, however, you need to declare an instance of it in your
ApplicationContext. The classpath scanner will look for event handlers and build up information
about them, but it won't actually wire a handler to accept events unless there's an instance of
it declared in your ApplicationContext.

(In JavaConfig style):

    @Configuration
    public class RepositoryConfiguration {

      @Bean PersonEventHandler personEventHandler() {
        return new PersonEventHandler();
      }

    }

When you have your beans properly declared, you need to declare an instance of the ApplicationListener.
You can pass the base package of the packages you want searched for handlers in the constructor.

    @Configuration
    public class RepositoryConfiguration {

      @Bean PersonEventHandler personEventHandler() {
        return new PersonEventHandler();
      }

      @Bean AnnotatedHandlerRepositoryEventListener repositoryEventListener() {
        return new AnnotatedHandlerRepositoryEventListener("com.mycompany.repository.handlers");
      }

    }

(In XML style):

      <bean class="com.mycompany.repository.handlers.PersonEventHandler"/>

      <bean class="org.springframework.data.rest.repository.context.AnnotatedHandlerRepositoryEventListener">
        <property name="basePackage" value="com.mycompany.repository.handlers"/>
      </bean>

### Handling Render events

It's possible to alter the representation of the resource that gets sent back to the client. There are two possible reponses: a `org.springframework.data.rest.core.Resources` bean, which has a `content` and `links` property. The `content` property is a list of `org.springframework.data.rest.core.Resource` objects. In the case of JPA @Entities, these will actually be the `org.springframework.data.rest.core.MapResource` subclass of `Resource`. These helpers are what are run through the JSON mapper and used to produce the output sent to the client.

If, for example, you want to add links to the representation for a `Person`, you could define a POJO class and annotate it with `@RepositoryEventHandler(Person.class)` just like in the above example. Then you could define a couple of methods for altering the representations of the `Resources` and `Resource` objects that are sent to the client.

The `org.springframework.data.rest.core.Resources` bean is sent to the client whenever lists or results are required. If you call a query method or ask for a list of entities, then the `Resources` response is the one you'll get. If you ask for a specific entity's representation, however, you'll want to alter the plain `org.springframework.data.rest.core.Resource` bean.

An example handler class for these would look something like this:

		@RepositoryEventHandler(Person.class)
		public class PersonRenderHandler {

			@HandleBeforeRenderResources
			public void handleBeforeRenderResources(ServerHttpRequest request, RepositoryMetadata repoMeta, Resources resources) {
				resources.addLink(new SimpleLink("linkAddedByHandler", new URI("http://localhost:8080/linkAddedByHandler"));
			}

			@HandleBeforeRenderResource
			public void handleBeforeRenderResource(ServerHttpRequest request, RepositoryMetadata repoMeta, Resource resource) {
				resource.addLink(new SimpleLink("linkAddedByHandler", new URI("http://localhost:8080/linkAddedByHandler"));
			}

		}
