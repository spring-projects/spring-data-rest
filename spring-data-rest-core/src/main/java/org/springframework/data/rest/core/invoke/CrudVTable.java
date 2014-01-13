package org.springframework.data.rest.core.invoke;

import java.lang.reflect.Method;

import org.springframework.util.ReflectionUtils;

/**
 * A 'VTable' for CRUD methods supported by
 * {@link org.springframework.data.repository.PagingAndSortingRepository} or
 * {@link org.springframework.data.repository.CrudRepository}.
 * 
 * This class contains a method delegate for each CRUD method. 
 * 
 * @author Nick Weedon
 */
public class CrudVTable {

	/**
	 * Simple delegate class that uses reflection to invoke
	 * the assigned method on the assigned repository. 
	 * 
	 * @author Nick Weedon
	 */
	public static class CrudMethodDelegate {
		private final Object repository;
		private Method method;

		/**
		 * Construct a delegate pointing to a repository CRUD method.
		 * @param repository The repository to invoke against
		 * @param method The CRUD method to call 
		 * 
		 * @author Nick Weedon
		 */
		public CrudMethodDelegate(Object repository, Method method) {
			this.repository = repository;
			this.method = method;
		}

		/**
		 * Call the repository CRUD method 
		 * using {@link org.springframework.util.ReflectionUtils#invokeMethod(Method, Object, Object...)}
		 * 
		 * @param arguments The method arguments
		 * 
		 * @author Nick Weedon
		 */
		@SuppressWarnings("unchecked")
		public <T> T invoke(Object... arguments) {
			return (T) ReflectionUtils.invokeMethod(method, repository, arguments);
		}
	};

	// The CRUD method delegates
	private CrudMethodDelegate saveMethod; 
	private CrudMethodDelegate deleteMethod; 
	private CrudMethodDelegate findOneMethod;
	private CrudMethodDelegate findAllPagableMethod;
	private CrudMethodDelegate findAllSortMethod;
	
	// Getters/Setters for CRUD method delegates
	public CrudVTable.CrudMethodDelegate getSaveMethod() {
		return saveMethod;
	}
	public void setSaveMethod(CrudVTable.CrudMethodDelegate saveMethod) {
		this.saveMethod = saveMethod;
	}
	public CrudVTable.CrudMethodDelegate getDeleteMethod() {
		return deleteMethod;
	}
	public void setDeleteMethod(CrudVTable.CrudMethodDelegate deleteMethod) {
		this.deleteMethod = deleteMethod;
	}
	public CrudVTable.CrudMethodDelegate getFindOneMethod() {
		return findOneMethod;
	}
	public void setFindOneMethod(CrudVTable.CrudMethodDelegate findOneMethod) {
		this.findOneMethod = findOneMethod;
	}
	public CrudVTable.CrudMethodDelegate getFindAllPagableMethod() {
		return findAllPagableMethod;
	}
	public void setFindAllPagableMethod(CrudVTable.CrudMethodDelegate findAllPagableMethod) {
		this.findAllPagableMethod = findAllPagableMethod;
	}
	public CrudVTable.CrudMethodDelegate getFindAllSortMethod() {
		return findAllSortMethod;
	}
	public void setFindAllSortMethod(CrudVTable.CrudMethodDelegate findAllSortMethod) {
		this.findAllSortMethod = findAllSortMethod;
	}
}