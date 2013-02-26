package org.springframework.data.rest;

import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.runner.RunWith;

/**
 * Abstract base classes for JUnit tests that use JMock.
 *
 * @author Jon Brisbin
 */
@RunWith(JMock.class)
public abstract class AbstractJMockTests {

  protected JUnit4Mockery context = new JUnit4Mockery() {{
    setImposteriser(ClassImposteriser.INSTANCE);
  }};

}
