package org.immutables.generator;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map.Entry;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class SourceTypeTest {
  @Test
  public void extract() {
    Entry<String, List<String>> sourceTypes = SourceTypes.extract("Map<String<X>,  Map<Int, B>>");

    check(sourceTypes.getKey()).is("Map");
    check(sourceTypes.getValue()).isOf("String<X>", "Map<Int, B>");
  }

  public void stringify() {
    Entry<String, List<String>> entry =
        Maps.<String, List<String>>immutableEntry("Map",
            ImmutableList.of("String<X>", "Map<Int, B>"));

    check(SourceTypes.stringify(entry)).is("Map<String<X>,  Map<Int, B>>");
  }
}
