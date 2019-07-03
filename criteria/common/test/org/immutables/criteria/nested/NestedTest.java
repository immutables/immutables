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

package org.immutables.criteria.nested;

import org.immutables.criteria.Criterias;
import org.immutables.criteria.Criterion;
import org.immutables.criteria.expression.DebugExpressionVisitor;
import org.immutables.criteria.expression.Query;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Deeply nested criterias
 */
public class NestedTest {

  @Test
  public void nested() {
    assertExpressional(RootCriteria.root.a.isPresent(), "call op=IS_PRESENT path=a");
    assertExpressional(RootCriteria.root.a.value().value.isEmpty(), "call op=EQUAL path=a.value constant=");
    assertExpressional(RootCriteria.root.a.value().b.value().value.isEmpty(), "call op=EQUAL path=a.b.value constant=");
    assertExpressional(RootCriteria.root.a.value().b.value().c.isPresent(), "call op=IS_PRESENT path=a.b.c");

    assertExpressional(RootCriteria.root.a.value().b.value().c.value().value.isEqualTo("gem")
            , "call op=EQUAL path=a.b.c.value constant=gem");

    assertExpressional(RootCriteria.root.a.value().b.value().c.value().hidden.value().isEqualTo("gem")
            , "call op=EQUAL path=a.b.c.hidden constant=gem");

    assertExpressional(RootCriteria.root.a.value().b.value().c.value().hidden.value(s -> s.isEqualTo("gem"))
            , "call op=EQUAL path=a.b.c.hidden constant=gem");

    assertExpressional(RootCriteria.root.a.value(a -> a.b.isPresent()),
            "call op=IS_PRESENT path=a.b"
            );

    assertExpressional(RootCriteria.root.a.value(a -> a.b.value(b -> b.c.isPresent())),
            "call op=IS_PRESENT path=a.b.c"
    );

    assertExpressional(RootCriteria.root.a.value(a -> a.b.value().c.isPresent()),
            "call op=IS_PRESENT path=a.b.c"
            );

    assertExpressional(RootCriteria.root.a.value(a -> a.b.value(b -> b.c.value(c -> c.value.isEqualTo("gem")))),
            "call op=EQUAL path=a.b.c.value constant=gem"
    );

    assertExpressional(RootCriteria.root.a.value(a -> a.b.value(b -> b.c.value(c -> c.hidden.value(h -> h.isEqualTo("gem"))))),
            "call op=EQUAL path=a.b.c.hidden constant=gem"
    );

    assertExpressional(RootCriteria.root.a.value(a -> a.b.isPresent()),
            "call op=IS_PRESENT path=a.b"
            );

  }

  @Test
  public void composed() {
    assertExpressional(RootCriteria.root.a.value(a -> a.b.isPresent().b.isAbsent()),
            "call op=AND",
            "  call op=IS_PRESENT path=a.b",
            "  call op=IS_ABSENT path=a.b"
    );

  }

  @Ignore
  @Test
  public void debug() {
    assertExpressional(RootCriteria.root.a.value(a -> a.b.isPresent().or().b.value(b -> b.c.isPresent())),
            "call op=OR",
            "  call op=IS_PRESENT path=a.b",
            "  call op=IS_PRESENT path=a.b.c"
    );
  }

  private static void assertExpressional(Criterion<?> crit, String ... expectedLines) {
    final StringWriter out = new StringWriter();
    Query query = Criterias.toQuery(crit);
    query.filter().ifPresent(f -> f.accept(new DebugExpressionVisitor<>(new PrintWriter(out))));
    final String expected = Arrays.stream(expectedLines).collect(Collectors.joining(System.lineSeparator()));
    Assert.assertEquals(expected, out.toString().trim());
  }

}
