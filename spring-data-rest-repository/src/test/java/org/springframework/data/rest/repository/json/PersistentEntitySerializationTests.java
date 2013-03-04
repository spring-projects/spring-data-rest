package org.springframework.data.rest.repository.json;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.repository.PersistentEntityResource;
import org.springframework.data.rest.repository.RepositoryTestsConfig;
import org.springframework.data.rest.repository.domain.jpa.Person;
import org.springframework.data.rest.repository.domain.jpa.PersonRepository;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkDiscoverer;
import org.springframework.hateoas.core.DefaultLinkDiscoverer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Jon Brisbin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RepositoryTestsConfig.class)
public class PersistentEntitySerializationTests {

	private static final String  PERSON_JSON_IN  = "{\"firstName\": \"John\",\"lastName\": \"Doe\"}";
	private static final Pattern PERSON_JSON_OUT = Pattern.compile(
			"\\{\"lastName\":\"Doe\",\"created\":([0-9]+),\"firstName\":\"John\",\"links\":\\[\\{\"rel\":\"people.person.siblings\",\"href\":\"http://localhost/people/2/siblings\"}]}");
	@Autowired
	private ObjectMapper     mapper;
	@Autowired
	private Repositories     repositories;
	@Autowired
	private PersonRepository people;
	private LinkDiscoverer links = new DefaultLinkDiscoverer();

	public static Matcher<Link> isLinkWithHref(final String href) {
		return new BaseMatcher<Link>() {
			@Override public boolean matches(Object item) {
				return (item instanceof Link && href.equals(((Link)item).getHref()));
			}

			@Override public void describeTo(Description description) {
				description.appendText(href);
			}
		};
	}

	@Test
	public void deserializesPersonEntity() throws IOException {
		Person p = mapper.readValue(PERSON_JSON_IN, Person.class);
		assertThat(p.getFirstName(), is("John"));
		assertThat(p.getLastName(), is("Doe"));
		assertThat(p.getSiblings(), is(Collections.EMPTY_LIST));
	}

	@Test
	@Ignore
	public void serializesPersonEntity() throws IOException, InterruptedException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		mapper.writeValue(out, PersistentEntityResource.wrap(repositories.getPersistentEntity(Person.class),
		                                                     people.save(new Person("John", "Doe")),
		                                                     URI.create("http://localhost")));
		out.flush();
		String s = new String(out.toByteArray());

		assertThat("Siblings Link looks correct",
		           JsonPath.read(s, "$links[0].href").toString(),
		           endsWith("/2/siblings"));
	}

}
