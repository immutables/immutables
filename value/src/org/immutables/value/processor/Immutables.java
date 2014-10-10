package org.immutables.value.processor;

import org.immutables.generator.Templates;
import org.immutables.value.processor.meta.DiscoveredValue;
import org.immutables.value.processor.meta.DiscoveredAttribute;
import org.immutables.generator.Generator;
import org.immutables.generator.AbstractTemplate;

@Generator.Template
abstract class Immutables extends AbstractTemplate {
  @Generator.Typedef
  DiscoveredValue Type;
  @Generator.Typedef
  DiscoveredAttribute Attribute;

  final String empty = "";
  final String staticPrefix = "static ";

  abstract Templates.Invokable generate();
  
  abstract Templates.Invokable immutableCollectionFrom();

  static Immutables create() {
    return new Generator_Immutables();
  }
  
  // abstract indexToBitmask
}
