package org.springframework.data.rest.repository.context;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.rest.repository.RepositoryConstraintViolationException;
import org.springframework.data.rest.repository.ValidationErrors;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class ValidatingRepositoryEventListener
    extends AbstractRepositoryEventListener<ValidatingRepositoryEventListener>
    implements InitializingBean {

  private static final Logger LOG = LoggerFactory.getLogger(ValidatingRepositoryEventListener.class);

  private Multimap<String, Validator> validators = ArrayListMultimap.create();

  @Override public void afterPropertiesSet() throws Exception {
    if (validators.size() == 0) {
      Map<String, Validator> validators = applicationContext.getBeansOfType(Validator.class);
      for (Map.Entry<String, Validator> entry : validators.entrySet()) {
        String name = entry.getKey();
        Validator v = entry.getValue();

        if (name.contains("Save")) {
          name = name.substring(0, name.indexOf("Save") + 4);
        } else if (name.contains("Delete")) {
          name = name.substring(0, name.indexOf("Delete") + 6);
        }

        this.validators.put(name, v);
      }
    }
  }

  public Map<String, Collection<Validator>> getValidators() {
    return validators.asMap();
  }

  public ValidatingRepositoryEventListener setValidators(Map<String, Collection<Validator>> validators) {
    for (Map.Entry<String, Collection<Validator>> entry : validators.entrySet()) {
      this.validators.replaceValues(entry.getKey(), entry.getValue());
    }
    return this;
  }

  public ValidatingRepositoryEventListener addValidator(String event, Validator validator) {
    validators.put(event, validator);
    return this;
  }

  @Override protected void onBeforeSave(Object entity) {
    validate("beforeSave", entity);
  }

  @Override protected void onAfterSave(Object entity) {
    validate("afterSave", entity);
  }

  @Override protected void onBeforeLinkSave(Object parent, Object linked) {
    validate("beforeLinkSave", parent);
  }

  @Override protected void onAfterLinkSave(Object parent, Object linked) {
    validate("afterLinkSave", parent);
  }

  @Override protected void onBeforeDelete(Object entity) {
    validate("beforeDelete", entity);
  }

  @Override protected void onAfterDelete(Object entity) {
    validate("afterDelete", entity);
  }

  private Errors validate(String event, Object entity) {
    Errors errors = null;
    if (null != entity) {
      Class<?> domainType = entity.getClass();
      errors = new ValidationErrors(domainType.getSimpleName(),
                                    entity,
                                    repositoryMetadataFor(domainType).entityMetadata());
      Collection<Validator> validators = this.validators.get(event);
      if (null != validators) {
        for (Validator v : validators) {
          if (v.supports(entity.getClass())) {
            LOG.debug(event + ": " + entity + " with " + v);
            ValidationUtils.invokeValidator(v, entity, errors);
          }
        }
      }
      if (errors.getErrorCount() > 0) {
        throw new RepositoryConstraintViolationException(errors);
      }
    }
    return errors;
  }

}
