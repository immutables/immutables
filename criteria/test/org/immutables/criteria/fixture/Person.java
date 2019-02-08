package org.immutables.criteria.fixture;

import com.google.common.base.Optional;
import org.immutables.criteria.Criteria;
import org.immutables.value.Value;

@Value.Immutable
@Criteria
public interface Person {

  String firstName();

  Optional<String> lastName();

  boolean isMarried();

  int age();

}
