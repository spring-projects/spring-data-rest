/*
 * Copyright 2014-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.webmvc.json.patch;

import static org.assertj.core.api.Assertions.*;

import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.webmvc.json.BindContextFactory;
import org.springframework.data.rest.webmvc.json.PersistentEntitiesBindContextFactory;
import org.springframework.data.rest.webmvc.json.patch.SpelPath.UntypedSpelPath;
import org.springframework.data.rest.webmvc.json.patch.SpelPath.WritingOperations;
import org.springframework.format.support.DefaultFormattingConversionService;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Unit tests for {@link SpelPath}.
 *
 * @author Oliver Gierke
 * @author Greg Turnquist
 */
class SpelPathUnitTests {

	BindContext context;

	@BeforeEach
	void setUp() {

		KeyValueMappingContext<?, ?> context = new KeyValueMappingContext<>();
		context.getPersistentEntity(MapWrapper.class);
		context.getPersistentEntity(Todo.class);
		context.getPersistentEntity(Person.class);

		PersistentEntities entities = new PersistentEntities(Arrays.asList(context));
		BindContextFactory factory = new PersistentEntitiesBindContextFactory(entities,
				new DefaultFormattingConversionService());

		this.context = factory.getBindContextFor(new ObjectMapper());
	}

	@Test
	void listIndex() {

		UntypedSpelPath expr = SpelPath.untyped("/1/description");

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		Object value = expr.bindForRead(Todo.class, context).getValue(todos);

		assertThat(value).isEqualTo("B");
	}

	@Test
	void accessesLastCollectionElementWithDash() {

		UntypedSpelPath expr = SpelPath.untyped("/-/description");

		List<Todo> todos = new ArrayList<Todo>();
		todos.add(new Todo(1L, "A", false));
		todos.add(new Todo(2L, "B", false));
		todos.add(new Todo(3L, "C", false));

		Object value = expr.bindForRead(Todo.class, context).getValue(todos);

		assertThat(value).isEqualTo("C");
	}

	@Test // DATAREST-1152
	void cachesSpelPath() {

		UntypedSpelPath left = SpelPath.untyped("/description");
		UntypedSpelPath right = SpelPath.untyped("/description");

		assertThat(left).isSameAs(right);
	}

	@Test // DATAREST-1152
	void cachesTypedSpelPath() {

		UntypedSpelPath source = SpelPath.untyped("/description");
		WritingOperations left = source.bindForWrite(Todo.class, context);
		WritingOperations right = source.bindForWrite(Todo.class, context);

		assertThat(left).isSameAs(right);
	}

	@Test // DATAREST-1274
	void supportsMultiDigitCollectionIndex() {
		assertThat(SpelPath.untyped("/11/description").bindForWrite(Todo.class, context).getLeafType())
				.isEqualTo(String.class);
	}

	@Test // DATAREST-1338
	void handlesStringMapKeysInPathExpressions() {

		WritingOperations path = SpelPath.untyped("people/Dave/name").bindForWrite(MapWrapper.class, context);

		assertThat(path.getExpressionString()).isEqualTo("people['Dave'].name");
		assertThat(path.getLeafType()).isEqualTo(String.class);
	}

	@Test // DATAREST-1338
	void handlesIntegerMapKeysInPathExpressions() {

		WritingOperations path = SpelPath.untyped("peopleByInt/0/name").bindForWrite(MapWrapper.class, context);

		assertThat(path.getExpressionString()).isEqualTo("peopleByInt[0].name");
		assertThat(path.getLeafType()).isEqualTo(String.class);
	}

	@Test
	void failsAccessingPropertyIgnoredByJackson() {

		String path = "peopleByInt/0/hiddenProperty";

		assertThatExceptionOfType(PatchException.class) //
				.isThrownBy(() -> SpelPath.untyped(path).bindForWrite(MapWrapper.class, context)) //
				.withMessageContaining("hiddenProperty") //
				.withMessageContaining(Person.class.getName()) //
				.withMessageContaining(path); //
	}

	@Test
	void failsAccessingGetterIgnoredByJackson() {

		String path = "peopleByInt/0/hiddenGetter";

		assertThatExceptionOfType(PatchException.class) //
				.isThrownBy(() -> SpelPath.untyped(path).bindForWrite(MapWrapper.class, context)) //
				.withMessageContaining("hiddenGetter") //
				.withMessageContaining(Person.class.getName()) //
				.withMessageContaining(path); //
	}

	@Test
	void mapsRenamedProperty() {

		WritingOperations path = SpelPath.untyped("demaner").bindForWrite(Person.class, context);

		assertThat(path.getExpressionString()).isEqualTo("renamed");
	}

	@Test // #2233
	void bindsDatesProperly() {

		Person person = new Person();

		SpelPath.untyped("/birthday")
				.bindForWrite(Person.class, context)
				.setValue(person, "2000-01-01");

		assertThat(person.birthday).isEqualTo(LocalDate.of(2000, 1, 1));
	}

	// DATAREST-1338

	static class Person {
		String name;
		@JsonIgnore String hiddenProperty;
		String hiddenGetter;
		@JsonProperty("demaner") String renamed;
		LocalDate birthday;

		public String getName() {
			return this.name;
		}

		public String getHiddenProperty() {
			return this.hiddenProperty;
		}

		public String getRenamed() {
			return this.renamed;
		}

		public LocalDate getBirthday() {
			return this.birthday;
		}

		public void setName(String name) {
			this.name = name;
		}

		@JsonIgnore
		public void setHiddenProperty(String hiddenProperty) {
			this.hiddenProperty = hiddenProperty;
		}

		public void setHiddenGetter(String hiddenGetter) {
			this.hiddenGetter = hiddenGetter;
		}

		@JsonProperty("demaner")
		public void setRenamed(String renamed) {
			this.renamed = renamed;
		}

		public void setBirthday(LocalDate birthday) {
			this.birthday = birthday;
		}

		@JsonIgnore
		public String getHiddenGetter() {
			return this.hiddenGetter;
		}
	}

	static class MapWrapper {
		Map<String, Person> people;
		Map<Integer, Person> peopleByInt;

		public Map<String, Person> getPeople() {
			return this.people;
		}

		public Map<Integer, Person> getPeopleByInt() {
			return this.peopleByInt;
		}

		public void setPeople(Map<String, Person> people) {
			this.people = people;
		}

		public void setPeopleByInt(Map<Integer, Person> peopleByInt) {
			this.peopleByInt = peopleByInt;
		}
	}
}
