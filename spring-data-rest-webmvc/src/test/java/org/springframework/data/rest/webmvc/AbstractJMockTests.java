package org.springframework.data.rest.webmvc;

import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.legacy.ClassImposteriser;

/**
 * Abstract base classes for JUnit tests that use JMock.
 *
 * @author Jon Brisbin
 */
public abstract class AbstractJMockTests {

	protected JUnitRuleMockery context = new JUnitRuleMockery() {{
		setImposteriser(ClassImposteriser.INSTANCE);
	}};

}
