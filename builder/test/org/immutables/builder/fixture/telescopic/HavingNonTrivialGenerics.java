package org.immutables.builder.fixture.telescopic;

import java.math.BigDecimal;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(stagedBuilder = true)
public interface HavingNonTrivialGenerics<T extends Comparable<? super T>> extends Comparable<HavingNonTrivialGenerics<T>> {

  T key();

  BigDecimal a();

  int b();

  @Override
  default int compareTo(HavingNonTrivialGenerics<T> other) {
    return key().compareTo(other.key());
  }
}
