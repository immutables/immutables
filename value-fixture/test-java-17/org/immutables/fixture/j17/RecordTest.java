package org.immutables.fixture.j17;

import java.util.List;
import org.junit.Ignore;
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
}
