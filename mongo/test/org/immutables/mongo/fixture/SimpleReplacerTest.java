/*
   Copyright 2017 Immutables Authors and Contributors

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
package org.immutables.mongo.fixture;

import com.mongodb.DuplicateKeyException;
import org.junit.Rule;
import org.junit.Test;
import static org.immutables.check.Checkers.check;
import static org.junit.Assert.fail;

public class SimpleReplacerTest {

  @Rule
  public final MongoContext context = new MongoContext();

  private final EntityRepository repository = new EntityRepository(context.setup());

  @Test
  public void findAndReplace() throws Exception {
    final ImmutableEntity entity = ImmutableEntity.builder().id("e1").version(0).value("v0").build();

    repository.upsert(entity).getUnchecked();

    // first upsert
    repository.find(repository.criteria().id(entity.id()).version(entity.version()))
        .andReplaceFirst(entity.withVersion(1).withValue("v1"))
        .upsert()
        .getUnchecked();

    check(findById("e1").version()).is(1);
    check(findById("e1").value()).isOf("v1");

    // second upsert
    repository.find(repository.criteria().id(entity.id()).version(1))
        .andReplaceFirst(entity.withVersion(2).withValue("v2"))
        .upsert()
        .getUnchecked();

    check(findById("e1").version()).is(2);
    check(findById("e1").value()).isOf("v2");

    // now try to update version which doesn't exists (v0). should return null (absent)
    check(repository.find(repository.criteria().id(entity.id()).version(0))
        .andReplaceFirst(entity.withVersion(33).withValue("v33"))
        .update()
        .getUnchecked()).isAbsent();

    // last version is 2
    check(findById("e1").version()).is(2);
  }

  /**
   * When upsert is requested on different versions but same ID there should be duplicate
   * key exception thrown by Mongo since there will be an attempt to insert new document (same id
   * different version)
   * Based on criteria it is a new document, based on primary key ({@code _id}) it exists already.
   */
  @Test
  public void duplicateKeyException_upsert_SameKey_different_versions() throws Exception {
    final ImmutableEntity entity = ImmutableEntity.builder().id("e1").version(0).value("v0").build();
    repository.upsert(entity).getUnchecked();

    // first upsert successful (document should be with new version)
    repository.find(repository.criteria().id(entity.id()).version(0))
        .andReplaceFirst(entity.withVersion(1))
        .upsert()
        .getUnchecked();

    try {
      // this should fail because here upsert == insert (document e1 with version 0 doesn't exist)
      repository.find(repository.criteria().id(entity.id()).version(0))
          .andReplaceFirst(entity.withVersion(1))
          .upsert()
          .getUnchecked();

      fail("Should fail with " + DuplicateKeyException.class.getName());
    } catch (Exception e) {
      if (!(e.getCause() instanceof DuplicateKeyException)) {
        fail(String.format("Expected failure to be %s got %s",
            DuplicateKeyException.class.getName(),
            e.getCause().getClass()));
      }
    }

  }

  @Test
  public void updateUpsert_when_empty() throws Exception {
    ImmutableEntity entity = ImmutableEntity.builder().id("e1").version(0).value("v1").build();

    check(repository.find(repository.criteria().id("missing").version(0))
        .andReplaceFirst(entity)
        .update()
        .getUnchecked()).isAbsent();

    check(repository.find(repository.criteria().id(entity.id()))
        .andReplaceFirst(entity)
        .returningNew()
        .upsert()
        .getUnchecked()).isOf(entity);

    check(repository.findAll().fetchAll().getUnchecked()).hasSize(1);

  }

  private Entity findById(String id) {
    return repository.findById(id).fetchFirst().getUnchecked().get();
  }

}
