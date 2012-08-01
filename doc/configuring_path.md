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

### Hiding certain Repositories, query methods, or fields

You may not want a certain Repository, a query method on a Repository, or a field of your entity to be exported at all. To tell the exporter to not export these items, annotate them with `@RestResource` and set `exported = false`.

For example, to skip exporting a Repository:

    @RestResource(exported = false)
    public interface PersonRepository extends CrudRepository<Person, Long> {
    }

To skip exporting a query method:

    @RestResource(path = "people", rel = "people")
    public interface PersonRepository extends CrudRepository<Person, Long> {

      @RestResource(exported = false)
      public List<Person> findByName(String name);

    }

Or to skip exporting a field:

    @Entity
    public class Person {
      @Id @GeneratedValue private Long id;
      @OneToMany
      @RestResource(exported = false)
      private Map<String, Profile> profiles;
    }

### Hiding Repository CRUD methods

If you don't want to expose a save or delete method on your `CrudRepository`, you can use the `@RestResource(exported = false)` setting by overriding the method you want to turn off and placing the annotation on the overriden version. For example, to prevent HTTP users from invoking the delete methods of `CrudRepository`, override all of them and add the annotation to the overriden methods.

    @RestResource(path = "people", rel = "people")
    public interface PersonRepository extends CrudRepository<Person, Long> {

			@Override
			@RestResource(exported = false)
			void delete(Long id);

			@Override
			@RestResource(exported = false)
			void delete(Person entity);

    }

NOTE: It is important that you override _both_ delete methods as the exporter currently uses a somewhat naive algorithm for determing which CRUD method to use in the interest of faster runtime performance. It's not currently possible to turn off the version of delete which takes an ID but leave exported the version that takes an entity instance. For the time being, you can either export the delete methods or not. If you want turn them off, then just keep in mind you have to annotate both versions with `exported = false`.