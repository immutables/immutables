package org.immutables.value.processor.meta;

final class UnshadeGuava {
  private static final String GOOGLE_COMMON_PREFIX = "com.go".concat("ogle.common.");

  static String typeString(String partiallyQualifiedType) {
    return GOOGLE_COMMON_PREFIX + partiallyQualifiedType;
  }
}
