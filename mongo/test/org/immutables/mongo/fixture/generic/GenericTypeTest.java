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
package org.immutables.mongo.fixture.generic;

import org.immutables.mongo.fixture.MongoContext;
import org.immutables.mongo.fixture.holder.ImmutablePrimitives;
import org.immutables.mongo.fixture.holder.Primitives;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.immutables.check.Checkers.check;

public class GenericTypeTest {

  @Rule
  public final MongoContext context = MongoContext.create();

  private GenericHolderRepository repository;

  @Before
  public void setUp() {
    repository = new GenericHolderRepository(context.setup());
  }

  @Test
  public void object() {
    Primitives prim = ImmutablePrimitives.builder()
        .booleanValue(true)
        .byteValue((byte) 4)
        .shortValue((short) 16)
        .intValue(1024)
        .longValue(8096)
        .floatValue(1.1f)
        .doubleValue(3.3d)
        .build();

    GenericHolder genericHolder = ImmutableGenericHolder.builder()
        .id("h1")
        .value(ImmutableGenericType.<Primitives>builder().value(prim).build())
        .build();

    check(repository.upsert(genericHolder).getUnchecked()).is(1);

    final List<GenericHolder> GenericHolders = repository.findAll().fetchAll().getUnchecked();

    check(GenericHolders).hasSize(1);
    check(GenericHolders.get(0).id()).is("h1");
    check(GenericHolders.get(0)).is(genericHolder);
  }

  @Test
  public void string() {
    GenericHolder genericHolder = ImmutableGenericHolder.builder()
        .id("h1")
        .value(ImmutableGenericType.<String>builder().value("123").build())
        .build();
    check(repository.upsert(genericHolder).getUnchecked()).is(1);
    check(repository.findAll().fetchAll().getUnchecked()).has(genericHolder);
  }

  @Test
  public void justInt() {
    GenericHolder genericHolder = ImmutableGenericHolder.builder()
        .id("h1")
        .value(ImmutableGenericType.<Integer>builder().value(123).build())
        .build();
    check(repository.upsert(genericHolder).getUnchecked()).is(1);
    check(repository.findAll().fetchAll().getUnchecked()).has(genericHolder);
  }

  @Test
  public void listOfStrings() {
    GenericHolder genericHolder = ImmutableGenericHolder.builder()
        .id("h1")
        .value(ImmutableGenericType.<String>builder().value("123").build())
        .addListOfValues(
            ImmutableGenericType.<String>builder().value("a").build(),
            ImmutableGenericType.<String>builder().value("a").build()
        )
        .build();
    check(repository.upsert(genericHolder).getUnchecked()).is(1);
    check(repository.findAll().fetchAll().getUnchecked()).has(genericHolder);
  }
}
