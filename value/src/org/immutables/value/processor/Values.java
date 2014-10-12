package org.immutables.value.processor;

import com.google.common.collect.ImmutableList;
import org.immutables.generator.AbstractTemplate;
import org.immutables.generator.Generator;
import org.immutables.value.processor.meta.DiscoveredValue;
import org.immutables.value.processor.meta.Discovery;
import java.util.List;

@Generator.Template
class Values extends AbstractTemplate {

  Immutables immutables = new Generator_Immutables();

  Modifiables modifiables = new Generator_Modifiables();

  Parboileds parboileds = new Generator_Parboileds();

  Transformers transformers = new Generator_Transformers();

  private List<DiscoveredValue> type;

  public List<DiscoveredValue> types() {
    return type != null ? type : (type = new Discovery(
        processing(),
        round(),
        annotations()).discover());
  }

  private List<DiscoveredValue> linearizedTypes;

  List<DiscoveredValue> linearizedTypes() {
    if (linearizedTypes == null) {
      ImmutableList.Builder<DiscoveredValue> builder = ImmutableList.builder();
      for (DiscoveredValue type : types()) {
        if (!type.isEmptyNesting()) {
          builder.add(type);
        }
        if (type.isHasNestedChildren()) {
          for (DiscoveredValue nestedType : type.getNestedChildren()) {
            builder.add(nestedType);
          }
        }
      }
      linearizedTypes = builder.build();
    }
    return linearizedTypes;
  }

}
