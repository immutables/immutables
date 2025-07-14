package org.immutables.fixture.datatype;

import java.util.Set;
import org.junit.Test;
import static org.immutables.check.Checkers.check;

// These are tests to see how records works with datatype descriptors
// rest of tests are in {@code datatype} module
public class DataTest {
  @Test public void cases() {
    var someTypes = Datatypes_SomeTypes._SomeTypes();
    var absVal = Datatypes_SomeTypes._AbsVal();
    var recAbc = Datatypes_SomeTypes._RecAbc();

    check(!someTypes.isInstantiable());
    check(someTypes.cases()).is(Set.of(absVal, recAbc));
    check(absVal.isInstantiable());
    check(recAbc.isInstantiable());
  }

  // can use abstract builder to build records as we can
  @Test public void buildRecord() {
    var recAbc = Datatypes_SomeTypes._RecAbc();

    var builder = recAbc.builder();
    builder.set(recAbc.a_, 13);
    builder.set(recAbc.b_, "BBB");
    builder.set(recAbc.c_, true);

    var rec = builder.build();

    check(rec).is(new SomeTypes.RecAbc(13, "BBB", true));
  }
}
