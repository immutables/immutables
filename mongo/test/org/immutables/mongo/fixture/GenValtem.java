package org.immutables.mongo.fixture;

import org.immutables.mongo.Mongo;
import org.immutables.mongo.fixture.imp.Valtem;
import org.immutables.mongo.repository.RepositorySetup;
import org.immutables.value.Value;

@Mongo.Repository
@Value.Include(Valtem.class)
enum GenValtem {
	;
	void use() {
		RepositorySetup setup = RepositorySetup.forUri("mongodb://localhost/test");
		ValtemRepository repo = new ValtemRepository(setup);
		repo.criteria();
	}
}
