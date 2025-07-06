package org.immutables.datatype;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.immutables.datatype.Datatype.Builder;
import org.immutables.datatype.Datatype.Feature;
import org.immutables.datatype.Datatype.Violation;
import org.immutables.datatype.Datatypes_Dtt.Dtt_;
import org.immutables.datatype.Datatypes_Maybe.Maybe_;
import org.junit.Test;
import static org.immutables.check.Checkers.check;
import static org.immutables.datatype.Datatypes_Dtt._Dtt;
import static org.immutables.datatype.Datatypes_Dtt._Ign;
import static org.immutables.datatype.Datatypes_Dtt._Inl;
import static org.immutables.datatype.Datatypes_Dtt._Really;
import static org.immutables.datatype.Datatypes_Dtt._Sin;
import static org.immutables.datatype.Datatypes_Maybe._Maybe;

public class DataTest {
  @Test
  public void simpleBuild() {
    Dtt_ dtt = _Dtt();
    Builder<Dtt> b = dtt.builder();
    b.set(dtt.a_, 44);
    b.set(dtt.b_, "xyz");
    Dtt d = b.build();

    check((Object) dtt.get(dtt.feature(Dtt_.A_), d)).is(44);
    check((Object) dtt.get(dtt.feature(Dtt_.B_), d)).is("xyz");

    check(_Sin().builder().build()).same(ImmutableDtt.Sin.of());
  }

  @Test
  public void verifyAndBuild() {
    Dtt_ dtt = _Dtt();
    Builder<Dtt> b = dtt.builder();
    check(b.verify()).hasSize(1);

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
    Dtt_ dtt = _Dtt();
    Builder<Dtt> b = dtt.builder();
    b.set((Feature<Dtt, Object>) dtt.feature("b"), 445);
    List<Violation> vs = b.verify();
    check(vs).hasSize(2);

    check(vs.get(0).rule()).is("required");
    check(vs.get(0).feature()).is(Optional.of(dtt.a_));

    check(vs.get(1).rule()).is("cast");
    check(vs.get(1).feature()).is(Optional.of(dtt.b_));
  }

  @Test
  public void isInline() {
    check(_Inl().isInline());
  }

  @Test
  public void isIgnorable() {
    check(_Ign().g_.ignorableOnOutput());
    check(_Ign().g_.omittableOnInput());
  }

  @Test
  public void maybeGenerated() {
    // check only generated fact and structure here
    Maybe_<String> may = _Maybe(String.class);

    check(!may.isInstantiable());
    check(may.cases()).hasSize(2);
  }

  @Test public void reallyComplexGenerics() {
    Datatypes_Dtt.Really_<Double, Short> r = _Really(Double.class, Short.class);
    check(r.a_.type()).is(Types.newParameterized(Optional.class, Double.class));
    check(r.b_.type()).is(String.class);
    check(r.i_.type()).is(int.class);
    check(r.v_.type()).is(String.class);
    check(r.y_.type()).is(Short.class);
    check(r.d_.type()).is(Types.newParameterized(List.class, String.class));
    check(r.z_.type()).is(Types.newParameterized(Set.class, String.class));
    check(r.u_.type()).is(Types.newParameterized(List.class, Void.class));
  }
}
