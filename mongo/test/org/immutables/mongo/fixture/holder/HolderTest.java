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
package org.immutables.mongo.fixture.holder;

import java.util.List;
import org.immutables.mongo.fixture.MongoContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.immutables.check.Checkers.check;

public class HolderTest {

  @Rule
  public final MongoContext context = new MongoContext();

  private HolderRepository repository;

  @Before
  public void setUp() throws Exception {
    repository = new HolderRepository(context.setup());
  }

  /**
   * Tests GSON parsing error when using primitives in polymorphic repository class {@link Holder}
   * {@code Expected VALUE_STRING but was VALUE_NUMBER_FLOAT}. GSON lazily loads numbers (without
   * parsing the string
   * right away) so nextString() token might be number or float instead of string.
   */
  @Test
  public void primitives() throws Exception {
    Primitives prim = ImmutablePrimitives.builder()
        .booleanValue(true)
        .byteValue((byte) 4)
        .shortValue((short) 16)
        .intValue(1024)
        .longValue(8096)
        .floatValue(1.1f)
        .doubleValue(3.3d)
        .build();

    Holder holder = ImmutableHolder.builder().id("h1").value(prim).build();

    check(repository.upsert(holder).getUnchecked()).is(1);

    final List<Holder> holders = repository.findAll().fetchAll().getUnchecked();

    check(holders).hasSize(1);
    check(holders.get(0).id()).is("h1");
    check(holders.get(0)).is(holder);
  }

  @Test
  public void string() throws Exception {
    Holder holder = ImmutableHolder.builder().id("h1").value("foo").build();
    check(repository.upsert(holder).getUnchecked()).is(1);
    check(repository.findAll().fetchAll().getUnchecked()).has(holder);
  }

  @Test
  public void justInt() throws Exception {
    Holder holder = ImmutableHolder.builder().id("h1").value(123).build();
    check(repository.upsert(holder).getUnchecked()).is(1);
    check(repository.findAll().fetchAll().getUnchecked()).has(holder);
  }
}
