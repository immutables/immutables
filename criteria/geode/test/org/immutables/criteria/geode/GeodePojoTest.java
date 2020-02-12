/*
 * Copyright 2019 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.criteria.geode;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.query.FunctionDomainException;
import org.apache.geode.cache.query.NameResolutionException;
import org.apache.geode.cache.query.QueryInvocationTargetException;
import org.apache.geode.cache.query.SelectResults;
import org.apache.geode.cache.query.TypeMismatchException;
import org.immutables.criteria.backend.ContainerNaming;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GeodeExtension.class)
public class GeodePojoTest {

  private final Cache cache;
  private final ContainerNaming containerNaming;
  private final AutocreateRegion autocreate;

  private Region<String, GeodePojo> region;

  public GeodePojoTest(Cache cache) {
    this.cache = cache;
    this.containerNaming = ContainerNaming.DEFAULT;
    this.autocreate = new AutocreateRegion(cache, containerNaming);
  }

  @BeforeEach
  void setUp() {
    autocreate.accept(GeodePojo.class);

    region = cache.getRegion(containerNaming.name(GeodePojo.class));

    setUpTestData();
  }

  @Test
  void regex() throws NameResolutionException, TypeMismatchException, QueryInvocationTargetException, FunctionDomainException {
    final String query = "SELECT COUNT(*) FROM /geodePojo where name.matches($1)";
    Assertions.assertEquals(1, executeCountQuery(query, "\\w+"));
    Assertions.assertEquals(2, executeCountQuery(query, "\\w*"));
    Assertions.assertEquals(2, executeCountQuery(query, "^\\w+\\s\\w+$"));
  }

  @Test
  void stringNull() throws NameResolutionException, TypeMismatchException, QueryInvocationTargetException, FunctionDomainException {
    Assertions.assertEquals(1, executeCountQuery("SELECT COUNT(*) FROM /geodePojo where name = null"));
    Assertions.assertEquals(4, executeCountQuery("SELECT COUNT(*) FROM /geodePojo where name != null"));
  }

  @Test
  void stringEmpty() throws NameResolutionException, TypeMismatchException, QueryInvocationTargetException, FunctionDomainException {
    Assertions.assertEquals(1, executeCountQuery("SELECT COUNT(*) FROM /geodePojo where name = ''"));
  }

  @Test
  void stringNotEmpty() throws NameResolutionException, TypeMismatchException, QueryInvocationTargetException, FunctionDomainException {
    Assertions.assertEquals(4, executeCountQuery("SELECT COUNT(*) FROM /geodePojo where name != ''"));
  }

  @Test
  void stringLength() throws NameResolutionException, TypeMismatchException, QueryInvocationTargetException, FunctionDomainException {
    final String query = "SELECT COUNT(*) FROM /geodePojo where name.length = $1";
    Assertions.assertEquals(1, executeCountQuery(query, 0));
    Assertions.assertEquals(2, executeCountQuery(query, 8));
  }

  private int executeCountQuery(String query, Object... bindVariables) throws NameResolutionException, TypeMismatchException, QueryInvocationTargetException, FunctionDomainException {
    return ((SelectResults<Integer>) execute(query, bindVariables)).stream()
            .findAny().orElseThrow(IllegalArgumentException::new);
  }

  private Object execute(String query, Object... bindVariables) throws FunctionDomainException, TypeMismatchException, NameResolutionException, QueryInvocationTargetException {
    return cache.getQueryService().newQuery(query).execute(bindVariables);
  }

  private void setUpTestData() {
    Map<String, GeodePojo> pojos = createPojos();
    region.putAll(pojos);
  }

  private static Map<String, GeodePojo> createPojos() {
    final Function<GeodePojo, String> toName = GeodePojo::getName;
    return createPojos("John Doe", "Jane Doe", "Joe", "", null)
            .collect(Collectors.toMap(toName.andThen(Objects::toString), Function.identity()));
  }

  private static Stream<GeodePojo> createPojos(String... names) {
    return Arrays.stream(names).map(name -> {
      final GeodePojo pojo = new GeodePojo();
      pojo.setName(name);
      return pojo;
    });
  }

  public static class GeodePojo {

    String name;

    public String getName() {
      return name;
    }

    public GeodePojo setName(String name) {
      this.name = name;
      return this;
    }

  }
}
