package org.immutables.criteria.sql.compiler;

import org.h2.jdbcx.JdbcDataSource;
import org.immutables.criteria.Criterias;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.sql.SQLSetup;
import org.immutables.criteria.sql.reflection.SQLTypeMetadata;
import org.junit.Test;

import javax.sql.DataSource;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SQLCompilerTests {
    private static DataSource datasource() {
        final JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test");
        ds.setUser("sa");
        ds.setPassword("sa");
        return ds;
    }

    private static SQLSetup setup(final DataSource datasource, final String table) {
        return SQLSetup.of(datasource, SQLTypeMetadata.of(Dummy.class));
    }

    @Test
    public void testEmptySelect() {
        final SQLSetup setup = setup(datasource(), "dummy");
        final SQLSelectStatement result = SQLCompiler.select(setup, Query.of(Dummy.class));
        assertEquals("SELECT `id`,`name` FROM `dummy`",
                setup.dialect().select(result));
    }

    @Test
    public void testEqualitySelect() {
        final SQLSetup setup = setup(datasource(), "dummy");
        final SQLSelectStatement result = SQLCompiler.select(setup(datasource(), "dummy"),
                Criterias.toQuery(DummyCriteria.dummy.id.is(1)));
        assertEquals("SELECT `id`,`name` FROM `dummy` WHERE (`id`)=(:param_0)",
                setup.dialect().select(result));
        assertTrue("Missing filter", result.filter().isPresent());
        assertEquals(1, result.filter().get().parameters().get(":param_0").value());
    }

    @Test
    public void testInequalitySelect() {
        final SQLSetup setup = setup(datasource(), "dummy");
        final SQLSelectStatement result = SQLCompiler.select(setup(datasource(), "dummy"),
                Criterias.toQuery(DummyCriteria.dummy.id.isNot(1)));
        assertEquals("SELECT `id`,`name` FROM `dummy` WHERE (`id`)!=(:param_0)",
                setup.dialect().select(result));
        assertTrue("Missing filter", result.filter().isPresent());
        assertEquals(1, result.filter().get().parameters().get(":param_0").value());
    }

    @Test
    public void testLessThanSelect() {
        final SQLSetup setup = setup(datasource(), "dummy");
        final SQLSelectStatement result = SQLCompiler.select(setup(datasource(), "dummy"),
                Criterias.toQuery(DummyCriteria.dummy.id.lessThan(1)));
        assertEquals("SELECT `id`,`name` FROM `dummy` WHERE (`id`)<(:param_0)",
                setup.dialect().select(result));
        assertTrue("Missing filter", result.filter().isPresent());
        assertEquals(1, result.filter().get().parameters().get(":param_0").value());
    }

    @Test
    public void testLessThanEqualsSelect() {
        final SQLSetup setup = setup(datasource(), "dummy");
        final SQLSelectStatement result = SQLCompiler.select(setup(datasource(), "dummy"),
                Criterias.toQuery(DummyCriteria.dummy.id.atMost(1)));
        assertEquals("SELECT `id`,`name` FROM `dummy` WHERE (`id`)<=(:param_0)",
                setup.dialect().select(result));
        assertTrue("Missing filter", result.filter().isPresent());
        assertEquals(1, result.filter().get().parameters().get(":param_0").value());
    }

    @Test
    public void testGreaterThanSelect() {
        final SQLSetup setup = setup(datasource(), "dummy");
        final SQLSelectStatement result = SQLCompiler.select(setup(datasource(), "dummy"),
                Criterias.toQuery(DummyCriteria.dummy.id.greaterThan(1)));
        assertEquals("SELECT `id`,`name` FROM `dummy` WHERE (`id`)>(:param_0)",
                setup.dialect().select(result));
        assertTrue("Missing filter", result.filter().isPresent());
        assertEquals(1, result.filter().get().parameters().get(":param_0").value());
    }

    @Test
    public void testGreaterThanEqualsSelect() {
        final SQLSetup setup = setup(datasource(), "dummy");
        final SQLSelectStatement result = SQLCompiler.select(setup(datasource(), "dummy"),
                Criterias.toQuery(DummyCriteria.dummy.id.atLeast(1)));
        assertEquals("SELECT `id`,`name` FROM `dummy` WHERE (`id`)>=(:param_0)",
                setup.dialect().select(result));
        assertTrue("Missing filter", result.filter().isPresent());
        assertEquals(1, result.filter().get().parameters().get(":param_0").value());
    }

    @Test
    public void testBetweenSelect() {
        final SQLSetup setup = setup(datasource(), "dummy");
        final SQLSelectStatement result = SQLCompiler.select(setup(datasource(), "dummy"),
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
        final SQLSetup setup = setup(datasource(), "dummy");
        final SQLSelectStatement result = SQLCompiler.select(setup(datasource(), "dummy"),
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
        final SQLSetup setup = setup(datasource(), "dummy");
        final SQLSelectStatement result = SQLCompiler.select(setup(datasource(), "dummy"),
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
        final SQLSetup setup = setup(datasource(), "dummy");
        final SQLSelectStatement result = SQLCompiler.select(setup(datasource(), "dummy"),
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
        final SQLSetup setup = setup(datasource(), "dummy");
        final SQLSelectStatement result = SQLCompiler.select(setup(datasource(), "dummy"),
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
        final SQLSetup setup = setup(datasource(), "dummy");
        final SQLSelectStatement result = SQLCompiler.select(setup(datasource(), "dummy"),
                Criterias.toQuery(DummyCriteria.dummy.name.startsWith("a")));
        assertEquals("SELECT `id`,`name` FROM `dummy` WHERE (`name`)LIKE(CONCAT(:param_0,\"%\"))",
                setup.dialect().select(result));
        assertTrue("Missing filter", result.filter().isPresent());
        assertEquals("a", result.filter().get().parameters().get(":param_0").value());
    }

    @Test
    public void testStringEndsWithSelect() {
        final SQLSetup setup = setup(datasource(), "dummy");
        final SQLSelectStatement result = SQLCompiler.select(setup(datasource(), "dummy"),
                Criterias.toQuery(DummyCriteria.dummy.name.endsWith("a")));
        assertEquals("SELECT `id`,`name` FROM `dummy` WHERE (`name`)LIKE(CONCAT(\"%\",:param_0))",
                setup.dialect().select(result));
        assertTrue("Missing filter", result.filter().isPresent());
        assertEquals("a", result.filter().get().parameters().get(":param_0").value());
    }

    @Test
    public void testStringContainsSelect() {
        final SQLSetup setup = setup(datasource(), "dummy");
        final SQLSelectStatement result = SQLCompiler.select(setup(datasource(), "dummy"),
                Criterias.toQuery(DummyCriteria.dummy.name.contains("a")));
        assertEquals("SELECT `id`,`name` FROM `dummy` WHERE (`name`)LIKE(CONCAT(\"%\",:param_0,\"%\"))",
                setup.dialect().select(result));
        assertTrue("Missing filter", result.filter().isPresent());
        assertEquals("a", result.filter().get().parameters().get(":param_0").value());
    }

    @Test
    public void testStringMatchesSelect() {
        final SQLSetup setup = setup(datasource(), "dummy");
        final SQLSelectStatement result = SQLCompiler.select(setup(datasource(), "dummy"),
                Criterias.toQuery(DummyCriteria.dummy.name.matches(Pattern.compile("a.*b'.b?"))));
        assertEquals("SELECT `id`,`name` FROM `dummy` WHERE (`name`)LIKE('a%b''_b%')",
                setup.dialect().select(result));
    }

    @Test
    public void testStringHasLengthSelect() {
        final SQLSetup setup = setup(datasource(), "dummy");
        final SQLSelectStatement result = SQLCompiler.select(setup(datasource(), "dummy"),
                Criterias.toQuery(DummyCriteria.dummy.name.hasLength(10)));
        assertEquals("SELECT `id`,`name` FROM `dummy` WHERE LEN(`name`)=(:param_0)",
                setup.dialect().select(result));
        assertTrue("Missing filter", result.filter().isPresent());
        assertEquals(10, result.filter().get().parameters().get(":param_0").value());
    }

    @Test
    public void testStringIsEmptySelect() {
        final SQLSetup setup = setup(datasource(), "dummy");
        final SQLSelectStatement result = SQLCompiler.select(setup(datasource(), "dummy"),
                Criterias.toQuery(DummyCriteria.dummy.name.isEmpty()));
        assertEquals("SELECT `id`,`name` FROM `dummy` WHERE (`name`)=(:param_0)",
                setup.dialect().select(result));
        assertTrue("Missing filter", result.filter().isPresent());
        assertEquals("", result.filter().get().parameters().get(":param_0").value());
    }

    @Test
    public void testStringIsNotEmptySelect() {
        final SQLSetup setup = setup(datasource(), "dummy");
        final SQLSelectStatement result = SQLCompiler.select(setup(datasource(), "dummy"),
                Criterias.toQuery(DummyCriteria.dummy.name.notEmpty()));
        assertEquals("SELECT `id`,`name` FROM `dummy` WHERE (`name`)!=(:param_0)",
                setup.dialect().select(result));
        assertTrue("Missing filter", result.filter().isPresent());
        assertEquals("", result.filter().get().parameters().get(":param_0").value());
    }

    @Test
    public void testAndExpression() {
        final SQLSetup setup = setup(datasource(), "dummy");
        final SQLSelectStatement result = SQLCompiler.select(setup(datasource(), "dummy"),
                Criterias.toQuery(DummyCriteria.dummy.id.is(1).and(DummyCriteria.dummy.name.is("A"))));
        assertEquals("SELECT `id`,`name` FROM `dummy` WHERE ((`id`)=(:param_0))AND((`name`)=(:param_1))",
                setup.dialect().select(result));
        assertTrue("Missing filter", result.filter().isPresent());
        assertEquals(1, result.filter().get().parameters().get(":param_0").value());
        assertEquals("A", result.filter().get().parameters().get(":param_1").value());
    }

    @Test
    public void testOrExpression() {
        final SQLSetup setup = setup(datasource(), "dummy");
        final SQLSelectStatement result = SQLCompiler.select(setup(datasource(), "dummy"),
                Criterias.toQuery(DummyCriteria.dummy.id.is(1).or(DummyCriteria.dummy.name.is("A"))));
        assertEquals("SELECT `id`,`name` FROM `dummy` WHERE ((`id`)=(:param_0))OR((`name`)=(:param_1))",
                setup.dialect().select(result));
        assertTrue("Missing filter", result.filter().isPresent());
        assertEquals(1, result.filter().get().parameters().get(":param_0").value());
        assertEquals("A", result.filter().get().parameters().get(":param_1").value());
    }

    @Test
    public void testCompoundExpression() {
        final SQLSetup setup = setup(datasource(), "dummy");
        final SQLSelectStatement result = SQLCompiler.select(setup(datasource(), "dummy"),
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
