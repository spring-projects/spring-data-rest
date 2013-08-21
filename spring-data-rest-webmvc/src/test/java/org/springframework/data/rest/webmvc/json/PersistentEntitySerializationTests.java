package org.springframework.data.rest.webmvc.json;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.jpa.Person;
import org.springframework.data.rest.webmvc.jpa.PersonRepository;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkDiscoverer;
import org.springframework.hateoas.core.DefaultLinkDiscoverer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Jon Brisbin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RepositoryTestsConfig.class)
@Transactional
public class PersistentEntitySerializationTests {

	private static final String PERSON_JSON_IN = "{\"firstName\": \"John\",\"lastName\": \"Doe\"}";

	@Autowired ObjectMapper mapper;
	@Autowired Repositories repositories;
	@Autowired PersonRepository people;

	LinkDiscoverer linkDiscoverer;

	@Before
	public void setUp() {
		linkDiscoverer = new DefaultLinkDiscoverer();
	}

	@Test
	public void deserializesPersonEntity() throws IOException {

		Person p = mapper.readValue(PERSON_JSON_IN, Person.class);

		assertThat(p.getFirstName(), is("John"));
		assertThat(p.getLastName(), is("Doe"));
		assertThat(p.getSiblings(), is(Collections.EMPTY_LIST));
	}

	@Test
	public void serializesPersonEntity() throws IOException, InterruptedException {

		PersistentEntity<?, ?> persistentEntity = repositories.getPersistentEntity(Person.class);
		Person person = people.save(new Person("John", "Doe"));

		StringWriter writer = new StringWriter();
		mapper.writeValue(writer, PersistentEntityResource.wrap(persistentEntity, person));

		String s = writer.toString();

		Link fatherLink = linkDiscoverer.findLinkWithRel("father", s);
		assertThat(fatherLink.getHref(), endsWith(new UriTemplate("/{id}/father").expand(person.getId()).toString()));

		Link siblingLink = linkDiscoverer.findLinkWithRel("siblings", s);
		assertThat(siblingLink.getHref(), endsWith(new UriTemplate("/{id}/siblings").expand(person.getId()).toString()));
	}
}
