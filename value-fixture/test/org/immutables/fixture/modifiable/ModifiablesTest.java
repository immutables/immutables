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
package org.immutables.fixture.modifiable;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;

import java.util.Collections;
import java.util.Map;
import org.immutables.fixture.modifiable.FromTypesModifiables.FromType;
import org.junit.jupiter.api.Test;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.immutables.check.Checkers.check;

public class ModifiablesTest {

  @Test
  public void modifiableCollection() {
    ModifiableUnit unit = ModifiableUnit.create();
    unit.getPrices().add(1.0f);
    unit.addPrices(2.0f);

    check(unit.getPrices()).isOf(1.0f, 2.0f);
  }

  @Test
  public void isAttributesSet() {
    ModifiableCompanion c1 = ModifiableCompanion.create();
    check(!c1.isInitialized());

    check(!c1.arrayIntsIsSet());
    c1.setArrayInts(1);
    check(c1.arrayIntsIsSet());

    check(!c1.arrayStringsIsSet());
    c1.setArrayStrings("a", "b", "c");
    check(c1.arrayStringsIsSet());

    check(c1).asString().isNonEmpty();

    check(!c1.integerIsSet());
    c1.setInteger(1);
    check(c1.integerIsSet());

    check(!c1.stringIsSet());
    c1.setString("_");
    check(c1.stringIsSet());

    check(c1.isInitialized());

    check(c1).asString().isNonEmpty();
  }

  @Test
  public void clear() {
    ModifiableCompanion c1 = ModifiableCompanion.create();
    check(!c1.isInitialized());
    c1.setArrayInts(1);
    c1.setArrayStrings("a", "b", "c");
    c1.setInteger(1);
    c1.setString("_");

    check(c1.stringIsSet());
    check(c1.isInitialized());
    c1.addOrds(1, 2, 3);

    c1.clear();

    check(!c1.isInitialized());
    check(!c1.stringIsSet());
    check(c1.ords()).isEmpty();
  }

  @Test
  public void unset() {
    ModifiableCompanion c1 = ModifiableCompanion.create();
    check(!c1.isInitialized());
    c1.setArrayInts(1);
    c1.setArrayStrings("a", "b", "c");
    c1.setInteger(1);
    c1.setString("_");

    check(c1.stringIsSet());
    check(c1.isInitialized());

    c1.unsetArrayInts();
    c1.unsetString();

    check(!c1.isInitialized());
    check(c1.arrayStringsIsSet());
    check(c1.integerIsSet());
    check(!c1.arrayIntsIsSet());
    check(!c1.stringIsSet());

    c1.unsetArrayStrings();
    c1.unsetInteger();

    check(!c1.isInitialized());
    check(!c1.arrayStringsIsSet());
    check(!c1.integerIsSet());
    check(!c1.arrayIntsIsSet());
    check(!c1.stringIsSet());
  }

  @Test
  public void mutatingEquals() {
    ModifiableExtra c1 = ModifiableExtra.create();
    ModifiableExtra c2 = ModifiableExtra.create();

    check(c1).is(c2);

    c1.addBag("a");
    c2.addBag("a");

    check(c1).is(c2);
    check(c1.hashCode()).is(c2.hashCode());

    c1.putIndex(1, "a");
    c2.putIndex(2, "b");

    check(c1).not().is(c2);
    check(c1.hashCode()).not().is(c2.hashCode());
  }

  @Test
  public void uninitializedEquals() {
    ModifiableCompanion c1 = ModifiableCompanion.create();
    ModifiableCompanion c2 = ModifiableCompanion.create();

    check(!c1.isInitialized());
    check(!c2.isInitialized());

    check(c1).is(c1);
    check(c1).not().is(c2);

    check(c1).asString().isNonEmpty();
  }

  @Test
  public void equalsWithDifferentObjectType() {
    check(ModifiableCompanion.create()).not().is(equalTo(new Object()));
  }

  @Test
  public void defaults() {
    ModifiableStandalone m = ModifiableStandalone.create();

    check(m.def()).is(1);
    m.setDef(2);
    check(m.def()).is(2);

    check(m.defs()).is("");
    m.setDefs("a");
    check(m.defs()).is("a");

    check(m).asString().isNonEmpty();
  }

  @Test
  public void deferedAllocationAndNullable() {
    check(ModifiableNullableAndDefault.create()).is(ModifiableNullableAndDefault.create());
    check(ModifiableNullableAndDefault.create().addLst()).is(ModifiableNullableAndDefault.create());
    check(ModifiableNullableAndDefault.create().addLst("c")).not().is(ModifiableNullableAndDefault.create());
    check(!ModifiableNullableAndDefault.create().lstIsSet());
    check(ModifiableNullableAndDefault.create().addLst("d").lstIsSet());
    check(ModifiableNullableAndDefault.create().str()).isNull();
    check(!ModifiableNullableAndDefault.create().intsIsSet());
    check(ModifiableNullableAndDefault.create().ints()).isOf(1);
    ModifiableNullableAndDefault m = ModifiableNullableAndDefault.create();
    m.lst().add("a");
    m.lst().add("b");
    check(m.lst()).isOf("a", "b");
  }

