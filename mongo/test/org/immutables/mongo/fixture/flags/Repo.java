package org.immutables.mongo.fixture.flags;

import com.google.common.base.Optional;
import org.immutables.mongo.Mongo;
import org.immutables.value.Value;

public interface Repo {

  @Mongo.Id
  String id();

  Optional<String> name();

  @Mongo.Repository
  @Value.Immutable
  interface Standard extends Repo {

  }

  /**
   * Repository without delete / andModifyFirst / andReplaceFirst methods.
   */
  @Mongo.Repository(readonly = true)
  @Value.Immutable
  interface Readonly extends Repo {

  }


  @Mongo.Repository(index = false, readonly = true)
  @Value.Immutable
  interface NoIndex extends Repo {

  }

}
