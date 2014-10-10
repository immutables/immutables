package org.immutables.value.processor;

import java.util.List;
import org.immutables.generator.AbstractTemplate;
import org.immutables.generator.Generator;
import org.immutables.value.processor.meta.DiscoveredValue;
import org.immutables.value.processor.meta.Discovery;

@Generator.Template
class Values extends AbstractTemplate {

  Immutables immutables = Immutables.create();

  List<DiscoveredValue> discoveredTypes() {
    return new Discovery(
        processing(),
        round(),
        annotations()).discover();
  }
}
