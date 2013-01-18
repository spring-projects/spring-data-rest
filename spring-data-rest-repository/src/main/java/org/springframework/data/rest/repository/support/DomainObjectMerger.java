package org.springframework.data.rest.repository.support;

import static org.springframework.beans.BeanUtils.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.repository.support.Repositories;

/**
 * @author Jon Brisbin
 */
public class DomainObjectMerger {

  private final Map<Class<?>, PersistentEntity> entities      = new ConcurrentHashMap<Class<?>, PersistentEntity>();
  private final Map<String, Object>             defaultValues = new ConcurrentHashMap<String, Object>();
  private final Repositories      repositories;
  private final ConversionService conversionService;

  @Autowired
  public DomainObjectMerger(Repositories repositories,
                            ConversionService conversionService) {
    this.repositories = repositories;
    this.conversionService = conversionService;
  }

  @SuppressWarnings({"unchecked"})
  public void merge(Object from, Object target) {
    if(null == from || null == target) {
      return;
    }
    final BeanWrapper fromWrapper = BeanWrapper.create(from, conversionService);
    final BeanWrapper targetWrapper = BeanWrapper.create(target, conversionService);

    PersistentEntity entity = getPerisistentEntity(target.getClass());
    Class<?> clazz = entity.getType();
    final String clazzName = clazz.getSimpleName();

    entity.doWithProperties(new PropertyHandler() {
      @Override public void doWithPersistentProperty(PersistentProperty persistentProperty) {
        String mapKey = clazzName + "." + persistentProperty.getName();
        Object fromVal = fromWrapper.getProperty(persistentProperty);
        Object defaultVal = defaultValues.get(mapKey);
        if(null != fromVal && !fromVal.equals(defaultVal)) {
          targetWrapper.setProperty(persistentProperty, fromVal);
        }
      }
    });

    entity.doWithAssociations(new AssociationHandler() {
      @Override public void doWithAssociation(Association association) {
        PersistentProperty persistentProperty = association.getInverse();
        String mapKey = clazzName + "." + persistentProperty.getName();
        Object fromVal = fromWrapper.getProperty(persistentProperty);
        Object defaultVal = defaultValues.get(mapKey);
        if(null != fromVal && !fromVal.equals(defaultVal)) {
          targetWrapper.setProperty(persistentProperty, fromVal);
        }
      }
    });
  }

  @SuppressWarnings({"unchecked"})
  private PersistentEntity getPerisistentEntity(Class<?> clazz) {
    PersistentEntity entity = entities.get(clazz);
    if(null == entity) {
      entity = repositories.getPersistentEntity(clazz);
      final String clazzName = clazz.getSimpleName();
      final BeanWrapper wrapper = BeanWrapper.create(instantiateClass(clazz), conversionService);
      entity.doWithProperties(new PropertyHandler() {
        @Override public void doWithPersistentProperty(PersistentProperty persistentProperty) {
          Object val = wrapper.getProperty(persistentProperty);
          if(null != val) {
            defaultValues.put(clazzName + "." + persistentProperty.getName(), val);
          }
        }
      });
      entity.doWithAssociations(new AssociationHandler() {
        @Override public void doWithAssociation(Association association) {
          PersistentProperty persistentProperty = association.getInverse();
          Object val = wrapper.getProperty(persistentProperty);
          if(null != val) {
            defaultValues.put(clazzName + "." + persistentProperty.getName(), val);
          }
        }
      });
      entities.put(clazz, entity);
    }
    return entity;
  }

}
