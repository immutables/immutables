package org.immutables.criteria;

import com.google.common.base.Optional;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@Criteria
public interface Person {

  String firstName();

  Optional<String> lastName();

  boolean isMarried();

  int age();

  Friend bestFriend();

  List<Friend> friends();

}
