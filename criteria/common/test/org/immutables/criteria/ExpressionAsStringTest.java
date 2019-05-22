package org.immutables.criteria;

import org.immutables.criteria.constraints.DebugExpressionVisitor;
import org.immutables.criteria.constraints.Expressional;
import org.junit.Assert;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Tests that expression is built correctly by "serializing" it to string
 */
public class ExpressionAsStringTest {

  @Test
  public void string() {
    PersonCriteria<PersonCriteria.Self> crit = PersonCriteria.create();

    assertExpressional(crit.lastName.isPresent(), "call op=IS_PRESENT path=lastName");
    assertExpressional(crit.lastName.isAbsent(), "call op=IS_ABSENT path=lastName");
    assertExpressional(crit.lastName.value().isEqualTo("aaa"), "call op=EQUAL path=lastName literal=aaa");
    assertExpressional(crit.lastName.value(f -> f.isEqualTo("bbb")), "call op=EQUAL path=lastName literal=bbb");
    assertExpressional(crit.firstName.isIn("n1", "n2"), "call op=IN path=firstName literal=[n1, n2]");

    assertExpressional(crit.firstName.isEqualTo("John").or().firstName.isEqualTo("Marry"),
            "call op=OR\n" +
                    "  call op=EQUAL path=firstName literal=John\n" +
                    "  call op=EQUAL path=firstName literal=Marry");
  }

  private static void assertExpressional(Expressional<?> expressional, String expected) {
    final StringWriter out = new StringWriter();
    expressional.expression().accept(new DebugExpressionVisitor<>(new PrintWriter(out)));
    Assert.assertEquals(out.toString().trim(), expected);
  }

}
