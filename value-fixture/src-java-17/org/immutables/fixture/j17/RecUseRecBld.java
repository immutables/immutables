package org.immutables.fixture.j17;

import org.immutables.value.Value;

@Value.Style(attributeBuilderDetection = true)
@Value.Builder
record Usbld(String aa, int bb) {}

@Value.Style(attributeBuilderDetection = true)
@Value.Builder
record RecUseRecBld(int cc, Usbld rec) {
}

@Value.Style(attributeBuilderDetection = true)
@Value.Immutable
interface Useit {
  String aa();
  int bb();
}

@Value.Style(attributeBuilderDetection = true)
@Value.Immutable
interface PlainImm {
  RecUseRecBld rc();
  Usbld ub();
  Useit it();
}
