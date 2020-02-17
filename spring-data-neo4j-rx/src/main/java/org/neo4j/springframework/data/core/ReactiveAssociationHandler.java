package org.neo4j.springframework.data.core;

import java.util.Set;

import org.neo4j.springframework.data.core.mapping.Neo4jPersistentEntity;
import org.neo4j.springframework.data.core.mapping.Neo4jPersistentProperty;
import org.neo4j.springframework.data.core.schema.RelationshipDescription;
import org.springframework.data.mapping.Association;

public class ReactiveAssociationHandler extends Neo4jAssociationHandler {
	protected ReactiveAssociationHandler(Neo4jPersistentEntity<?> parentPersistentEntity, Object parentObject,
		String inDatabase, Set<RelationshipDescription> processedRelationshipDescriptions) {
		super(parentPersistentEntity, parentObject, inDatabase, processedRelationshipDescriptions, cypherGenerator,
			mappingContext);
	}

	@Override
	public void doWithAssociation(Association<Neo4jPersistentProperty> association) {

	}
}
