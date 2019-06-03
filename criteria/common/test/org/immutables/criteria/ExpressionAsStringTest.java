package org.immutables.criteria;

import org.immutables.criteria.expression.DebugExpressionVisitor;
import org.junit.Assert;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Tests that expression is built correctly by "serializing" it to string
 */
public class ExpressionAsStringTest {

  @Test
  public void string() {
    PersonCriteria<PersonCriteria.Self> crit = PersonCriteria.create();

    assertExpressional(crit.lastName.isPresent(), "call op=IS_PRESENT path=lastName");
    assertExpressional(crit.lastName.isAbsent(), "call op=IS_ABSENT path=lastName");
    assertExpressional(crit.lastName.value().isEqualTo("aaa"), "call op=EQUAL path=lastName constant=aaa");
    assertExpressional(crit.lastName.value(f -> f.isEqualTo("bbb")), "call op=EQUAL path=lastName constant=bbb");
    assertExpressional(crit.firstName.isIn("n1", "n2"), "call op=IN path=firstName constant=[n1, n2]");

    assertExpressional(crit.firstName.isEqualTo("John").or().firstName.isEqualTo("Marry"),
            "call op=OR",
                    "  call op=EQUAL path=firstName constant=John",
                    "  call op=EQUAL path=firstName constant=Marry");

    assertExpressional(crit.bestFriend.nickName.isEqualTo("John"),
                    "call op=EQUAL path=bestFriend.nickName constant=John");

  }

  @Test
  public void not() {
    PersonCriteria<PersonCriteria.Self> crit = PersonCriteria.create();

    assertExpressional(crit.firstName.not(n -> n.isEqualTo("John")),
            "call op=NOT",
                    "  call op=EQUAL path=firstName constant=John");

    assertExpressional(crit.not(f -> f.firstName.isEqualTo("John").lastName.isPresent()),
            "call op=NOT",
                    "  call op=AND",
                    "    call op=EQUAL path=firstName constant=John",
                    "    call op=IS_PRESENT path=lastName");

  }

  private static void assertExpressional(DocumentCriteria<?> crit, String ... expectedLines) {
    final StringWriter out = new StringWriter();
    Criterias.toExpressional(crit).expression().accept(new DebugExpressionVisitor<>(new PrintWriter(out)));
    final String expected = Arrays.stream(expectedLines).collect(Collectors.joining(System.lineSeparator()));
    Assert.assertEquals(expected, out.toString().trim());
  }

}
