package org.immutables.fixture.defaults;

import org.immutables.value.Value;
import org.immutables.value.Value.Default;

@Value.Immutable
public interface AllDefaults {

  @Default.Boolean(false)
  boolean aBoolean();

  @Default.Char('a')
  char aChar();

  @Default.Double(1.0)
  double aDouble();

  @Default.Float(0.1f)
  float aFloat();

  @Default.Int(0)
  int aInt();

  @Default.Long(1)
  long aLong();

  @Default.String("foo")
  String aString();
}
