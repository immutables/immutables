package org.immutables.value.processor;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimaps;
import org.immutables.generator.Generator;
import org.immutables.value.processor.meta.DiscoveredValue;

import java.util.Collection;
import java.util.Map;

@Generator.Template
abstract class Marshalers extends ValuesTemplate {

  int roundCode() {
    return System.identityHashCode(round());
  }

  final Function<Iterable<DiscoveredValue>, Iterable<DiscoveredValue>> onlyMarshaled =
      new Function<Iterable<DiscoveredValue>, Iterable<DiscoveredValue>>() {
        @Override
        public Iterable<DiscoveredValue> apply(Iterable<DiscoveredValue> input) {
          ImmutableList.Builder<DiscoveredValue> builder = ImmutableList.builder();
          for (DiscoveredValue value : input) {
            if (value.isGenerateMarshaled()) {
              builder.add(value);
            }
          }
          return builder.build();
        }
      };

  final ByPackageGrouper byPackage = new ByPackageGrouper();

  class ByPackageGrouper
      implements Function<Iterable<DiscoveredValue>, Iterable<Map.Entry<String, Collection<DiscoveredValue>>>> {

    @Override
    public Iterable<Map.Entry<String, Collection<DiscoveredValue>>> apply(
        Iterable<DiscoveredValue> discoveredValue) {
      return Multimaps.index(discoveredValue, new Function<DiscoveredValue, String>() {
        @Override
        public String apply(DiscoveredValue input) {
          return input.getPackageName();
        }
      }).asMap().entrySet();
    }
  }

  @Generator.Typedef
  Map.Entry<String, Collection<DiscoveredValue>> ByPackage;
}
