package org.springframework.data.rest.core.projection;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.data.rest.core.config.Projection;

public class SubTypeAwareProxyProjectionFactoryTest {

	SubTypeAwareProxyProjectionFactory factory = new SubTypeAwareProxyProjectionFactory();

	/**
	 * @see DATAREST-739
	 */
	@Test
	public void supportsProjectionsWithoutAnnotations() {

		UnannotatedProjection projection = factory.createProjection(UnannotatedProjection.class, new Object());
		assertThat(projection, instanceOf(UnannotatedProjection.class));
	}

	/**
	 * @see DATAREST-739
	 */
	@Test
	public void createsSuperProjectionIfNoSubs() {

		AnnotatedProjection projection = factory.createProjection(AnnotatedProjection.class, new Object());
		assertThat(projection, instanceOf(AnnotatedProjection.class));
	}

	/**
	 * @see DATAREST-739
	 */
	@Test
	public void createsSuperProjectionIfNoMatchingSubs() {

		ParentProjection projection = factory.createProjection(ParentProjection.class, new Object());
		assertThat(projection, instanceOf(ParentProjection.class));
		assertThat(projection, not(anyOf(
				instanceOf(ChildAProjection.class),
				instanceOf(ChildBProjection.class),
				instanceOf(ChildBCProjection.class)
		)));
	}

	/**
	 * @see DATAREST-739
	 */
	@Test
	public void createsMatchingSubProjection() {

		ParentProjection projection = factory.createProjection(ParentProjection.class, new ChildA());
		assertThat(projection, instanceOf(ChildAProjection.class));
		assertThat(projection, not(instanceOf(SubChildAProjection.class)));
	}

	/**
	 * @see DATAREST-739
	 */
	@Test
	public void createsMatchingSubProjectionRecursively() {

		ParentProjection projection = factory.createProjection(ParentProjection.class, new SubChildA());
		assertThat(projection, instanceOf(SubChildAProjection.class));
	}

	/**
	 * @see DATAREST-739
	 */
	@Test
	public void createsFirstMatchingSubProjection() {

		ParentProjection projection = factory.createProjection(ParentProjection.class, new ChildB());
		assertThat(projection, instanceOf(ChildBProjection.class));
	}

	/**
	 * @see DATAREST-739
	 */
	@Test
	public void createsMatchingSubProjectionForAnyTypeMatched() {

		ParentProjection projection = factory.createProjection(ParentProjection.class, new ChildC());
		assertThat(projection, instanceOf(ChildBCProjection.class));
	}

	interface UnannotatedProjection {
	}

	@Projection(types = Object.class)
	interface AnnotatedProjection {
	}

	@Projection(types = Parent.class, subProjections = {ChildAProjection.class, ChildBProjection.class, ChildBCProjection.class})
	interface ParentProjection {
	}

	@Projection(types = ChildA.class, subProjections = SubChildAProjection.class)
	interface ChildAProjection extends ParentProjection {
	}

	@Projection(types = ChildB.class)
	interface ChildBProjection extends ParentProjection {
	}

	@Projection(types = SubChildA.class)
	interface SubChildAProjection extends ChildAProjection {
	}

	@Projection(types = {ChildB.class, ChildC.class})
	interface ChildBCProjection extends ParentProjection {
	}

	class Parent {
	}

	class ChildA extends Parent {
	}

	class ChildB extends Parent {
	}

	class ChildC extends Parent {
	}

	class ChildD extends Parent {
	}

	class SubChildA extends ChildA {
	}
}