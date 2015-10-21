package org.immutables.fixture.modifiable;

import static org.immutables.check.Checkers.*;
import org.junit.Test;

public class ModifiablesTest {

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

    check(!c1.integerIsSet());
    c1.setInteger(1);
    check(c1.integerIsSet());

    check(!c1.stringIsSet());
    c1.setString("_");
    check(c1.stringIsSet());

    check(c1.isInitialized());
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
  }
}
