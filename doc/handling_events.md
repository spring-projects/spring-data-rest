# Handling ApplicationEvents in the REST Exporter

There are six different events that the REST exporter emits throughout the process of working with an entity. Those are:

* BeforeSaveEvent
* AfterSaveEvent
* BeforeLinkSaveEvent
* AfterLinkSaveEvent
* BeforeDeleteEvent
* AfterDeleteEvent

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
