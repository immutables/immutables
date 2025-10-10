package org.immutables.generator;

import com.google.common.base.Ascii;
import com.google.common.base.CaseFormat;
import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Converter-like function to apply or extract naming, derived from input.
 */
public abstract class Naming implements Function<String, String> {
  protected Naming() {}

  protected static final String NOT_DETECTED = "";
  protected static final String NAME_PLACEHOLDER = "*";
  protected static final Splitter TEMPLATE_SPLITTER = Splitter.on(NAME_PLACEHOLDER);
  protected static final CharMatcher TEMPLATE_CHAR_MATCHER =
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

  /**
   * Require naming to follow JavaBeam capitalization convention.
   *
   * <p>See 8.8 Capitalization of inferred names
   *
   * <pre>
   * Thus when we extract a property or event name from the middle of an existing Java name, we
   * normally convert the first character to lower case. However to support the occasional use of all
   * upper-case names, we check if the first two characters of the name are both upper case and if
   * so leave it alone. So for example, "FooBah" becomes "fooBah", "Z" becomes "z", "URL" becomes "URL"
   * </pre>
   * @see  <a href="https://download.oracle.com/otndocs/jcp/7224-javabeans-1.01-fr-spec-oth-JSpec/">javabean spec</a>
   */

  public abstract Naming requireJavaBeanConvention();

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
    public Naming requireJavaBeanConvention() {
      return this;
    }

    @Override
    public String toString() {
      return NAME_PLACEHOLDER;
    }
  };
}