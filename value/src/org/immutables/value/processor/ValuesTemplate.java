package org.immutables.value.processor;

import org.immutables.generator.AbstractTemplate;
import org.immutables.generator.Generator;
import org.immutables.generator.Templates;
import org.immutables.value.processor.meta.DiscoveredAttribute;
import org.immutables.value.processor.meta.DiscoveredValue;
import org.immutables.value.processor.meta.LongBits;

abstract class ValuesTemplate extends AbstractTemplate {
  @Generator.Typedef
  DiscoveredValue Type;
  @Generator.Typedef
  DiscoveredAttribute Attribute;

  @Generator.Typedef
  LongBits.LongPositions LongPositions;

  @Generator.Typedef
  LongBits.BitPosition BitPosition;

  public final LongBits longsFor = new LongBits();

  public abstract Templates.Invokable generate();
}
