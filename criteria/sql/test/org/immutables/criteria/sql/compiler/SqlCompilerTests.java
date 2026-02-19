/*
 * Copyright 2022 Immutables Authors and Contributors
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
package org.immutables.criteria.sql.compiler;

import org.h2.jdbcx.JdbcDataSource;
import org.immutables.criteria.Criterias;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.sql.SqlSetup;
import org.immutables.criteria.sql.reflection.SqlTypeMetadata;
import org.junit.Test;

import javax.sql.DataSource;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SqlCompilerTests {
  private static DataSource datasource() {
    final JdbcDataSource ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:test");
    ds.setUser("sa");
    ds.setPassword("sa");
    return ds;
  }

  private static SqlSetup setup(final DataSource datasource, final String table) {
    return SqlSetup.of(datasource, SqlTypeMetadata.of(Dummy.class));
  }

  @Test
  public void testEmptySelect() {
    final SqlSetup setup = setup(datasource(), "dummy");
    final SqlSelectStatement result = SqlCompiler.select(setup, Query.of(Dummy.class));
    assertEquals("SELECT `id`,`name` FROM `dummy`",
        setup.dialect().select(result));
  }

  @Test
  public void testEqualitySelect() {
    final SqlSetup setup = setup(datasource(), "dummy");
    final SqlSelectStatement result = SqlCompiler.select(setup(datasource(), "dummy"),
        Criterias.toQuery(DummyCriteria.dummy.id.is(1)));
    assertEquals("SELECT `id`,`name` FROM `dummy` WHERE (`id`)=(:param_0)",
        setup.dialect().select(result));
    assertTrue("Missing filter", result.filter().isPresent());
    assertEquals(1, result.filter().get().parameters().get(":param_0").value());
  }

  @Test
  public void testInequalitySelect() {
    final SqlSetup setup = setup(datasource(), "dummy");
    final SqlSelectStatement result = SqlCompiler.select(setup(datasource(), "dummy"),
        Criterias.toQuery(DummyCriteria.dummy.id.isNot(1)));
    assertEquals("SELECT `id`,`name` FROM `dummy` WHERE (`id`)!=(:param_0)",
        setup.dialect().select(result));
    assertTrue("Missing filter", result.filter().isPresent());
    assertEquals(1, result.filter().get().parameters().get(":param_0").value());
  }

  @Test
  public void testLessThanSelect() {
    final SqlSetup setup = setup(datasource(), "dummy");
    final SqlSelectStatement result = SqlCompiler.select(setup(datasource(), "dummy"),
        Criterias.toQuery(DummyCriteria.dummy.id.lessThan(1)));
    assertEquals("SELECT `id`,`name` FROM `dummy` WHERE (`id`)<(:param_0)",
        setup.dialect().select(result));
    assertTrue("Missing filter", result.filter().isPresent());
    assertEquals(1, result.filter().get().parameters().get(":param_0").value());
  }

  @Test
  public void testLessThanEqualsSelect() {
    final SqlSetup setup = setup(datasource(), "dummy");
    final SqlSelectStatement result = SqlCompiler.select(setup(datasource(), "dummy"),
        Criterias.toQuery(DummyCriteria.dummy.id.atMost(1)));
    assertEquals("SELECT `id`,`name` FROM `dummy` WHERE (`id`)<=(:param_0)",
        setup.dialect().select(result));
    assertTrue("Missing filter", result.filter().isPresent());
    assertEquals(1, result.filter().get().parameters().get(":param_0").value());
  }

  @Test
  public void testGreaterThanSelect() {
    final SqlSetup setup = setup(datasource(), "dummy");
    final SqlSelectStatement result = SqlCompiler.select(setup(datasource(), "dummy"),
        Criterias.toQuery(DummyCriteria.dummy.id.greaterThan(1)));
    assertEquals("SELECT `id`,`name` FROM `dummy` WHERE (`id`)>(:param_0)",
        setup.dialect().select(result));
    assertTrue("Missing filter", result.filter().isPresent());
    assertEquals(1, result.filter().get().parameters().get(":param_0").value());
  }

  @Test
  public void testGreaterThanEqualsSelect() {
    final SqlSetup setup = setup(datasource(), "dummy");
    final SqlSelectStatement result = SqlCompiler.select(setup(datasource(), "dummy"),
        Criterias.toQuery(DummyCriteria.dummy.id.atLeast(1)));
    assertEquals("SELECT `id`,`name` FROM `dummy` WHERE (`id`)>=(:param_0)",
        setup.dialect().select(result));
    assertTrue("Missing filter", result.filter().isPresent());
    assertEquals(1, result.filter().get().parameters().get(":param_0").value());
  }

  @Test
  public void testBetweenSelect() {
    final SqlSetup setup = setup(datasource(), "dummy");
    final SqlSelectStatement result = SqlCompiler.select(setup(datasource(), "dummy"),
        Criterias.toQuery(DummyCriteria.dummy.id.between(1, 2)));
    // Note that this is how criteria maps this
    assertEquals("SELECT `id`,`name` FROM `dummy` WHERE ((`id`)>=(:param_0))AND((`id`)<=(:param_1))",
        setup.dialect().select(result));
    assertTrue("Missing filter", result.filter().isPresent());
    assertEquals(1, result.filter().get().parameters().get(":param_0").value());
    assertEquals(2, result.filter().get().parameters().get(":param_1").value());
  }

  @Test
  public void testInSelect() {
    final SqlSetup setup = setup(datasource(), "dummy");
    final SqlSelectStatement result = SqlCompiler.select(setup(datasource(), "dummy"),
        Criterias.toQuery(DummyCriteria.dummy.id.in(1, 2, 3)));
    assertEquals("SELECT `id`,`name` FROM `dummy` WHERE (`id`)IN(:param_0,:param_1,:param_2)",
        setup.dialect().select(result));
    assertTrue("Missing filter", result.filter().isPresent());
    assertEquals(1, result.filter().get().parameters().get(":param_0").value());
    assertEquals(2, result.filter().get().parameters().get(":param_1").value());
    assertEquals(3, result.filter().get().parameters().get(":param_2").value());
  }

  @Test
  public void testNotInSelect() {
    final SqlSetup setup = setup(datasource(), "dummy");
    final SqlSelectStatement result = SqlCompiler.select(setup(datasource(), "dummy"),
        Criterias.toQuery(DummyCriteria.dummy.id.notIn(1, 2, 3)));
    assertEquals("SELECT `id`,`name` FROM `dummy` WHERE (`id`)NOT IN(:param_0,:param_1,:param_2)",
        setup.dialect().select(result));
    assertTrue("Missing filter", result.filter().isPresent());
    assertEquals(1, result.filter().get().parameters().get(":param_0").value());
    assertEquals(2, result.filter().get().parameters().get(":param_1").value());
    assertEquals(3, result.filter().get().parameters().get(":param_2").value());
  }

  @Test
  public void testStringInSelect() {
    final SqlSetup setup = setup(datasource(), "dummy");
    final SqlSelectStatement result = SqlCompiler.select(setup(datasource(), "dummy"),
        Criterias.toQuery(DummyCriteria.dummy.name.in("a", "b", "c")));
    assertEquals("SELECT `id`,`name` FROM `dummy` WHERE (`name`)IN(:param_0,:param_1,:param_2)",
        setup.dialect().select(result));
    assertTrue("Missing filter", result.filter().isPresent());
    assertEquals("a", result.filter().get().parameters().get(":param_0").value());
    assertEquals("b", result.filter().get().parameters().get(":param_1").value());
    assertEquals("c", result.filter().get().parameters().get(":param_2").value());
  }

  @Test
  public void testStringNotInSelect() {
    final SqlSetup setup = setup(datasource(), "dummy");
    final SqlSelectStatement result = SqlCompiler.select(setup(datasource(), "dummy"),
        Criterias.toQuery(DummyCriteria.dummy.name.notIn("a", "b", "c")));
    assertEquals("SELECT `id`,`name` FROM `dummy` WHERE (`name`)NOT IN(:param_0,:param_1,:param_2)",
        setup.dialect().select(result));
    assertTrue("Missing filter", result.filter().isPresent());
    assertEquals("a", result.filter().get().parameters().get(":param_0").value());
    assertEquals("b", result.filter().get().parameters().get(":param_1").value());
    assertEquals("c", result.filter().get().parameters().get(":param_2").value());
  }

  @Test
  public void testStringStartsWithSelect() {
    final SqlSetup setup = setup(datasource(), "dummy");
    final SqlSelectStatement result = SqlCompiler.select(setup(datasource(), "dummy"),
        Criterias.toQuery(DummyCriteria.dummy.name.startsWith("a")));
    assertEquals("SELECT `id`,`name` FROM `dummy` WHERE (`name`)LIKE(CONCAT(:param_0,\"%\"))",
        setup.dialect().select(result));
    assertTrue("Missing filter", result.filter().isPresent());
    assertEquals("a", result.filter().get().parameters().get(":param_0").value());
  }

  @Test
  public void testStringEndsWithSelect() {
    final SqlSetup setup = setup(datasource(), "dummy");
    final SqlSelectStatement result = SqlCompiler.select(setup(datasource(), "dummy"),
        Criterias.toQuery(DummyCriteria.dummy.name.endsWith("a")));
    assertEquals("SELECT `id`,`name` FROM `dummy` WHERE (`name`)LIKE(CONCAT(\"%\",:param_0))",
        setup.dialect().select(result));
    assertTrue("Missing filter", result.filter().isPresent());
    assertEquals("a", result.filter().get().parameters().get(":param_0").value());
  }

  @Test
  public void testStringContainsSelect() {
    final SqlSetup setup = setup(datasource(), "dummy");
    final SqlSelectStatement result = SqlCompiler.select(setup(datasource(), "dummy"),
        Criterias.toQuery(DummyCriteria.dummy.name.contains("a")));
    assertEquals("SELECT `id`,`name` FROM `dummy` WHERE (`name`)LIKE(CONCAT(\"%\",:param_0,\"%\"))",
        setup.dialect().select(result));
    assertTrue("Missing filter", result.filter().isPresent());
    assertEquals("a", result.filter().get().parameters().get(":param_0").value());
  }

  @Test
  public void testStringMatchesSelect() {
    final SqlSetup setup = setup(datasource(), "dummy");
    final SqlSelectStatement result = SqlCompiler.select(setup(datasource(), "dummy"),
        Criterias.toQuery(DummyCriteria.dummy.name.matches(Pattern.compile("a.*b'.b?"))));
    assertEquals("SELECT `id`,`name` FROM `dummy` WHERE (`name`)LIKE('a%b''_b%')",
        setup.dialect().select(result));
  }

  @Test
  public void testStringHasLengthSelect() {
    final SqlSetup setup = setup(datasource(), "dummy");
    final SqlSelectStatement result = SqlCompiler.select(setup(datasource(), "dummy"),
        Criterias.toQuery(DummyCriteria.dummy.name.hasLength(10)));
    assertEquals("SELECT `id`,`name` FROM `dummy` WHERE LEN(`name`)=(:param_0)",
        setup.dialect().select(result));
    assertTrue("Missing filter", result.filter().isPresent());
    assertEquals(10, result.filter().get().parameters().get(":param_0").value());
  }

  @Test
  public void testStringIsEmptySelect() {
    final SqlSetup setup = setup(datasource(), "dummy");
    final SqlSelectStatement result = SqlCompiler.select(setup(datasource(), "dummy"),
        Criterias.toQuery(DummyCriteria.dummy.name.isEmpty()));
    assertEquals("SELECT `id`,`name` FROM `dummy` WHERE (`name`)=(:param_0)",
        setup.dialect().select(result));
    assertTrue("Missing filter", result.filter().isPresent());
    assertEquals("", result.filter().get().parameters().get(":param_0").value());
  }

  @Test
  public void testStringIsNotEmptySelect() {
    final SqlSetup setup = setup(datasource(), "dummy");
    final SqlSelectStatement result = SqlCompiler.select(setup(datasource(), "dummy"),
        Criterias.toQuery(DummyCriteria.dummy.name.notEmpty()));
    assertEquals("SELECT `id`,`name` FROM `dummy` WHERE (`name`)!=(:param_0)",
        setup.dialect().select(result));
    assertTrue("Missing filter", result.filter().isPresent());
    assertEquals("", result.filter().get().parameters().get(":param_0").value());
  }

  @Test
  public void testAndExpression() {
    final SqlSetup setup = setup(datasource(), "dummy");
    final SqlSelectStatement result = SqlCompiler.select(setup(datasource(), "dummy"),
        Criterias.toQuery(DummyCriteria.dummy.id.is(1).and(DummyCriteria.dummy.name.is("A"))));
    assertEquals("SELECT `id`,`name` FROM `dummy` WHERE ((`id`)=(:param_0))AND((`name`)=(:param_1))",
        setup.dialect().select(result));
    assertTrue("Missing filter", result.filter().isPresent());
    assertEquals(1, result.filter().get().parameters().get(":param_0").value());
    assertEquals("A", result.filter().get().parameters().get(":param_1").value());
  }

  @Test
  public void testOrExpression() {
    final SqlSetup setup = setup(datasource(), "dummy");
    final SqlSelectStatement result = SqlCompiler.select(setup(datasource(), "dummy"),
        Criterias.toQuery(DummyCriteria.dummy.id.is(1).or(DummyCriteria.dummy.name.is("A"))));
    assertEquals("SELECT `id`,`name` FROM `dummy` WHERE ((`id`)=(:param_0))OR((`name`)=(:param_1))",
        setup.dialect().select(result));
    assertTrue("Missing filter", result.filter().isPresent());
    assertEquals(1, result.filter().get().parameters().get(":param_0").value());
    assertEquals("A", result.filter().get().parameters().get(":param_1").value());
  }

  @Test
  public void testCompoundExpression() {
    final SqlSetup setup = setup(datasource(), "dummy");
    final SqlSelectStatement result = SqlCompiler.select(setup(datasource(), "dummy"),
        Criterias.toQuery(DummyCriteria.dummy.name.is("A")
            .or()
            .name.is("B")
            .id.is(1)));
    assertEquals("SELECT `id`,`name` FROM `dummy` WHERE ((`name`)=(:param_0))OR(((`name`)=(:param_1))AND((`id`)=(:param_2)))",
        setup.dialect().select(result));
    assertTrue("Missing filter", result.filter().isPresent());
    assertEquals("A", result.filter().get().parameters().get(":param_0").value());
    assertEquals("B", result.filter().get().parameters().get(":param_1").value());
    assertEquals(1, result.filter().get().parameters().get(":param_2").value());
  }
}
