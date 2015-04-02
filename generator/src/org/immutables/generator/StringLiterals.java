/*
    Copyright 2014 Immutables Authors and Contributors
    Copyright (C) 2009 The Guava Authors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.generator;

import com.google.common.collect.ImmutableMap;
import com.google.common.escape.ArrayBasedCharEscaper;
import com.google.common.escape.Escaper;

/**
 * String literals.
 */
public final class StringLiterals {
  private StringLiterals() {}

  public static Escaper escaper() {
    return ESCAPER;
  }

  private static final Escaper ESCAPER = new ArrayBasedCharEscaper(
      ImmutableMap.<Character, String>builder()
          .put('\b', "\\b")
          .put('\"', "\\\"")
          .put('\'', "\\'")
          .put('\\', "\\\\")
          .put('\f', "\\f")
          .put('\n', "\\n")
          .put('\r', "\\r")
          .put('\t', "\\t")
          .build(), ' ',/* 0x20 */'~'/* 0x7E */) {

    final char[] hex = "0123456789abcdef".toCharArray();

    @Override
    protected char[] escapeUnsafe(char c) {
      char[] result = new char[6];
      result[0] = '\\';
      result[1] = 'u';
      result[5] = hex[c & 0xF];
      c >>>= 4;
      result[4] = hex[c & 0xF];
      c >>>= 4;
      result[3] = hex[c & 0xF];
      c >>>= 4;
      result[2] = hex[c & 0xF];
      return result;
    }
  };

  public static String toLiteral(char character) {
    return "'" + escaper().escape("" + character) + "'";
  }

  public static String toLiteral(String string) {
    return "\"" + escaper().escape(string) + "\"";
  }
}
