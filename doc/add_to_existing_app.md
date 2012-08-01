# Adding Spring Data REST to an existing Spring MVC Application

If you have an existing Spring MVC application and you'd like to integrate Spring Data REST, it's actually very easy.

Somewhere in your Spring MVC configuration (most likely where you configure your MVC resources) add a bean reference to the JavaConfig class that is responsible for configuring the `RepositoryRestController`. The class name is `org.springframework.data.rest.webmvc.RepositoryRestMvcConfiguration`. In XML this would look like:

    <bean class="org.springframework.data.rest.webmvc.RepositoryRestMvcConfiguration"/>

When your ApplicationContext comes across this bean definition it will bootstrap the necessary Spring MVC resources to fully-configure the controller for exporting the Repositories it finds in that ApplicationContext and any parent contexts.

### More on required configuration

There are a couple Spring MVC resources that Spring Data REST depends on that must be configured correctly for it to work inside an existing Spring MVC application. We've tried to isolate those resources from whatever similar resources already exist within your application, but it may be that you want to customize some of the behavior of Spring Data REST by modifying these MVC components.

The most important things that we configure especially for use by Spring Data REST include:

#### RepositoryRestHandlerMapping

We register a custom `HandlerMapping` instance that responds only to the `RepositoryRestController` and only if a path is meant to be handled by Spring Data REST. In order to keep paths that are meant to be handled by your application separate from those handled by Spring Data REST, this custom HandlerMapping inspects the URL path and checks to see if a Repository has been exported under that name. If it has, it allows the request to be handled by Spring Data REST. If there is no Repository exported under that name, it returns `null`, which just means "let other HandlerMapping instances try to service this request".

Basically this means that Spring Data REST will always be first in line when it comes time to map a URL path and your existing application will never get a chance to service a request that is meant for a Repository. For example, if you have a Repository exported under the name "person", then all requests to your application that start with "/person" will be handled by Spring Data REST and your application will never see that request. If your Repository is exported under a different name, however (like "people"), then requests to "/people" will go to Spring Data REST and requests to "/person" will be handled by your application.