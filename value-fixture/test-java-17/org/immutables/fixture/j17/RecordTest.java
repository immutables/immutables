package org.immutables.fixture.j17;

import java.util.List;
import org.junit.Test;
import static org.immutables.check.Checkers.check;

public class RecordTest {
  @Test public void outsideBuilder() {
    var recordOne = new RecordOneBuilder()
        .aa(1)
        .bb("22")
        .build();

    check(recordOne.aa()).is(1);
    check(recordOne.bb()).is("22");
  }

  @Test public void enclosedBuilder() {
    var one = Recss.oneBuilder()
        .aaa(3)
        .build();

    check(one.aaa()).is(3);

    var two = Recss.twoBuilder()
        .bbb("44")
        .build();

    check(two.bbb()).is("44");
  }

  @Test(expected = IllegalStateException.class)
  public void expectedMissing() {
    Recss.oneBuilder().build();
  }

  @Test public void withers() {
    var ints = List.of(1, 2, 3);
    var a = new RecordWitherOne(1, "A", ints);

    var two = a.withAa(2);
    var b = a.withBb("B");

    check(a).not().same(b);
    check(a).not().same(two);

    check(two.aa()).is(2);
    check(b.bb()).is("B");

    check(a.withAa(3).withBb("C")).is(new RecordWitherOne(3, "C", ints));

    check(a.withAa(aa -> aa + 1)
        .withBb(bb -> "[" + bb + "]")
        .withInts(i -> i * i))
        .is(new RecordWitherOne(2, "[A]", List.of(1, 4, 9)));
  }

  @Test public void genericRecord() {
    new RecordGenericBuilder<Integer, String>()
        .aa(1)
        .bb("2")
        .build();
  }

  @Test public void factoryBuilderWithDefault() {
    var result = Recss.factoryBuilder()
        .left("A")
        .right("1")
        .build();
    check(result).is("A::1");
  }

  @Test public void builderInBuilder() {
    // TODO using nested builder
    var aaa = Recss.anotherBuilder()
        .buildme(new Recs.One(1))
        .andme(new Recs.Two("AAA"))
        .build();

    check(aaa.buildme().aaa()).is(1);
    check(aaa.andme().bbb()).is("AAA");
  }

  @Test public void builderAttribute() {
    var rec = new RecUseRecBldBuilder()
        .cc(1)
        .rec(b -> b.aa("A").bb(2))
        .build();

    check(rec.rec().aa()).is("A");

    var bld = ImmutablePlainImm.builder()
        .it(a -> a.aa("A").bb(12))
        .rc(b -> b.cc(2).rec(z -> z.bb(3).aa("AA")));

    bld.ubBuilder()
        .aa("BB")
        .bb(2);

    var plm = bld.build();

    check(plm.rc().rec().bb()).is(3);
    check(plm.ub().aa()).is("BB");
  }

  @Test public void defaultsInExtendingBuilder() {
    var bbb = new ExtBilly.Builder()
        .b("BBB");

    check(!bbb.isASet());
    check(bbb.isBSet());
    check(bbb.isCSet());

    // `a` is set inside overridden build
    var billy = bbb.build();
    check(billy.a()).is(14);
    check(billy.b()).is("BBB");
    check(!billy.c());
  }

  @Test public void constantDefaults() {
    var r = new RecordDefaultsBuilder()
        .required("req")
        .build();

    check(r.a()).is(42);
    check(r.b()).is("ABC");
    check(r.d()).is(1.5);
    check(r.l()).is(100L);
    check(r.ll()).is(-100L);
  }

  @Test public void constantDefaultsAllOverridden() {
    var r = new RecordDefaultsBuilder()
        .required("req2")
        .a(13)
        .b("XYZ")
        .l(1L)
        .ll(-1L)
        .d(1.16)
        .build();

    check(r.a()).is(13);
    check(r.b()).is("XYZ");
    check(r.d()).is(1.16);
    check(r.l()).is(1L);
    check(r.ll()).is(-1L);
  }

  @Test public void stagedBuilder() {
    var rec = RecordStaged.builder()
        .a(1)
        .b("B")
        .c("C")
        .build();

    check(rec).hasToString("RecordStaged[a=1, b=B, c=C]");
  }

  @Test public void genericStagedBuilder() {
    var rec = RecordGenericStaged.<String, Long>alsoStaged()
        .a(2)
        .b("B")
        .c(11L)
        .build();

    check(rec).hasToString("RecordGenericStaged[a=2, b=B, c=11]");
  }

  @Test public void enclosedStagedBuilder() {
    var rec1 = ImmutableEnclosedStaged.rec1Builder()
        .a(1)
        .b("B")
        .c("C")
        .build();

    check(rec1).hasToString("Rec1[a=1, b=B, c=C]");

    var rec2 = ImmutableEnclosedStaged.rec2Builder()
        .aa(11)
        .build();

    check(rec2).hasToString("Rec2[aa=11]");
  }
}
