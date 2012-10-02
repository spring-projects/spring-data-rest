package org.springframework.data.rest.test;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.Module;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.module.SimpleSerializers;
import org.codehaus.jackson.map.ser.std.SerializerBase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.rest.test.webmvc.Person;
import org.springframework.data.rest.test.webmvc.PersonValidator;
import org.springframework.data.rest.test.webmvc.TestRepositoryEventListener;
import org.springframework.data.rest.webmvc.RepositoryRestMvcConfiguration;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;

/**
 * @author Jon Brisbin
 */
@Configuration
@Import(RepositoryRestMvcConfiguration.class)
public class ApplicationRestConfig {

  @SuppressWarnings({"unchecked"})
  @Bean public ConversionService customConversionService() {
    DefaultFormattingConversionService cs = new DefaultFormattingConversionService();
    cs.addConverter(new Converter<String[], List<Long>>() {
      @Override public List<Long> convert(String[] source) {
        List<Long> longs = new ArrayList<Long>(source.length);
        for(String s : source) {
          longs.add(Long.parseLong(s));
        }
        return longs;
      }
    });
    //    cs.addConverter(new Converter<Person, Resource>() {
    //      @Override public Resource convert(Person person) {
    //        Map<String, Object> m = new HashMap<String, Object>();
    //        m.put("name", person.getName());
    //        CustomResource r = new CustomResource(m);
    //        r.add(new Link("http://localhost:8080/people/1", "self"));
    //        return r;
    //      }
    //    });
    return cs;
  }

  @Bean public ResourceProcessor<Resource<Person>> personProcessor() {
    return new ResourceProcessor<Resource<Person>>() {
      @Override public Resource<Person> process(Resource<Person> resource) {
        System.out.println("\t***** ResourceProcessor for Person: " + resource);
        resource.add(new Link("http://localhost:8080/people", "added-link"));
        return resource;
      }
    };
  }

  @Bean public TestRepositoryEventListener testRepositoryEventListener() {
    return new TestRepositoryEventListener();
  }

  /**
   * This validator will be picked up automatically. The default configuration is to look at the bean name
   * and figure out what event you're interested in. This validator is interested in 'beforeSave' events
   * because the word 'beforeSave' appears in the first part of the bean name. It recognizes:
   * <p/>
   * - beforeSave
   * - afterSave
   * - beforeDelete
   * - afterDelete
   * - beforeLinkSave
   * - afterLinkSave
   * <p/>
   * What you put after that doesn't matter, you just need to make the bean name unique, of course.
   *
   * @return
   */
  @Bean public PersonValidator beforeSavePersonValidator() {
    return new PersonValidator();
  }

  @Bean public Module customModule() {
    return new Module() {
      private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

      @Override public String getModuleName() {
        return "custom";
      }

      @Override public Version version() {
        return Version.unknownVersion();
      }

      @Override public void setupModule(SetupContext context) {
        context.getDeserializationConfig().withDateFormat(dateFormat);

        SimpleSerializers sers = new SimpleSerializers();
        sers.addSerializer(Timestamp.class, new SerializerBase<Timestamp>(Timestamp.class) {
          @Override public void serialize(Timestamp value, JsonGenerator jgen, SerializerProvider provider)
              throws IOException, JsonGenerationException {
            synchronized(dateFormat) {
              jgen.writeString(dateFormat.format(value));
            }
          }
        });

        context.addSerializers(sers);
      }
    };
  }

}
