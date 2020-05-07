package org.immutables.data;

import java.util.List;
import java.util.Optional;
import org.immutables.data.Datatype.Builder;
import org.immutables.data.Datatype.Feature;
import org.immutables.data.Datatype.Violation;
import org.immutables.data.Datatypes_Dtt.Dtt_;
import org.junit.Test;
import static org.immutables.check.Checkers.check;
import static org.immutables.data.Datatypes_Dtt.dtt;
import static org.immutables.data.Datatypes_Dtt.sin;

public class DataTest {
  @Test
  public void simpleBuild() {
    Dtt_ dtt = dtt();
    Builder<Dtt> b = dtt.builder();
    b.set(dtt.a_, 44);
    b.set(dtt.b_, "xyz");
    Dtt d = b.build();

    check((Object) dtt.get(dtt.feature(Dtt_.A_), d)).is(44);
    check((Object) dtt.get(dtt.feature(Dtt_.B_), d)).is("xyz");

    check(sin().builder().build()).same(ImmutableDtt.Sin.of());
  }

  @Test
  public void verifyAndBuild() {
    Dtt_ dtt = dtt();
    Builder<Dtt> b = dtt.builder();
    check(b.verify()).hasSize(2);

    try {
      b.build();
      check(false);
    } catch (IllegalStateException ok) {
    }

    b.set(dtt.a_, 44);
    b.set(dtt.b_, "xyz");

    check(b.verify()).isEmpty();
    check(b.build()).not().same(b.build());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void violationRules() {
    Dtt_ dtt = dtt();
    Builder<Dtt> b = dtt.builder();
    b.set((Feature<Dtt, Object>) dtt.feature("b"), 445);
    List<Violation> vs = b.verify();
    check(vs).hasSize(2);

    check(vs.get(0).rule()).is("required");
    check(vs.get(0).feature()).is(Optional.of(dtt.a_));

    check(vs.get(1).rule()).is("cast");
    check(vs.get(1).feature()).is(Optional.of(dtt.b_));
  }
}
