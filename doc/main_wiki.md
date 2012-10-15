# Spring Data JPA Repository Web Exporter

The Spring Data JPA Repository Web Exporter allows you to export your [JPA Repositories](http://static.springsource.org/spring-data/data-jpa/docs/current/reference/html/#jpa.repositories) as a RESTful web application. The exporter exposes the CRUD methods of a [CrudRepository](http://static.springsource.org/spring-data/data-commons/docs/current/api/org/springframework/data/repository/CrudRepository.html) for doing basic entity management. Relationships can also be managed between linked entities. The exporter is deployed as a traditional Spring MVC Controller, which means all the traditional Spring MVC tools are available to work with the Web Exporter (like Spring Security, for instance).

### Installation

#### Servlet environment

Deployment of the Spring Data Web Exporter is extremely flexible. You can build a WAR file for deploying in a Servlet 2.5 or Servlet 3.0 environment. You can drop the spring-data-rest-webmvc.war artifact into an existing Servlet 3.0 application.

Start by cloning the base web application project: [https://github.com/SpringSource/spring-data-rest-webmvc](https://github.com/SpringSource/spring-data-rest-webmvc). This sample application contains a `web.xml` file in `src/main/webapp/WEB-INF/servlet-2.5-web.xml` for deployment to pre-servlet-3 containers. The prefered way to configure the exporter, though, is using the XML-free Servlet 3.0 version. Tomcat 7 and Jetty 8 both support deploying this project directly.

    git clone https://github.com/SpringSource/spring-data-rest-webmvc.git
    cd spring-data-rest-webmvc
    ./gradlew war

Deploy the built WAR file to your servlet container:

    cp build/libs/spring-data-rest-webmvc-1.0.0.RELEASE.war $TOMCAT_HOME/webapps/data.war
    cd $TOMCAT_HOME
    bin/catalina.sh run

You can also run the project directly a Tomcat web container embedded in the build:

    ./gradlew tomcatRun

 The WAR file has a couple example domain classes and exposes a couple repositories by default. You can verify that this configuration is working by issuing an HTTP GET to the root of the web application:

    curl -v http://localhost:8080/spring-data-rest-webmvc/

 In return, you should see:

     > GET /data/ HTTP/1.1
     > User-Agent: curl/7.19.7 (universal-apple-darwin10.0) libcurl/7.19.7 OpenSSL/0.9.8r zlib/1.2.3
     > Host: localhost:8080
     > Accept: */*
     >
     < HTTP/1.1 200 OK
     < Content-Type: application/json;charset=ISO-8859-1
     < Content-Length: 257
     <
     {
       "links" : [ {
         "rel" : "address",
         "href" : "http://localhost:8080/spring-data-rest-webmvc/address"
       }, {
         "rel" : "person",
         "href" : "http://localhost:8080/spring-data-rest-webmvc/person"
       }, {
         "rel" : "profile",
         "href" : "http://localhost:8080/spring-data-rest-webmvc/profile"
       } ]
     }

### Export Repositories

The preferred method to configure the Spring Data REST Exporter is to use the JavaConfig annotations. There is an example ApplicationConfig in the example application you can follow. You want to make sure the configuration class with the `@EnableJpaRepositores` annotation on it is loaded by the servlet context loader. Either use the special `RepositoryRestExporterServlet` or a `DispatcherServlet` the the appropriate `contextConfigLocation` set (refer to the `RepositoryRestExporterServlet` for more information).

		@Configuration
		@ComponentScan(basePackageClasses = ApplicationConfig.class)
		@EnableJpaRepositories
		@EnableTransactionManagement
		public class ApplicationConfig {

			@Bean public DataSource dataSource() {
				EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
				return builder.setType(EmbeddedDatabaseType.HSQL).build();
			}

			@Bean public EntityManagerFactory entityManagerFactory() {
				HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
				vendorAdapter.setDatabase(Database.HSQL);
				vendorAdapter.setGenerateDdl(true);

				LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
				factory.setJpaVendorAdapter(vendorAdapter);
				factory.setPackagesToScan(getClass().getPackage().getName());
				factory.setDataSource(dataSource());

				factory.afterPropertiesSet();

				return factory.getObject();
			}

			@Bean public JpaDialect jpaDialect() {
				return new HibernateJpaDialect();
			}

			@Bean public PlatformTransactionManager transactionManager() {
				JpaTransactionManager txManager = new JpaTransactionManager();
				txManager.setEntityManagerFactory(entityManagerFactory());
				return txManager;
			}

		}

Your `WebApplicationInitializer` class would look like this:

		public class RestExporterWebInitializer implements WebApplicationInitializer {

			@Override public void onStartup(ServletContext ctx) throws ServletException {

				AnnotationConfigWebApplicationContext rootCtx = new AnnotationConfigWebApplicationContext();
				rootCtx.register(ApplicationConfig.class);

				ctx.addListener(new ContextLoaderListener(rootCtx));

				RepositoryRestExporterServlet exporter = new RepositoryRestExporterServlet();

				ServletRegistration.Dynamic reg = ctx.addServlet("rest-exporter", exporter);
				reg.setLoadOnStartup(1);
				reg.addMapping("/*");

			}

		}

The REST exporter will also load any XML config files it finds under the path `META-INF/spring-data-rest/*-export.xml`. If you have XML configuration (Spring Integration configuration, for example), then just put your XML files in this location and they will also be bootstrapped in the ApplicationContext.

    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://www.springframework.org/schema/beans 
           http://www.springframework.org/schema/beans/spring-beans.xsd
           http://www.springframework.org/schema/data/jpa 
           http://www.springframework.org/schema/data/jpa/spring-jpa.xsd">

      <import resource="shared.xml"/>

		  <bean id="beforeSavePersonValidator" class="org.springframework.data.rest.example.PersonValidator"/>

    </beans>

### Including your domain artifacts

To expose your domain objects (your JPA entities, Repositories) and Spring configuration using the web exporter, you need to copy those resources to the web exporter's `WEB-INF/lib` or `WEB-INF/classes` directory. There are potentially other ways to deploy these artifacts without modifying the web exporter's WAR file, but those methods are considerably more complicated and prone to classpath problems. The easiest and most reliable way to deploy your user artifacts are by deploying them alongside the web exporter's artifacts.

### Exposing your repositories

By default, any repositories found are exported using the bean name of the repository in the Spring configuration (minus the word "Repository", if it appears in the bean name). 

If you have a JPA entity in your domain model that looks like...

    @Entity
    public class Person {
      @Id @GeneratedValue
      private Long id;
      private String name;
      @Version 
      private Long version;
      @OneToMany 
      private List<Address> addresses;
      @OneToMany 
      private Map<String, Profile> profiles;
    }

...and an appropriate CrudRepository interface defined like...

    public interface PersonRepository extends CrudRepository<Person, Long> {
    }

...your PersonRepository will by default be declared in the ApplicationContext with a bean name of "personRepository". The web exporter will strip the word "Repository" from it and expose a resource named "person". The resulting URL of this repository (assuming the exporter webapp is deployed at context path `/data` in your servlet container) will be `http://localhost:8080/spring-data-rest-webmvc/person`.

You can configure under what path, or whether a resource is exported at all, by using the `@RestResource` annotation. Details are here: [Configuring the REST URL path](../wiki/Configuring-the-REST-URL-path)

### Using the rest-shell

There is a command-line utility to make REST interaction easier. It includes history support and has helper commands to reduce the amount of typing you need to do to effect interactions with your REST services. It's called the `rest-shell`. You can download the binary package or the source from GitHub here: [https://github.com/jbrisbin/rest-shell](https://github.com/jbrisbin/rest-shell).

### Discoverability

The Web Exporter implements some aspects of the [HATEOAS](http://en.wikipedia.org/wiki/HATEOAS) methodology. That means all the services of the web exporter are discoverable and exposed to the client using links.

If you issue an HTTP request to the root of the exporter:

    curl -v http://localhost:8080/spring-data-rest-webmvc/

You'll get back a chunk of JSON that points your user agent to the locations of the exported repositories:

    {
      "links" : [{
        "rel" : "person",
        "href" : "http://localhost:8080/spring-data-rest-webmvc/person"
      }]
    }

The "rel" of the link will match the exported name of the repository. Your application should keep track of this rel value as the key to this repository.

Similarly, if you issue a GET to `http://localhost:8080/spring-data-rest-webmvc/person`, you should get back a list of entities exported at this resource (as returned by the CrudRepository.findAll method). See the wiki for more information about the paging and sorting options.

    curl -v http://localhost:8080/spring-data-rest-webmvc/person
    
    {
      "content": [ ],
      "links" : [ {
        "rel" : "person.search",
        "href" : "http://localhost:8080/spring-data-rest-webmvc/person/search"
      } ]
    }

The default "rel" of these links will be the rel of the repository plus a dot '.' plus the simple class name of the entity managed by this repository. The rel value can be configured using the `@RestResource` annotation, discussed on [Configuring the REST URL path](../wiki/Configuring-the-REST-URL-path).

Following these links will give your user agent a chunk of JSON that represents the entity. Besides properly handling nested objects and simple values, the web exporter will show relationships between entities using links just like those presented previously.

    curl -v http://localhost:8080/spring-data-rest-webmvc/person/1
    
    {
      "name" : "John Doe",
      "links" : [ {
        "rel" : "profiles",
        "href" : "http://localhost:8080/spring-data-rest-webmvc/person/1/profiles"
      }, {
        "rel" : "addresses",
        "href" : "http://localhost:8080/spring-data-rest-webmvc/person/1/addresses"
      }, {
        "rel" : "self",
        "href" : "http://localhost:8080/spring-data-rest-webmvc/person/1"
      } ],
      "version" : 1
    }

This entity has a simple String value called "name", and two relationships to other entities ("profiles", and "addresses"). Note that the "rel" value of the link corresponds to the property name of the @Entity.

The "self" link will always point to the resource for this entity. Use the "self" link to access the entity itself if you wish to update or delete the entity.

Following the links for the "profiles" property gives us a list of links to the actual entities that are referenced by this relationship:

    curl -v http://localhost:8080/spring-data-rest-webmvc/person/1/profiles
    
    {
      "profiles" : [ {
        "rel" : "twitter",
        "href" : "http://localhost:8080/spring-data-rest-webmvc/person/1/profiles/1"
      }, {
        "rel" : "facebook",
        "href" : "http://localhost:8080/spring-data-rest-webmvc/person/1/profiles/2"
      } ]
    }

In this case, the "profiles" property is a Map, so the "rel" value of the links is the key in the Map. The resource link, however, does not use the Map key in the URL. It is consistent with all other links to child resources and uses the ID of the child entity as the last component of the URL.

Retrieving the linked entity gives us a JSON representation of the entity, as well as the "self" link necessary to update and delete the entity.

    curl -v http://localhost:8080/spring-data-rest-webmvc/person/1/profiles/1
    
    {
      "links" : [ {
        "rel" : "self",
        "href" : "http://localhost:8080/spring-data-rest-webmvc/profile/1"
      } ],
      "type" : "twitter",
      "url" : "#!/johndoe"
    }

### Updating relationships

To maintain a relationship between two entities, access the resource of the relationship by using the id of the entity as the last element in the resource path. For example, to add a link to a Profile with id 3 to a Person with id 1, issue a POST to the "profiles" resource and include in the body of the request a list of resource paths to entities you want to link to (make sure to use the [special Content-Type "text/uri-list"](http://www.ietf.org/rfc/rfc2483.txt) which, as the name implies, is a representation of a list of URIs):

    curl -v -X POST -H "Content-Type: text/uri-list" -d "http://localhost:8080/spring-data-rest-webmvc/profile/3" http://localhost:8080/spring-data-rest-webmvc/person/1/profiles

You can also delete a relationship by issuing a DELETE request to the resource path that represents the relationship between parent and child entities. For example, to delete a relationship between a Profile entity with an id of 2 and a Person with an id of 1:

    curl -v -X DELETE http://localhost:8080/spring-data-rest-webmvc/person/1/profiles/2

### Calling Query methods

Starting with Spring Data REST 1.0.0.M2, the exporter exposes Repository query methods under the special URL path `/repository/search/*`.

To see what query methods are exported, issue a GET request to the entity resource URL and add the segment "search". You'll get back a list of links to the exported search methods.

    curl -v http://localhost:8080/spring-data-rest-webmvc/person/search

    {
      "links" : [ {
        "rel" : "person.findByName",
        "href" : "http://localhost:8080/spring-data-rest-webmvc/person/search/findByName"
      } ]
    }

To query for entities using this search method, add a query parameter to the URL. The response will be a list of links to the top-level URL for that resource.

    curl -v http://localhost:8080/spring-data-rest-webmvc/person/search/findByName?name=John+Doe

    [ {
      "rel" : "person.Person",
      "href" : "http://localhost:8080/spring-data-rest-webmvc/person/1"
    } ]

To change the URL under which the query method is exported or set the name of the query parameter containing the search term, use the `@RestResource` annotation.

    @RestResource(path = "people")
    public interface PersonRepository extends CrudRepository<Person, Long> {

      @RestResource(path = "name", rel = "names")
      public List<Person> findByName(@Param("name") String name);

    }

This changes the path the PersonRepository is exported under to `/people`, changes the rel of the search URL to `people.names`, changes the path under with the query method is exported to `/name`, and sets the query parameter containing the search term to `name`. To search the Repository using this method, issue a GET request.

    curl -v http://localhost:8080/spring-data-rest-webmvc/people/search/name?name=John+Doe