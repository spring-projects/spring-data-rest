# Spring Data JPA Repository Web Exporter

The Spring Data JPA Repository Web Exporter allows you to export your [JPA Repositories](http://static.springsource.org/spring-data/data-jpa/docs/current/reference/html/#jpa.repositories) as a RESTful web application. The exporter exposes the CRUD methods of a [CrudRepository](http://static.springsource.org/spring-data/data-commons/docs/1.1.0.RELEASE/api/org/springframework/data/repository/CrudRepository.html) for doing basic entity management. Relationships can also be managed between linked entities. The exporter is deployed as a traditional Spring MVC Controller, which means all the traditional Spring MVC tools are available to work with the Web Exporter (like Spring Security, for instance).

### Installation

#### Servlet environment

To use the Spring Data Web Exporter, you need to build a WAR file. Start by cloning the base web application project that contains the web.xml file you'll need to run the Web Exporter: [https://github.com/SpringSource/spring-data-rest-webmvc](https://github.com/SpringSource/spring-data-rest-webmvc).

    git clone https://github.com/SpringSource/spring-data-rest-webmvc.git
    cd spring-data-rest-webmvc
    ./gradlew war

Deploy the built WAR file to your servlet container:

    cp build/libs/spring-data-rest-webmvc-1.0.0.M1.war $TOMCAT_HOME/webapps/data.war
    cd $TOMCAT_HOME
    bin/catalina.sh run

You can also deploy to a Jetty web container embedded in the build:

    ./gradlew jettyRun

 The WAR file has a couple example domain classes and exposes a couple repositories by default. You can verify that this configuration is working by issuing an HTTP GET to the root of the web application:

    curl -v http://localhost:8080/data/

 In return, you should see:

     > GET /data/ HTTP/1.1
     > User-Agent: curl/7.19.7 (universal-apple-darwin10.0) libcurl/7.19.7 OpenSSL/0.9.8r zlib/1.2.3
     > Host: localhost:8080
     > Accept: */*
     >
     < HTTP/1.1 200 OK
     < Server: Apache-Coyote/1.1
     < Content-Type: application/json;charset=ISO-8859-1
     < Content-Language: en-US
     < Content-Length: 257
     < Date: Mon, 16 Apr 2012 14:32:44 GMT
     <
     {
       "_links" : [ {
         "rel" : "address",
         "href" : "http://localhost:8080/data/address"
       }, {
         "rel" : "person",
         "href" : "http://localhost:8080/data/person"
       }, {
         "rel" : "profile",
         "href" : "http://localhost:8080/data/profile"
       } ]
     }

### Export Repositories

To expose your Repositories to the exporter, include a Spring XML configuration file in the classpath (e.g. in a client JAR or in `WEB-INF/classes`). The filename should end with "-export.xml" and reside under the path `META-INF/spring-data-rest/`. Your configuration should include a properly-instaniated EntityManagerFactoryBean, an appropriate DataSource, and the appropriate repository configuration. It's easiest to use the special XML namespace for this purpose. An example configuration (named `WEB-INF/spring-data-rest/repositories-export.xml`) would look like something like this:

    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
           xmlns:jpa="http://www.springframework.org/schema/data/jpa"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://www.springframework.org/schema/beans 
           http://www.springframework.org/schema/beans/spring-beans.xsd
           http://www.springframework.org/schema/data/jpa 
           http://www.springframework.org/schema/data/jpa/spring-jpa.xsd">

      <import resource="shared.xml"/>

      <jpa:repositories base-package="com.mycompany.domain.repositories"/>

    </beans>

The file `shared.xml` contains a JDBC DataSource configuration, an EntityManagerFactoryBean, and a JpaTransactionManager.

### Including your domain artifacts

To expose your domain objects (your JPA entities, Repositories) and Spring configuration using the web exporter, you need to copy those resources to the web exporter's `WEB-INF/lib` or `WEB-INF/classes` directory. There are potentially other ways to deploy these artifacts without modifying the web exporter's WAR file, but those methods are considerably more complicated and prone to classpath problems. The easiest and most reliable way to deploy your user artifacts are by deploying them alongside the web exporter's artifacts.

### Exposing your repositories

By default, any repositories found are exported using the bean name of the repository in the Spring configuration (minus the word "Repository", if it appears in the bean name). 

If you have a JPA entity in your domain model that looks like...

    @Entity
    public class Person {
      @Id 
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

...your PersonRepository will by default be declared in the ApplicationContext with a bean name of "personRepository". The web exporter will strip the word "Repository" from it and expose a resource named "person". The resulting URL of this repository (assuming the exporter webapp is deployed at context path `/data` in your servlet container) will be `http://localhost:8080/data/person`.

You can configure under what path, or whether a resource is exported at all, by using the `@RestResource` annotation. Details are here: [Configuring the REST URL path](../wiki/Configuring-the-REST-URL-path)

### Discoverability

The Web Exporter implements some aspects of the [HATEOS](http://en.wikipedia.org/wiki/HATEOAS) methodology. That means all the services of the web exporter are discoverable and exposed to the client using links.

If you issue an HTTP request to the root of the exporter:

    curl -v http://localhost:8080/data/

You'll get back a chunk of JSON that points your user agent to the locations of the exposed repositories:

    {
      "_links" : [{
        "rel" : "person",
        "href" : "http://localhost:8080/data/person"
      }]
    }

The "rel" of the link will match the exposed name of the repository. Your application should keep track of this rel value as the key to this repository.

Similarly, if you issue a GET to `http://localhost:8080/data/person`, you should get back a list of entities exposed at this resource (as returned by the CrudRepository.findAll method). At the moment, there is no paging, sorting, or querying capability.

    curl -v http://localhost:8080/data/person
    
    {
      "_links" : [ {
        "rel" : "Person",
        "href" : "http://localhost:8080/data/person/1"
      }, {
        "rel" : "Person",
        "href" : "http://localhost:8080/data/person/2"
      } ]
    }

The "rel" of these links will be the simple class name of the entity managed by this repository.

Following these links will give your user agent a chunk of JSON that represents the entity. Besides properly handling nested objects and simple values, the web exporter will show relationships between entities using links just like those presented previously.

    curl -v http://localhost:8080/data/person/1
    
    {
      "name" : "John Doe",
      "_links" : [ {
        "rel" : "profiles",
        "href" : "http://localhost:8080/data/person/1/profiles"
      }, {
        "rel" : "addresses",
        "href" : "http://localhost:8080/data/person/1/addresses"
      }, {
        "rel" : "self",
        "href" : "http://localhost:8080/data/person/1"
      } ],
      "version" : 1
    }

This entity has a simple String value called "name", and two relationships to other entities ("profiles", and "addresses"). Note that the "rel" value of the link corresponds to the property name of the @Entity.

The "self" link will always point to the resource for this entity. Use the "self" link to access the entity itself if you wish to update or delete the entity.

Following the links for the "profiles" property gives us a list of links to the actual entities that are referenced by this relationship:

    curl -v http://localhost:8080/data/person/1/profiles
    
    {
      "profiles" : [ {
        "rel" : "twitter",
        "href" : "http://localhost:8080/data/person/1/profiles/1"
      }, {
        "rel" : "facebook",
        "href" : "http://localhost:8080/data/person/1/profiles/2"
      } ]
    }

In this case, the "profiles" property is a Map, so the "rel" value of the links is the key in the Map. The resource link, however, does not use the Map key in the URL. It is consistent with all other links to child resources and uses the ID of the child entity as the last component of the URL.

Retrieving the linked entity gives us a JSON representation of the entity, as well as the "self" link necessary to update and delete the entity.

    curl -v http://localhost:8080/data/person/1/profiles/1
    
    {
      "_links" : [ {
        "rel" : "self",
        "href" : "http://localhost:8080/data/profile/1"
      } ],
      "type" : "twitter",
      "url" : "#!/johndoe"
    }

### Updating relationships

To maintain a relationship between two entities, access the resource of the relationship by using the id of the entity as the last element in the resource path. For example, to add a link to a Profile with id 3 to a Person with id 1, issue a POST to the "profiles" resource and include in the body of the request a list of resource paths to entities you want to link to (make sure to use the [special Content-Type "text/uri-list"](http://www.ietf.org/rfc/rfc2483.txt) which, as the name implies, is a representation of a list of URIs):

    curl -v -X POST -H "Content-Type: text/uri-list" -d "http://localhost:8080/data/profile/3" http://localhost:8080/data/person/1/profiles

You can also delete a relationship by issuing a DELETE request to the resource path that represents the relationship between parent and child entities. For example, to delete a relationship between a Profile entity with an id of 2 and a Person with an id of 1:

    curl -v -X DELETE http://localhost:8080/data/person/1/profiles/2

### Handling events

