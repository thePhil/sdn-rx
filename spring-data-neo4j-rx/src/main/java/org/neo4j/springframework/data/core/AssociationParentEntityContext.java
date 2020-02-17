package org.neo4j.springframework.data.core;

import org.neo4j.springframework.data.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.util.Assert;

public class AssociationParentEntityContext<T> {

	private final PersistentPropertyAccessor<T> parentPropertyAccessor;
	private final Neo4jPersistentEntity<T> parentPersistentEntity;
	private final T parentObject;
	private final Object fromId;

	public AssociationParentEntityContext(
		T parentObject,
		Neo4jPersistentEntity<T> parentPersistentEntity
	) {
		String msg = "Parent Entity and it's metadata must be present";
		Assert.notNull(parentPersistentEntity, msg);
		Assert.notNull(parentObject, msg);
		this.parentObject = parentObject;
		this.parentPersistentEntity = parentPersistentEntity;

		// init common properties
		this.parentPropertyAccessor = parentPersistentEntity.getPropertyAccessor(parentObject);
		this.fromId = parentPropertyAccessor.getProperty(parentPersistentEntity.getRequiredIdProperty());
	}

	public Object getFromId() {
		return fromId;
	}


	public Neo4jPersistentEntity<T> getParentPersistentEntity() {
		return parentPersistentEntity;
	}

	public T getParentObject() {
		return parentObject;
	}

	public PersistentPropertyAccessor<T> getParentPropertyAccessor() {
		return parentPropertyAccessor;
	}

	protected boolean isObverseNew() {
		return this.parentPersistentEntity.isNew(parentObject);
	}
}
