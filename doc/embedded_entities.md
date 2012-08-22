# Embedded Entity references in complex object graphs

Sometimes it's necessary to populate the incoming JSON with references to pre-existing @Entity objects. Often, this is because of referential integrity constraints. Consider the following relationship between two entities:

		@Entity
		public class Person {
			// ... person's properties
		}

		@Entity
		public class Address {

			@OneToOne(optional = false)
			private Person person;

		}

Because of the `optional = false` on the `@OneToOne` annotation, I have to include a reference to an existing `Person` entity if I want to create a new `Address`.

If you pull up the list of `Person` links using your user agent (Javascript, for instance), you'll want to save the link object that refers to the `Person` instance you're interested in. For example, if I list the `Person`s in the database and use the compact JSON format:

		curl -v -H "Accept: application/x-spring-data-compact+json" http://localhost:8080/people

I'll get back the link objects I need to reference this entity again (this link is the same as the "self" link that appears in other places):

		{
			"links" : [ {
				"rel" : "people.Person",
				"href" : "http://localhost:8080/people/2"
			}, {
				"rel" : "people.Person",
				"href" : "http://localhost:8080/people/1"
			}, {
				"rel" : "people.search",
				"href" : "http://localhost:8080/people/search"
			} ],
			"content" : [ ],
			"page" : {
				"number" : 1,
				"size" : 20,
				"totalPages" : 1,
				"totalElements" : 2
			}
		}

If you present the user with, for example, a drop-down combo box of the `people.Person` type links, and keep track of their selection, then you could just include that object in a new JSON object something like the following:

		{
			"postalCode": "12345",
			"province": "MO",
			"lines": ["1 W 1st St."],
			"city": "Univille",
			"person": {
				"rel" : "people.Person",
				"href" : "http://localhost:8080/people/1"
			}
		}

You'll remember from the entity definition that the relationship between `Address` and `Person` is not optional. In that case, you'd simply include a JSON link object that refers to the entity you're interested in wherever that entity is supposed to appear. In this case, as the value of the "person" property. You can now POST this JSON to the server to create a new `Address` instance, with the "person" properly populated:

		curl -v -X POST -d '...json data...' http://localhost:8080/address

