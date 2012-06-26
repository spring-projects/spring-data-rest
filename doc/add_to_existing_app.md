# Adding Spring Data REST to an existing Spring MVC Application

If you have an existing Spring MVC application and you'd like to integrate Spring Data REST, it's actually very easy.

Somewhere in your Spring MVC configuration (most likely where you configure your MVC resources) add a bean reference to the JavaConfig class that is responsible for configuring the `RepositoryRestController`. The class name is `org.springframework.data.rest.webmvc.RepositoryRestMvcConfiguration`. In XML this would look like:

    <bean class="org.springframework.data.rest.webmvc.RepositoryRestMvcConfiguration"/>

When your ApplicationContext comes across this bean definition it will bootstrap the necessary Spring MVC resources to fully-configure the controller for exporting the Repositories it finds in that ApplicationContext and any parent contexts.

### More on required configuration

There are a couple Spring MVC resources that Spring Data REST depends on that must be configured correctly for it to work inside an existing Spring MVC application. We've tried to isolate those resources from whatever similar resources already exist within your application, but it may be that you want to customize some of the behavior of Spring Data REST by modifying these MVC components.

The most important things that we configure especially for use by Spring Data REST include:

#### View Resolvers

We register a `ContentNegotiatingViewResolver` that will order itself in the highest priority, which means it will attempt to override any other `ViewResolver`s you already have configured. Currently there are two views registered with this view resolver: a special `JsonView` and a `UrilistView` (for rendering `text/uri-list` resources which are used for links as an alternative to JSON). These views will only respond to special view names returned by the `RepositoryRestController`. These view names begin with "org.springframework.data.rest" as follows:

 * `org.springframework.data.rest.list_links` - For listing repositories registered and exported.
 * `org.springframework.data.rest.list_entities` - List entities (not a query but uses the `findAll` Repository method).
 * `org.springframework.data.rest.list_queries` - List query methods found and exported on a Repository.
 * `org.springframework.data.rest.query_results` - Results of calling a query method.
 * `org.springframework.data.rest.after_create` - Used after an entity is created.
 * `org.springframework.data.rest.empty` - Used whenever an empty response is sent back.
 * `org.springframework.data.rest.entity` - Renders an entity.
 * `org.springframework.data.rest.entity_property` - Renders the property of an entity.
 * `org.springframework.data.rest.linked_entity` - Renders a linked property of an entity.

To register your own custom view for any of these internal view names, you need to subclass `RepositoryRestMvcConfiguration.contentNegotiatingViewResolver()` and mimic the functionality you find [in the source code](https://github.com/SpringSource/spring-data-rest/blob/master/spring-data-rest-webmvc/src/main/java/org/springframework/data/rest/webmvc/RepositoryRestMvcConfiguration.java#L59). You do not need to completely override all these views. You simply register custom views on the `RepositoryRestViewResolver` for either the JSON or uri-list views by setting the `customViewMappings` property with a `Map<String, View>` that overrides one or more of the above default views.

#### RepositoryRestHandlerMapping

We register a custom `HandlerMapping` instance that responds only to the `RepositoryRestController` and only if a path is meant to be handled by Spring Data REST. In order to keep paths that are meant to be handled by your application separate from those handled by Spring Data REST, this custom HandlerMapping inspects the URL path and checks to see if a Repository has been exported under that name. If it has, it allows the request to be handled by Spring Data REST. If there is no Repository exported under that name, it returns `null`, which just means "let other HandlerMapping instances try to service this request".

Basically this means that Spring Data REST will always be first in line when it comes time to map a URL path and your existing application will never get a chance to service a request that is meant for a Repository. For example, if you have a Repository exported under the name "person", then all requests to your application that start with "/person" will be handled by Spring Data REST and your application will never see that request. If your Repository is exported under a different name, however (like "people"), then requests to "/people" will go to Spring Data REST and requests to "/person" will be handled by your application.