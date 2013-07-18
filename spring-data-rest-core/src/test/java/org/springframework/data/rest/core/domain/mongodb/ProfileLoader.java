package org.springframework.data.rest.core.domain.mongodb;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Jon Brisbin
 */
@Component
public class ProfileLoader implements InitializingBean {

	@Autowired private ProfileRepository profiles;

	@Override
	public void afterPropertiesSet() throws Exception {
		profiles.save(new Profile("jdoe", "jdoe", "account"));
	}

}