  @Test
  public void listsAreNullableSafe() {
    // Test for #578
    ModifiableStandalone.create().addAllNullableUnit(null);
  }

  @Test
  @SuppressWarnings("deprecation")
  public void modifiableImmutableCollections() {
    ModifiableMutableImmutableCollection m = ModifiableMutableImmutableCollection.create();
    m.addA("a");
    m.addA("b", "c");
    m.addB("d", "e");
    m.putC("x", 1);
    m.putC("y", 2);

    check(m.a()).isA(ImmutableCollection.class);
    check(m.b()).isA(ImmutableCollection.class);
    check(m.c()).isA(ImmutableMultimap.class);
    check(m.d()).isA(ImmutableMap.class);

    check(m.a()).isOf("a", "b", "c");
    check(m.b()).isOf("d", "e");

    check(m.c().values()).isOf(1, 2);
    check(m.c().keySet()).isOf("x", "y");

    check(m.d().isEmpty());

    m.clear();

    check(m.a()).isEmpty();
    check(m.b()).isEmpty();
    check(m.c().entries()).isEmpty();
    check(m.d().entrySet()).isEmpty();
  }

  @Test
  public void allowNulls() {
    ImmutableAllowNullsObject.builder()
        .putMap("key", null)
        .addList((String) null)
        .build()
        .getClass();

    ModifiableAllowNullsObject.create()
        .putMap("key", null)
        .addList((String) null);
  }

  @Test
  public void composeBuilders() {
    ModifiableBeanFriendly first = new ModifiableBeanFriendly();
    first.setPrimary(true);
    first.setId(42);
    ModifiableBeanFriendly second = new ModifiableBeanFriendly();
    second.setPrimary(false);
    second.setDescription("foo");
    ImmutableBeanFriendly result = ImmutableBeanFriendly.builder()
        .from(first)
        .from(second)
        .build();
    check(result.getId()).is(42);
    check(!result.isPrimary());
    check(result.getDescription()).is("foo");
  }

  @Test
  public void copyPartialModifiable() {
    ModifiableBeanFriendly first = new ModifiableBeanFriendly();
    first.setPrimary(true);
    first.setId(42);
    ModifiableBeanFriendly second = new ModifiableBeanFriendly().from(first);
    check(second.getId()).is(42);
  }

  @Test
  public void generics() {
    GenericHolder<String> tester = ImmutableGenericHolder.<String>builder()
        .from(ModifiableGenericHolder.<String>create().setOptional("optional"))
        .mandatory("mandatory")
        .build();

    check(tester.mandatory()).is("mandatory");
    check(tester.optional()).is("optional");
  }

  @Test
  public void modifiablesMergeFrom() {
    ModifiableFromType m = ModifiableFromType.create();

    FromType f = new FromType.Builder()
        .mergeFrom(m) // will not initialize anything but will not fail
        .a(1)
        .build();

    FromType f2 = ModifiableFromType.create()
        .mergeFrom(m) // will not initialize anything but will not fail
        .setA(1)
        .toImmutable();

    check(f2).is(f);
  }

  @Test
  public void modifiablesMergeFromManySupertypes() {
    ModifiableFromManyTypes m = ModifiableFromManyTypes.create()
        .setA(true);

    ImmutableFromManyTypes f = ImmutableFromManyTypes.builder()
        .mergeFrom(m) // will initialize a but not b, but will not fail
        .b(1)
        .build();

    ImmutableFromManyTypes f2 = ModifiableFromManyTypes.create()
        .mergeFrom(ModifiableFromManyTypes.create()) // will not initialize anything but will not
        .mergeFrom(() -> true) // will initialize a
        .setB(1)
        .toImmutable();

    check(f).is(f2);
  }

  @Test
  public void setOnMapSetsOptBits() {
    // test for #1055
    Map<String, String> map = ImmutableMap.of("1", "2");
    ModifiableDefaultMap m1 = ModifiableDefaultMap.create().setMap(map);
    m1.setMap(map).setMap(map); // can set multiple times
    check(m1.getMap()).is(map);
    check(m1.mapIsSet());
    check(ModifiableDefaultMap.create().setMap(Collections.emptyMap()).mapIsSet());
    check(ModifiableDefaultMap.create().setMap(Collections.singletonMap("k", "v")).mapIsSet());
    check(ModifiableDefaultMap.create().putMap("a", "b").mapIsSet());
    check(ModifiableDefaultMap.create().putAllMap(Collections.emptyMap()).mapIsSet());
    check(ModifiableDefaultMap.create().putAllMap(Collections.singletonMap("a", "b")).mapIsSet());
  }
}
