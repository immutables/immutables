package org.immutables.metainf.fixture;

import com.google.common.base.Functions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import java.io.Serializable;
import java.util.ServiceLoader;
import java.util.TreeSet;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class ServiceTest {

  @Test
  public void serviceSets() {
    check(sortedToStringsFrom(ServiceLoader.load(Runnable.class))).isOf("Otherserv", "Servrun");
    check(sortedToStringsFrom(ServiceLoader.load(Serializable.class))).isOf("Serserv");
  }

  private TreeSet<String> sortedToStringsFrom(Iterable<?> iterable) {
    return Sets.newTreeSet(FluentIterable.from(iterable).transform(Functions.toStringFunction()));
  }
}
