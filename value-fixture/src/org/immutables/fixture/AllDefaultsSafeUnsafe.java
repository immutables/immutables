package org.immutables.fixture;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(unsafeDefaultAndDerived = true)
public interface AllDefaultsSafeUnsafe {

  @Value.Default.Boolean(false)
  boolean aBoolean();

  @Value.Default.Char('a')
  char aChar();

  @Value.Default.Double(1.0)
  double aDouble();

  @Value.Default.Float(0.1f)
  float aFloat();

  @Value.Default.Int(0)
  int aInt();

  @Value.Default.Long(1)
  long aLong();

  @Value.Default.String("foo")
  String aString();
}
