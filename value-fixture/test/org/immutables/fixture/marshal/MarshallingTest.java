/*
    Copyright 2013-2014 Immutables Authors and Contributors

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
package org.immutables.fixture.marshal;

import com.google.gson.JsonNull;
import org.immutables.fixture.subpack.SillySubstructure;
import org.immutables.fixture.SillyStructure;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.google.common.base.CharMatcher;
import com.google.gson.reflect.TypeToken;
import de.undercouch.bson4jackson.BsonFactory;
import java.util.List;
import org.immutables.fixture.ImmutableHasNullable;
import org.immutables.fixture.ImmutableJsonIgnore;
import org.immutables.fixture.ImmutableSillySub1;
import org.immutables.fixture.ImmutableSillySub2;
import org.immutables.fixture.ImmutableSillySub3;
import org.immutables.fixture.JsonIgnore;
import org.immutables.fixture.SillyPolyHost;
import org.immutables.fixture.SillyPolyHost2;
import org.immutables.fixture.SillyTuplie;
import org.immutables.fixture.nested.ImmutableCadabra;
import org.immutables.fixture.nested.NonGrouped;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

@SuppressWarnings("resource")
public class MarshallingTest {

  JsonFactory jsonFactory = new JsonFactory()
      .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
      .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
      .disable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);

  JsonFactory strictierJsonFactory = new JsonFactory()
      .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
      .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);

  BsonFactory bsonFactory = new BsonFactory();

  @Test
  public void discoveredMarhaler() {
    SillySubstructure substructure =
        Marshaling.fromJson("{\"e1\":\"SOURCE\"}", SillySubstructure.class);
    check(substructure).not().isNull();
  }

  @Test
  public void unmarshalSingleton() {
    check(Marshaling.fromJson("[11]", NonGrouped.Cadabra.class)).same(ImmutableCadabra.of());
    check(Marshaling.fromJson("{\"x\": true}", NonGrouped.Cadabra.class)).same(ImmutableCadabra.of());
    check(Marshaling.fromJson("{}", NonGrouped.Cadabra.class)).same(ImmutableCadabra.of());
  }

  @Test
  public void unmarshalNull() {
    check(Marshaling.fromJson("null", NonGrouped.Cadabra.class)).isNull();
    check(Marshaling.getGson().getAdapter(NonGrouped.Cadabra.class).toJsonTree(null)).isA(JsonNull.class);
  }

  @Test
  public void nullableMarshaling() {
    check(CharMatcher.WHITESPACE.removeFrom(Marshaling.toJson(ImmutableHasNullable.of())))
        .is("{}");
    check(Marshaling.fromJson("{}", ImmutableHasNullable.class)).is(ImmutableHasNullable.of());
    check(Marshaling.fromJson("{\"in\":1}", ImmutableHasNullable.class)).is(ImmutableHasNullable.of(1));
    check(Marshaling.fromJson("{\"def\":\"1\"}", ImmutableHasNullable.class)).is(ImmutableHasNullable.of().withDef("1"));
  }

  @Test
  public void jsonIgnore() {
    ImmutableJsonIgnore minimal = ImmutableJsonIgnore.of(1);
    ImmutableJsonIgnore expanded = minimal.withValues(1, 2);
    check(minimal).not().is(expanded);
    check(Marshaling.fromJson(Marshaling.toJson(expanded), JsonIgnore.class)).is(minimal);
  }

  @Test
  public void unmarshalingPolymorphicTypes() {
    SillyPolyHost host = Marshaling.fromJson("{ s:[{a:1},{b:'b'},{a:14}] }", SillyPolyHost.class);
    check(host.s()).isOf(
        ImmutableSillySub1.builder().a(1).build(),
        ImmutableSillySub2.builder().b("b").build(),
        ImmutableSillySub1.builder().a(14).build());

    SillyPolyHost2 s1 = Marshaling.fromJson("{s:{b:[1,2]}}", SillyPolyHost2.class);
    check(s1.s()).is(ImmutableSillySub3.builder().addB(1).addB(2).build());

    SillyPolyHost2 s2 = Marshaling.fromJson("{s:{b:'b'}}", SillyPolyHost2.class);
    check(s2.s()).is(ImmutableSillySub2.builder().b("b").build());
  }

  @Test
  public void marshalingPolymorphicTypesList() {
    SillyPolyHost h = Marshaling.fromJson("{s:[{a:1},{b:'b'},{a:14}]}", SillyPolyHost.class);
    check(Marshaling.fromJson(Marshaling.toJson(h), SillyPolyHost.class)).is(h);
  }

  @Test
  public void marshalingIterableMethods() {
    TypeToken<List<SillySubstructure>> m1 = new TypeToken<List<SillySubstructure>>() {};

    List<SillySubstructure> it = fromJsonIterable("[{e1:'SOURCE'},{e1:'CLASS'},{e1:'RUNTIME'}]", m1);

    check(fromJsonIterable(toJsonIterable(it, m1), m1)).is(it);
    TypeToken<List<SillyTuplie>> m2 = new TypeToken<List<SillyTuplie>>() {};

    List<SillyTuplie> tuplies = fromJsonIterable("[[1,null,[]],[2,null,[]]]", m2);
    check(fromJsonIterable(toJsonIterable(tuplies, m2), m2)).is(tuplies);
  }

  @Test
  public void marshalingPolymorphicOptionalTypes() {
    TypeToken<List<SillyPolyHost2>> m = new TypeToken<List<SillyPolyHost2>>() {};
    List<SillyPolyHost2> list = fromJsonIterable("[{s:{b:[1,2]},o:{b:'b'}}]", m);
    check(fromJsonIterable(toJsonIterable(list, m), m)).is(list);
    check(list.get(0).o()).isOf(ImmutableSillySub2.builder().b("b").build());
  }

  @Test
  public void marshalAndUnmarshalGeneratedType() {
    SillyStructure structure =
        Marshaling.fromJson("{attr1:'x', flag2:false,opt3:1, very4:33, wet5:555.55, subs6:null,"
            + " nest7:{ set2:'METHOD', set3: [1,2,4],floats4:[333.11] },"
            + "int9:0, tup3: [1212.441, null, [true,true,false]]}", SillyStructure.class);

    check(Marshaling.fromJson(Marshaling.toJson(structure), SillyStructure.class)).is(structure);
  }

  private <T> List<T> fromJsonIterable(String json, TypeToken<List<T>> typeToken) {
    return Marshaling.getGson().fromJson(json, typeToken.getType());
  }

  private <T> String toJsonIterable(List<? extends T> list, TypeToken<List<T>> typeToken) {
    return Marshaling.getGson().toJson(list, typeToken.getType());
  }
}
