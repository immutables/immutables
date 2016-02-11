package org.immutables.fixture.style;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.immutables.value.Value;
import org.immutables.value.Value.Style;

/**
 * Compilation test to demonstrate generated builder methods
 * according to logic and examples from {@link Style#depluralize()} javadoc.
 */
@Value.Immutable
@Value.Style(depluralize = {"foot:feet", "person:people", "goods"})
public interface Depluralize {

  List<String> feet();

  Set<String> boats();

  Map<String, String> people();

  Multimap<String, String> peopleRepublics();

  List<Integer> feetPeople();

  Multiset<Boolean> goods();

  default void use() {
    ImmutableDepluralize.builder()
        .addBoat("") // automatically trims s
        .addFoot("") // applies "foot:feet"
        .addGoods(true) // used literally due to exception "goods"
        .addFeetPerson(1) // last segment converted using "person:people"
        .putPerson("", "") // converted using "person:people"
        .putPeopleRepublic("", "") // last segment s auto-trimmed
        .build();
  }
}
