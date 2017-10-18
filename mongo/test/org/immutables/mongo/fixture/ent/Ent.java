/*
   Copyright 2016 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.mongo.fixture.ent;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.GsonBuilder;
import com.mongodb.MongoClient;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import org.immutables.gson.Gson.TypeAdapters;
import org.immutables.mongo.Mongo.Repository;
import org.immutables.mongo.fixture.ent.EntRepository.Criteria;
import org.immutables.mongo.repository.RepositorySetup;
import org.immutables.mongo.types.TimeInstant;
import org.immutables.value.Value.Immutable;

@Immutable
@Repository
@TypeAdapters
public abstract class Ent {
  public abstract String uuid();

  public abstract String action();

  public abstract Optional<TimeInstant> expires();

  public static void main(String... args) throws UnknownHostException {
    MongoClient client = new MongoClient("localhost");
    RepositorySetup setup = RepositorySetup.builder()
        .database(client.getDatabase("test"))
        .executor(MoreExecutors.listeningDecorator(Executors.newCachedThreadPool()))
        .gson(new GsonBuilder()
            .registerTypeAdapterFactory(new GsonAdaptersEnt())
            .create())
        .build();

    EntRepository repository = new EntRepository(setup);

    EntRepository.Criteria where = repository.criteria()
        .uuid("8b7a881c-6ccb-4ada-8f6a-60cc99e6aa20")
        .actionIn("BAN", "IPBAN");

    Criteria or = where.expiresAbsent()
        .or()
        .with(where)
        .expiresGreaterThan(TimeInstant.of(1467364749679L));

    System.out.println(or);

    repository.find(or).fetchAll().getUnchecked();
  }
}
