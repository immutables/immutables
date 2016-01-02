/*
    Copyright 2014 Immutables Authors and Contributors

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

import com.google.common.base.Ascii;
import com.google.common.base.CaseFormat;
import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import java.util.List;
import static com.google.common.base.Preconditions.*;

/**
 * Converter-like function to apply or extract naming, derived from input.
 */
public abstract class Naming implements Function<String, String> {
  private Naming() {}

  private static final String NOT_DETECTED = "";
  private static final String NAME_PLACEHOLDER = "*";
  private static final Splitter TEMPLATE_SPLITTER = Splitter.on(NAME_PLACEHOLDER);
  private static final CharMatcher TEMPLATE_CHAR_MATCHER =
      CharMatcher.is('_')
          .or(CharMatcher.is(NAME_PLACEHOLDER.charAt(0)))
          .or(CharMatcher.inRange('a', 'z'))
          .or(CharMatcher.inRange('A', 'Z'))
          .or(CharMatcher.inRange('0', '9'))
          .precomputed();

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
   * Checks if it's identity naming.
   * @see #identity()
   * @return true, if is identity naming
   */
  public abstract boolean isIdentity();

  /**
   * Checks if is constant naming.
   * Verbatim naming convention do not use any supplied input name as base.
   * Consider example factory method "from" constant naming,
   * contrary to the factory method "newMyType" uses "MyType" as and input applying "new" prefix.
   * @return true, if is constant
   */
  public abstract boolean isConstant();

  /**
   * Returns non-contant naming which is this. Sometimes context require naming should be
   * non-contant, otherwise names will clash in shared identifier scope. If this naming is constant,
   * then it is turned into corresponding prefix naming.
   * @param preference preference for prefix or suffix naming
   * @return non-constant naming template or {@code this} if already non-constant
   */
  public abstract Naming requireNonConstant(Preference preference);

  public enum Preference {
    PREFIX, SUFFIX
  }

  public enum Usage {
    INDIFFERENT,
    CAPITALIZED,
    // funny name
    LOWERIZED;
    public String apply(String input) {
      if (!input.isEmpty()) {
        if (this == CAPITALIZED && !Ascii.isUpperCase(input.charAt(0))) {
          return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, input);
        }
        if (this == LOWERIZED && !Ascii.isLowerCase(input.charAt(0))) {
          return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, input);
        }
      }
      return input;
    }
  }

  /**
   * Naming the repeats the input name
   * @return identity naming
   */
  public static Naming identity() {
    return IDENTITY_NAMING;
  }

  /**
   * @param template template string
   * @return naming that could be applied or detected following template
   */
  public static Naming from(String template) {
    if (template.isEmpty() || template.equals(NAME_PLACEHOLDER)) {
      return IDENTITY_NAMING;
    }
    checkArgument(TEMPLATE_CHAR_MATCHER.matchesAllOf(template),
        "Naming template [%s] contains unsupported characters, only java identifier chars and '*' placeholder are allowed (ASCII only)",
        template);

    List<String> parts = TEMPLATE_SPLITTER.splitToList(template);
    checkArgument(parts.size() <= 2,
        "Naming template [%s] contains more than one '*' placeholder, which is unsupported",
        template);

    return parts.size() == 1
        ? new ConstantNaming(template)
        : new PrefixSuffixNaming(parts.get(0), parts.get(1));
  }

  public static Naming[] fromAll(String... templates) {
    Naming[] namings = new Naming[templates.length];
    for (int i = 0; i < templates.length; i++) {
      namings[i] = from(templates[i]);
    }
    return namings;
  }

  private static final Naming IDENTITY_NAMING = new Naming() {
    @Override
    public String apply(String input) {
      return input;
    }

    @Override
    public String detect(String identifier) {
      return identifier;
    }

    @Override
    public boolean isIdentity() {
      return true;
    }

    @Override
    public boolean isConstant() {
      return false;
    }

    @Override
    public Naming requireNonConstant(Preference preference) {
      return this;
    }

    @Override
    public String toString() {
      return Naming.class.getSimpleName() + ".identity()";
    }
  };

  private static class ConstantNaming extends Naming {
    final String name;

    ConstantNaming(String name) {
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
    public boolean isIdentity() {
      return false;
    }

    @Override
    public boolean isConstant() {
      return true;
    }

    @Override
    public Naming requireNonConstant(Preference preference) {
      switch (preference) {
      case SUFFIX:
        return new PrefixSuffixNaming("", Usage.CAPITALIZED.apply(name));
      case PREFIX:
      default:
        return new PrefixSuffixNaming(name, "");
      }
    }

    @Override
    public String toString() {
      return Naming.class.getSimpleName() + ".from(" + name + ")";
    }
  }

  private static class PrefixSuffixNaming extends Naming {
    final String prefix;
    final String suffix;
    final int lengthsOfPrefixAndSuffix;

    PrefixSuffixNaming(String prefix, String suffix) {
      this.prefix = prefix;
      this.suffix = suffix;
      this.lengthsOfPrefixAndSuffix = suffix.length() + prefix.length();
      Preconditions.checkArgument(lengthsOfPrefixAndSuffix > 0);
    }

    @Override
    public String apply(String input) {
      Usage resultFormat = prefix.isEmpty()
          ? Usage.INDIFFERENT
          : Usage.CAPITALIZED;

      return prefix + resultFormat.apply(input) + suffix;
    }

    @Override
    public String detect(String identifier) {
      if (identifier.length() <= lengthsOfPrefixAndSuffix) {
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
    public boolean isIdentity() {
      return false;
    }

    @Override
    public boolean isConstant() {
      return false;
    }

    @Override
    public Naming requireNonConstant(Preference preference) {
      return this;
    }

    @Override
    public String toString() {
      return Naming.class.getSimpleName() + ".from(" + prefix + NAME_PLACEHOLDER + suffix + ")";
    }
  }
}
