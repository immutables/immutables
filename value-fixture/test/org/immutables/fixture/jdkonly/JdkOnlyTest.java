package org.immutables.fixture.jdkonly;

import com.google.common.collect.ImmutableMap;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class JdkOnlyTest {
  @Test
  public void singleton() {
    check(ImmutableJdkColl.of()).same(ImmutableJdkColl.builder().build());
    check(ImmutableJdkColl.of()).same(ImmutableJdkColl.builder().build());
  }

  @Test
  public void collections() {
    ImmutableJdkColl coll = ImmutableJdkColl.builder()
        .addInts(1)
        .addInts(2, 3)
        .addAllInts(Arrays.asList(4, 5, 6))
        .addNavs(1, 2, 3)
        .addOrds(4, 6, 5)
        .addAllOrds(Arrays.asList(8, 7, 9))
        .addPols(RetentionPolicy.RUNTIME, RetentionPolicy.RUNTIME)
        .build();

    check(coll.ints()).isOf(1, 2, 3, 4, 5, 6);
    check(coll.navs()).isOf(3, 2, 1);
    check(coll.ords()).isOf(4, 5, 6, 7, 8, 9);
    check(coll.pols()).isOf(RetentionPolicy.RUNTIME);
  }

  @Test
  public void modify() {
    ImmutableJdkColl coll = ImmutableJdkColl.builder()
        .addInts(1)
        .addOrds(4, 6, 5)
        .addAllOrds(Arrays.asList(8, 7, 9))
        .addPols(RetentionPolicy.RUNTIME, RetentionPolicy.RUNTIME)
        .build();

    try {
      coll.ints().add(1);
      check(false);
    } catch (UnsupportedOperationException ex) {
    }
    try {
      coll.navs().add(1);
      check(false);
    } catch (UnsupportedOperationException ex) {
    }
    try {
      coll.pols().add(RetentionPolicy.CLASS);
      check(false);
    } catch (UnsupportedOperationException ex) {
    }
  }

  @Test
  public void collectionNulls() {
    try {
      ImmutableJdkColl.builder().addPols((RetentionPolicy) null).build();
      check(false);
    } catch (NullPointerException ex) {
    }
    try {
      ImmutableJdkColl.builder().addAllNavs(null).build();
      check(false);
    } catch (NullPointerException ex) {
    }
    try {
      ImmutableJdkColl.builder().addStr(null, null).build();
      check(false);
    } catch (NullPointerException ex) {
    }
  }

  @Test
  public void maps() {
    JdkMaps maps = JdkMapsBuilder.builder()
        .putJust(1, -1)
        .putJust(2, -2)
        .putNavs("22", 2)
        .putNavs("33", 3)
        .putAllOrds(ImmutableMap.of(2, "2", 1, "1"))
        .build();

    check(maps.navs().keySet()).isOf("33", "22");
    check(maps.just().keySet()).isOf(1L, 2L);
    check(maps.ords().keySet()).isOf(1, 2);
  }

  @Test
  public void mapNulls() {
    try {
      JdkMapsBuilder.builder().putPols((RetentionPolicy) null, 1).build();
      check(false);
    } catch (NullPointerException ex) {
    }
    try {
      JdkMapsBuilder.builder().putJust(null).build();
      check(false);
    } catch (NullPointerException ex) {
    }
    try {
      JdkMapsBuilder.builder().putAllNavs(Collections.singletonMap(null, null)).build();
      check(false);
    } catch (NullPointerException ex) {
    }
  }
}
