package org.immutables.fixture.with;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.immutables.check.Checkers.check;

public class WithAllowNullsMapTest {
  @Test void allowNulls() {
    WithAllowsNullMap obj = ImmutableWithAllowsNullMap.builder().build();
    Map<String, Object> map = new LinkedHashMap<>();
    map.put(null, null);
    map.put("A", null);
    map.put("B", 2);
    WithAllowsNullMap withMap = obj.withMap(Collections.unmodifiableMap(map));

    List<Object> list = Arrays.asList(null, 1, null, "C");
    WithAllowsNullMap withList = obj.withList(list);

    Map<String, Object> copiedMap = new HashMap<>(map);

    check(withMap.map()).is(copiedMap);
    check(withList.list()).is(list);
  }
}
