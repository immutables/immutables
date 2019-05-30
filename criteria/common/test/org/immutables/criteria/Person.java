package org.immutables.criteria;

import org.immutables.value.Value;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Value.Immutable
@Criteria
public interface Person {

  String firstName();

  Optional<String> lastName();

  Optional<LocalDate> dateOfBirth();

  boolean isMarried();

  int age();

  Friend bestFriend();

  List<Friend> friends();

  List<String> aliases();

}
