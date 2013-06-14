package org.springframework.data.rest.webmvc.json;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

/**
 * Helper class to register datatype modules based on their presence in the classpath.
 *
 * @author Jon Brisbin
 */
public class Jackson2DatatypeHelper {

	private static final Logger  LOG                            = LoggerFactory.getLogger(Jackson2DatatypeHelper.class);
	private static final boolean IS_HIBERNATE4_MODULE_AVAILABLE = ClassUtils.isPresent(
			"com.fasterxml.jackson.datatype.hibernate4.Hibernate4Module",
			Jackson2DatatypeHelper.class.getClassLoader()
	);
	private static final boolean IS_JODA_MODULE_AVAILABLE       = ClassUtils.isPresent(
			"com.fasterxml.jackson.datatype.joda.JodaModule",
			Jackson2DatatypeHelper.class.getClassLoader()
	);

	public static void configureObjectMapper(ObjectMapper mapper) {
		// Hibernate types
		if(IS_HIBERNATE4_MODULE_AVAILABLE) {
			try {
				mapper.registerModule((Module)Class.forName("com.fasterxml.jackson.datatype.hibernate4.Hibernate4Module")
				                                   .newInstance());
			} catch(Throwable t) {
				if(LOG.isDebugEnabled()) {
					LOG.debug(t.getMessage(), t);
				}
			}
		}
		// JODA time
		if(IS_JODA_MODULE_AVAILABLE) {
			try {
				mapper.registerModule((Module)Class.forName("com.fasterxml.jackson.datatype.joda.JodaModule")
				                                   .newInstance());
			} catch(Throwable t) {
				if(LOG.isDebugEnabled()) {
					LOG.debug(t.getMessage(), t);
				}
			}
		}
	}

}
