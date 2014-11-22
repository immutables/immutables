package org.immutables.value.processor.meta;

final class UnshadeGuava {
  private static final String GOOGLE_COMMON_PREFIX = "com.go".concat("ogle.common.");

  static String typeString(Object partiallyQualifiedClass) {
    return GOOGLE_COMMON_PREFIX + partiallyQualifiedClass;
  }
}
