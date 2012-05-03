package org.springframework.data.rest.repository.context;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.rest.repository.RepositoryConstraintViolationException;
import org.springframework.data.rest.repository.ValidationErrors;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * {@link org.springframework.context.ApplicationListener} implementation that dispatches {@link RepositoryEvent}s to a
 * specific {@link Validator}.
 *
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class ValidatingRepositoryEventListener
    extends AbstractRepositoryEventListener<ValidatingRepositoryEventListener>
    implements InitializingBean {

  private static final Logger LOG = LoggerFactory.getLogger(ValidatingRepositoryEventListener.class);

  private Multimap<String, Validator> validators = ArrayListMultimap.create();

  @Override public void afterPropertiesSet() throws Exception {
    if (validators.size() == 0) {
      for (Map.Entry<String, Validator> entry : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext,
                                                                                               Validator.class)
          .entrySet()) {
        String name = null;
        Validator v = entry.getValue();

        if (entry.getKey().contains("Save")) {
          name = entry.getKey().substring(0, name.indexOf("Save") + 4);
        } else if (entry.getKey().contains("Delete")) {
          name = entry.getKey().substring(0, name.indexOf("Delete") + 6);
        }
        if (null != name) {
          this.validators.put(name, v);
        }
      }
    }
  }

  /**
   * Get a Map of {@link Validator}s that are assigned to the various {@link RepositoryEvent}s.
   *
   * @return
   */
  public Map<String, Collection<Validator>> getValidators() {
    return validators.asMap();
  }

  /**
   * Assign a Map of {@link Validator}s that are assigned to the various {@link RepositoryEvent}s.
   *
   * @return
   */
  public ValidatingRepositoryEventListener setValidators(Map<String, Collection<Validator>> validators) {
    for (Map.Entry<String, Collection<Validator>> entry : validators.entrySet()) {
      this.validators.replaceValues(entry.getKey(), entry.getValue());
    }
    return this;
  }

  /**
   * Add a {@link Validator} that will be triggered on the given event.
   *
   * @param event
   * @param validator
   * @return
   */
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
