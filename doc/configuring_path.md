# Configuring the REST URL path

Configuring the segments of the URL path under which the resources of a JPA Repository are exported is simple. You just add an annotation at the class level and/or at the query method level.

By default, the exporter will expose your CrudRepository using the class name stripped of the word "Repository". So a Repository defined as follows:

    public interface PersonRepository extends CrudRepository<Person, Long> {}

Will, by default, be exposed under the URL:

    http://localhost:8080/person/

To change how the Repository is exported, add a `@RestResource` annotation at the class level:

    @RestResource(path = "people")
    public interface PersonRepository extends CrudRepository<Person, Long> {}

Now the Repository will be accessible under the URL:

    http://localhost:8080/people/

If you have query methods defined, those also default to be exposed by their name:

    public interface PersonRepository extends CrudRepository<Person, Long> {

      public List<Person> findByName(String name);

    }

This would be exposed under the URL:

    http://localhost:8080/person/search/findByName

_NOTE: All query method resources are exposed under the resource `search`._

To change the segment of the URL under which this query method is exposed, use the `@RestResource` annotation again:

    @RestResource(path = "people")
    public interface PersonRepository extends CrudRepository<Person, Long> {

      @RestResource(path = "names")
      public List<Person> findByName(String name);

    }

Now this query method will be exposed under the URL:

    http://localhost:8080/people/search/names

### Handling rels

Since these resources are all discoverable, you can also affect how the "rel" attribute is displayed in the links sent out by the exporter.

For instance, in the default configuration, if you issue a request to `http://localhost:8080/person/search` to find out what query methods are exposed, you'll get back a list of links:

    {
      "_links" : [ {
        "rel" : "person.findByName",
        "href" : "http://localhost:8080/person/search/findByName"
      } ]
    }

To change the rel value, use the `rel` property on the `@RestResource` annotation:

    @RestResource(path = "people")
    public interface PersonRepository extends CrudRepository<Person, Long> {

      @RestResource(path = "names", rel = "names")
      public List<Person> findByName(String name);

    }

This would result in a link value of:

    {
      "_links" : [ {
        "rel" : "person.names",
        "href" : "http://localhost:8080/people/search/names"
      } ]
    }

The Repository's rel can also be changed by using the `@RestResource` property:

    @RestResource(path = "people", rel = "people")
    public interface PersonRepository extends CrudRepository<Person, Long> {

      @RestResource(path = "names", rel = "names")
      public List<Person> findByName(String name);

    }

This would result in a link value of:

    {
      "_links" : [ {
        "rel" : "people.names",
        "href" : "http://localhost:8080/people/search/names"
      } ]
    }

