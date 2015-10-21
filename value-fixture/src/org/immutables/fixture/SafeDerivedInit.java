package org.immutables.fixture;

import org.immutables.value.Value;

public interface SafeDerivedInit {

  @Value.Immutable
  @Value.Style(defaultAsDefault = true)
  interface SafeInitIface {
    default int b() {
      return c();
    }

    default int a() {
      return b();
    }

    default int c() {
      return a();
    }
  }

  @Value.Immutable(singleton = true)
  interface SafeInitSingl {
    @Value.Default
    default int a() {
      return 1;
    }

    @Value.Derived
    default int b() {
      return a() + 1;
    }
  }

  @Value.Immutable
  @Value.Style(unsafeDefaultAndDerived = true)
  abstract class SafeInitAclass {
    @Value.Default
    int a() {
      return b();
    }

    @Value.Default
    int b() {
      return c();
    }

    @Value.Default
    int c() {
      return a();
    }
  }
}
