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

    assertExpressional(RootCriteria.root.a.value().b.value().c.value().hidden.value().with(s -> s.isEqualTo("gem"))
            , "call op=EQUAL path=a.b.c.hidden constant=gem");

    assertExpressional(RootCriteria.root.a.value().with(a -> a.b.isPresent()),
            "call op=IS_PRESENT path=a.b"
            );

    assertExpressional(RootCriteria.root.a.value().with(a -> a.b.value().with(b -> b.c.isPresent())),
            "call op=IS_PRESENT path=a.b.c"
    );

    assertExpressional(RootCriteria.root.a.value().with(a -> a.b.value().c.isPresent()),
            "call op=IS_PRESENT path=a.b.c"
            );

    assertExpressional(RootCriteria.root.a.value().with(a -> a.b.value().with(b -> b.c.value().with(c -> c.value.isEqualTo("gem")))),
            "call op=EQUAL path=a.b.c.value constant=gem"
    );

    assertExpressional(RootCriteria.root.a.value().with(a -> a.b.value().with(b -> b.c.value().with(c -> c.hidden.value().with(h -> h.isEqualTo("gem"))))),
            "call op=EQUAL path=a.b.c.hidden constant=gem"
    );

    assertExpressional(RootCriteria.root.a.value().with(a -> a.b.isPresent()),
            "call op=IS_PRESENT path=a.b"
            );

  }

  @Test
  public void nested2() {
    assertExpressional(RootCriteria.root.a.isAbsent().a.value().with(a -> a.value.isEmpty()),
            "call op=AND",
            "  call op=IS_ABSENT path=a",
            "  call op=EQUAL path=a.value constant=");

    assertExpressional(RootCriteria.root.a.isAbsent().or().a.value().with(a -> a.value.isEmpty()),
            "call op=OR",
            "  call op=IS_ABSENT path=a",
            "  call op=EQUAL path=a.value constant=");

    assertExpressional(RootCriteria.root.a.value().with(a -> a.b.isAbsent().or().b.value().with(b -> b.c.value().value.isEmpty())),
            "call op=OR",
            "  call op=IS_ABSENT path=a.b",
            "  call op=EQUAL path=a.b.c.value constant=");
  }

  @Test
  public void composed() {
    assertExpressional(RootCriteria.root
                    .a.value().with(a -> a.value.isEqualTo("a").value.isEqualTo("b"))
                    .a.value().value.isEmpty()
            ,
            "call op=AND",
            "  call op=AND",
            "    call op=EQUAL path=a.value constant=a",
            "    call op=EQUAL path=a.value constant=b",
            "  call op=EQUAL path=a.value constant="
    );
    assertExpressional(RootCriteria.root
                    .a.value().with(a -> a.value.isEqualTo("a").value.isEqualTo("b"))
                    .a.value().with(a -> a.value.isEmpty().value.isNotEmpty())
            ,
            "call op=AND",
            "  call op=AND",
            "    call op=EQUAL path=a.value constant=a",
            "    call op=EQUAL path=a.value constant=b",
            "  call op=AND",
            "    call op=EQUAL path=a.value constant=",
            "    call op=NOT_EQUAL path=a.value constant="
    );
  }

  @Test
  public void xyz() {
    assertExpressional(RootCriteria.root
                    .x.value.isEmpty()
            ,
            "call op=EQUAL path=x.value constant="
    );

    assertExpressional(RootCriteria.root
                    .x.y.value.isEqualTo("a")
            ,
            "call op=EQUAL path=x.y.value constant=a"
    );

    assertExpressional(RootCriteria.root
                    .x.value.isEmpty()
                    .x.y.value.isEqualTo("b")
            ,
            "call op=AND",
            "  call op=EQUAL path=x.value constant=",
            "  call op=EQUAL path=x.y.value constant=b"
    );

    assertExpressional(RootCriteria.root
                    .x.value.isEmpty().or()
                    .x.y.value.isEmpty()
            ,
            "call op=OR",
            "  call op=EQUAL path=x.value constant=",
            "  call op=EQUAL path=x.y.value constant="
    );

    assertExpressional(RootCriteria.root
                    .x.value.isEmpty()
                    .x.y.value.isEmpty()
                    .x.y.z.value.isEmpty()
            ,
            "call op=AND",
            "  call op=EQUAL path=x.value constant=",
            "  call op=EQUAL path=x.y.value constant=",
            "  call op=EQUAL path=x.y.z.value constant="
    );

    assertExpressional(RootCriteria.root
                    .x.value.isEmpty().or()
                    .x.y.value.isEmpty().or()
                    .x.y.z.value.isEmpty()
            ,
            "call op=OR",
            "  call op=EQUAL path=x.value constant=",
            "  call op=EQUAL path=x.y.value constant=",
            "  call op=EQUAL path=x.y.z.value constant="
    );
  }

  /**
   * Combination of required and optional fields
   */
  @Test
  public void xyzAndAbc() {
    assertExpressional(RootCriteria.root
                    .x.value.isEmpty()
                    .a.value().value.isEqualTo("a")
            ,
            "call op=AND",
            "  call op=EQUAL path=x.value constant=",
            "  call op=EQUAL path=a.value constant=a"
    );

    assertExpressional(RootCriteria.root
                    .x.value.isEmpty().or()
                    .a.value().value.isEqualTo("a")
            ,
            "call op=OR",
            "  call op=EQUAL path=x.value constant=",
            "  call op=EQUAL path=a.value constant=a"
    );

    assertExpressional(RootCriteria.root
                    .a.value().value.isEqualTo("a")
                    .x.value.isEmpty()
            ,
            "call op=AND",
            "  call op=EQUAL path=a.value constant=a",
            "  call op=EQUAL path=x.value constant="
    );

  }

  @Ignore("doesn't return optional statement")
  @Test
  public void debug() {
    assertExpressional(RootCriteria.root.a.isAbsent().a.value().with(a -> a.value.isEmpty()),
            "call op=AND",
            "  call op=IS_ABSENT path=a",
            "  call op=EQUAL path=a.value constant=");
  }

  private static void assertExpressional(Criterion<?> crit, String ... expectedLines) {
    final StringWriter out = new StringWriter();
    final Query query = Criterias.toQuery(crit);
    query.filter().ifPresent(f -> f.accept(new DebugExpressionVisitor<>(new PrintWriter(out))));
    final String expected = Arrays.stream(expectedLines).collect(Collectors.joining(System.lineSeparator()));
    Assert.assertEquals(expected, out.toString().trim());
  }

}
