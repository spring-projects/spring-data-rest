package org.springframework.data.rest.repository.invoke;

import static org.springframework.util.ReflectionUtils.*;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.core.RepositoryInformation;

/**
 * @author Jon Brisbin
 */
public class RepositoryMethodInvoker implements PagingAndSortingRepository<Object, Serializable> {

  private final Object repository;
  private final Map<String, RepositoryMethod> queryMethods = new HashMap<String, RepositoryMethod>();
  private RepositoryMethod saveOne;
  private RepositoryMethod saveSome;
  private RepositoryMethod findOne;
  private RepositoryMethod exists;
  private RepositoryMethod findAll;
  private RepositoryMethod findAllSorted;
  private RepositoryMethod findAllPaged;
  private RepositoryMethod findSome;
  private RepositoryMethod count;
  private RepositoryMethod deleteOne;
  private RepositoryMethod deleteOneById;
  private RepositoryMethod deleteSome;
  private RepositoryMethod deleteAll;

  @SuppressWarnings({"unchecked"})
  public RepositoryMethodInvoker(Object repository,
                                 RepositoryInformation repoInfo,
                                 final PersistentEntity persistentEntity) {
    this.repository = repository;
    Class<?> repoType = repoInfo.getRepositoryInterface();

    doWithMethods(repoType, new MethodCallback() {
      @Override public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
        String name = method.getName();
        int cardinality = method.getParameterTypes().length;
        Class<?> paramType = (cardinality == 1 ? method.getParameterTypes()[0] : null);
        boolean someMethod = (null != paramType && Iterable.class.isAssignableFrom(paramType));
        boolean byIdMethod = (null != paramType && paramType == Serializable.class);
        boolean sortable = (null != paramType && Sort.class.isAssignableFrom(paramType));
        boolean pageable = (null != paramType && Pageable.class.isAssignableFrom(paramType));
        RepositoryMethod repoMethod = new RepositoryMethod(method);

        if("save".equals(name) && someMethod) {
          saveSome = repoMethod;
        } else if("save".equals(name)) {
          saveOne = repoMethod;
        } else if("findOne".equals(name)) {
          findOne = repoMethod;
        } else if("exists".equals(name)) {
          exists = repoMethod;
        } else if("findAll".equals(name) && someMethod) {
          findSome = repoMethod;
        } else if("findAll".equals(name) && sortable) {
          findAllSorted = repoMethod;
        } else if("findAll".equals(name) && pageable) {
          findAllPaged = repoMethod;
        } else if("findAll".equals(name)) {
          findAll = repoMethod;
        } else if("count".equals(name)) {
          count = repoMethod;
        } else if("delete".equals(name) && byIdMethod) {
          deleteOneById = repoMethod;
        } else if("delete".equals(name) && someMethod) {
          deleteSome = repoMethod;
        } else if("delete".equals(name)) {
          deleteOne = repoMethod;
        } else if("deleteAll".equals(name)) {
          deleteAll = repoMethod;
        } else {
          queryMethods.put(name, repoMethod);
        }
      }
    });
  }

  @SuppressWarnings({"unchecked"})
  @Override public <S extends Object> S save(S entity) {
    return (S)invokeMethod(saveOne.getMethod(), repository, entity);
  }

  public boolean hasSaveOne() {
    return null != saveOne;
  }

  @SuppressWarnings({"unchecked"})
  @Override public <S extends Object> Iterable<S> save(Iterable<S> entities) {
    return (Iterable<S>)invokeMethod(saveSome.getMethod(), repository, entities);
  }

  public boolean hasSaveSome() {
    return null != saveSome;
  }

  @Override public Object findOne(Serializable serializable) {
    return invokeMethod(findOne.getMethod(), repository, serializable);
  }

  public boolean hasFindOne() {
    return null != findOne;
  }

  @Override public boolean exists(Serializable serializable) {
    return (Boolean)invokeMethod(exists.getMethod(), repository, serializable);
  }

  public boolean hasExists() {
    return null != exists;
  }

  @SuppressWarnings({"unchecked"})
  @Override public Iterable<Object> findAll() {
    return (Iterable<Object>)invokeMethod(findAll.getMethod(), repository);
  }

  public boolean hasFindAll() {
    return null != findAll;
  }

  @SuppressWarnings({"unchecked"})
  @Override public Iterable<Object> findAll(Iterable<Serializable> serializables) {
    return (Iterable<Object>)invokeMethod(findSome.getMethod(), repository, serializables);
  }

  public boolean hasFindSome() {
    return null != findSome;
  }

  @SuppressWarnings({"unchecked"})
  @Override public Iterable<Object> findAll(Sort sort) {
    return (Iterable<Object>)invokeMethod(findAllSorted.getMethod(), repository, sort);
  }

  public boolean hasFindAllSorted() {
    return null != findAllSorted;
  }

  @SuppressWarnings({"unchecked"})
  @Override public Page<Object> findAll(Pageable pageable) {
    return (Page<Object>)invokeMethod(findAllPaged.getMethod(), repository, pageable);
  }

  public boolean hasFindAllPageable() {
    return null != findAllPaged;
  }

  @Override public void delete(Serializable serializable) {
    invokeMethod(deleteOneById.getMethod(), repository, serializable);
  }

  public boolean hasDeleteOneById() {
    return null != deleteOneById;
  }

  @Override public long count() {
    return (Long)invokeMethod(count.getMethod(), repository);
  }

  public boolean hasCount() {
    return null != count;
  }

  @Override public void delete(Object entity) {
    invokeMethod(deleteOne.getMethod(), repository, entity);
  }

  public boolean hasDeleteOne() {
    return null != deleteOne;
  }

  @Override public void delete(Iterable<?> entities) {
    invokeMethod(deleteSome.getMethod(), repository, entities);
  }

  public boolean hasDeleteSome() {
    return null != deleteSome;
  }

  @Override public void deleteAll() {
    invokeMethod(deleteAll.getMethod(), repository);
  }

  public boolean hasDeleteAll() {
    return null != deleteAll;
  }

  public Map<String, RepositoryMethod> getQueryMethods() {
    return queryMethods;
  }

  public RepositoryMethod getRepositoryMethod(String name) {
    return queryMethods.get(name);
  }

  public Object invokeQueryMethod(String name, Object... params) {
    RepositoryMethod repoMethod = queryMethods.get(name);
    if(null == repoMethod) {
      throw new NoSuchMethodError(name);
    }
    return invokeMethod(repoMethod.getMethod(), repository, params);
  }

  public Object invokeQueryMethod(RepositoryMethod method, Object... params) {
    return invokeMethod(method.getMethod(), repository, params);
  }

}
