package org.immutables.value.processor.meta;

import com.google.common.collect.ImmutableSet;

class Keywords {
  private static final ImmutableSet<String> keywords = ImmutableSet.of(
      "abstract", "assert", "boolean",
      "break", "byte", "case", "catch", "char", "class", "const",
      "continue", "default", "do", "double", "else", "extends", "false",
      "final", "finally", "float", "for", "goto", "if", "implements",
      "import", "instanceof", "int", "interface", "long", "native",
      "new", "null", "package", "private", "protected", "public",
      "return", "short", "static", "strictfp", "super", "switch",
      "synchronized", "this", "throw", "throws", "transient", "true",
      "try", "void", "volatile", "while");

  static boolean is(String identifier) {
    return keywords.contains(identifier);
  }

  static String safeIdentifier(String identifier, String fallback) {
    return is(identifier) ? fallback : identifier;
  }
}
