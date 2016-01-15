package org.immutables.builder.fixture;

import org.immutables.builder.Builder;

// Problem with this class was reproducible under eclipse compiler,
// here auxiliary copy
public class EclipseSwitchTest {
  public enum EnumForSwitch {
    YES, NO
  }

  @Builder.Factory
  public static String factoryMethod(@Builder.Switch(defaultName = "YES") EnumForSwitch val) {
    return val.name();
  }
}
