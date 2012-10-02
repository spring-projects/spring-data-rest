package org.springframework.data.rest.test.webmvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
@Component
public class PersonLoader implements InitializingBean {

  @Autowired
  private PersonRepository  personRepository;
  @Autowired
  private ProfileRepository profileRepository;
  @Autowired
  private AddressRepository addressRepository;

  @Transactional
  @Override public void afterPropertiesSet()
      throws Exception {

    Person p1 = personRepository.save(new Person("John Doe"));

    Map<String, Profile> pers1profiles = new HashMap<String, Profile>();
    Profile twitter = profileRepository.save(new Profile("twitter", "#!/johndoe", p1));
    Profile fb = profileRepository.save(new Profile("facebook", "/johndoe", p1));
    pers1profiles.put("twitter", twitter);
    pers1profiles.put("facebook", fb);
    p1.setProfiles(pers1profiles);

    Address pers1addr = addressRepository.save(new Address(new String[]{"1234 W. 1st St."},
                                                           "Univille",
                                                           "ST",
                                                           "12345"));
    p1.setAddresses(Arrays.asList(pers1addr));

    personRepository.save(p1);


    Person p2 = personRepository.save(new Person("Jane Doe"));

    Map<String, Profile> pers2profiles = new HashMap<String, Profile>();
    Profile twitter2 = profileRepository.save(new Profile("twitter", "#!/janedoe", p2));
    Profile fb2 = profileRepository.save(new Profile("facebook", "/janedoe", p2));
    pers2profiles.put("twitter", twitter2);
    pers2profiles.put("facebook", fb2);
    p2.setProfiles(pers2profiles);

    Address pers2addr = addressRepository.save(new Address(new String[]{"1234 E. 2nd St."},
                                                           "Univille",
                                                           "ST",
                                                           "12345"));
    p2.setAddresses(Arrays.asList(pers2addr));

    personRepository.save(p2);

  }

}
