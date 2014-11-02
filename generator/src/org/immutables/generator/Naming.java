/*
    Copyright 2014 Ievgen Lukash

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

import com.google.common.base.CharMatcher;
import com.google.common.base.Ascii;
import com.google.common.base.CaseFormat;
import com.google.common.base.Function;
import com.google.common.base.Splitter;
import java.util.List;
import static com.google.common.base.Preconditions.*;

/**
 * Converter-like function to apply or extract naming
 */
public abstract class Naming implements Function<String, String> {
  private Naming() {}

  private static final String NOT_DETECTED = "";
  private static final String NAME_PLACEHOLDER = "*";
  private static final Splitter TEMPLATE_SPLITTER = Splitter.on(NAME_PLACEHOLDER);
  private static final CharMatcher TEMPLATE_CHAR_MATCHER =
      CharMatcher.JAVA_LETTER_OR_DIGIT.or(CharMatcher.is('*')).precomputed();

  /**
   * Applies naming to input identifier, converting it to desired naming.
   * @param input the input identifier
   * @return applied naming
   */
  @Override
  public abstract String apply(String input);

  /**
   * Tries to extract source identifier name out of already applied naming.
   * @param identifier to detect naming from
   * @return empty string if nothing detected
   */
  public abstract String detect(String identifier);

  /**
   * @param template template string
   * @return naming that could be applied or detected following template
   */
  public static Naming from(String template) {
    if (template.isEmpty()) {
      template = "*";
    }
    List<String> parts = TEMPLATE_SPLITTER.splitToList(template);
    checkArgument(parts.size() <= 2 && TEMPLATE_CHAR_MATCHER.matchesAllOf(template),
        "Wrong naming template: %s. Shoud be {prefix?}*{suffix?} where prefix and suffix optional identifier parts",
        template);

    return parts.size() == 1
        ? new VerbatimNaming(template)
        : new PrefixSuffixNaming(parts.get(0), parts.get(1));
  }

  /**
   * Verbatim naming convention do not use any supplied input name as base.
   * Consider example factory method "from", it used as {@link VerbatimNaming},
   * contrary to the factory method "newMyType" uses "MyType" as and input applying "new" prefix.
   */
  private static class VerbatimNaming extends Naming {
    final String name;

    VerbatimNaming(String name) {
      this.name = name;
    }

    @Override
    public String apply(String input) {
      return name;
    }

    @Override
    public String detect(String identifier) {
      return identifier.equals(name) ? name : NOT_DETECTED;
    }

    @Override
    public String toString() {
      return Naming.class.getSimpleName() + ".from(" + name + ")";
    }
  }

  private static class PrefixSuffixNaming extends Naming {
    final String prefix;
    final String suffix;

    PrefixSuffixNaming(String prefix, String suffix) {
      this.prefix = prefix;
      this.suffix = suffix;
    }

    @Override
    public String apply(String input) {
      CaseFormat resultFormat = prefix.isEmpty()
          ? CaseFormat.LOWER_CAMEL
          : CaseFormat.UPPER_CAMEL;

      return prefix + CaseFormat.LOWER_CAMEL.to(resultFormat, input) + suffix;
    }

    @Override
    public String detect(String identifier) {
      if (identifier.length() <= suffix.length() + prefix.length()) {
        return NOT_DETECTED;
      }

      boolean prefixMatches = prefix.isEmpty() ||
          (identifier.startsWith(prefix) && Ascii.isUpperCase(identifier.charAt(prefix.length())));

      boolean suffixMatches = suffix.isEmpty() || identifier.endsWith(suffix);

      if (prefixMatches && suffixMatches) {
        String detected = identifier.substring(prefix.length(), identifier.length() - suffix.length());
        return prefix.isEmpty()
            ? detected
            : CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, detected);
      }

      return NOT_DETECTED;
    }

    @Override
    public String toString() {
      return Naming.class.getSimpleName() + ".from(" + prefix + "*" + suffix + ")";
    }
  }
}
