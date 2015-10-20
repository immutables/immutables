package org.immutables.fixture.generatorext;

final class Preconditions {
  static <T> T checkNotNull(T instance) {
    if (instance == null) {
      throw new IllegalArgumentException();
    }
    return instance;
  }

  /**
   * @param instance
   * @param message
   */
  static <T> T checkNotNull(T instance, String message) {
    return checkNotNull(instance);
  }
}
