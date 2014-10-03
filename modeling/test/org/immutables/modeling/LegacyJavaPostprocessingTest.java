package org.immutables.modeling;

import static org.immutables.check.Checkers.*;
import com.google.common.base.Joiner;
import org.junit.Test;

@SuppressWarnings("deprecation")
public class LegacyJavaPostprocessingTest {
  private static final Joiner LINES = Joiner.on('\n');

  @Test
  public void imports() {
    CharSequence rewrited = LegacyJavaPostprocessing.rewrite(
        LINES.join("package start;",
            "import java.util.List;",
            "class My extends java.util.Set {}"));

    check(rewrited).hasToString(
        LINES.join("package start;",
            "import java.util.Set;",
            "import java.util.List;",
            "class My extends Set {}"));
  }

  @Test
  public void generatedImportsPlaceholder() {
    CharSequence rewrited = LegacyJavaPostprocessing.rewrite(
        LINES.join("package start;",
            "import java.util.List;",
            "// Generated imports",
            "",
            "class My extends java.util.Set {}"));

    check(rewrited).hasToString(
        LINES.join("package start;",
            "import java.util.List;",
            "import java.util.Set;",
            "",
            "class My extends Set {}"));
  }

  @Test
  public void javaLangImports() {
    CharSequence rewrited = LegacyJavaPostprocessing.rewrite(
        LINES.join("package start;",
            "class My extends java.lang.Throwable {}"));

    check(rewrited).hasToString(
        LINES.join("package start;",
            "class My extends Throwable {}"));
  }

  @Test
  public void importsNoPlaceholders() {
    CharSequence rewrited = LegacyJavaPostprocessing.rewrite(
        "class My extends java.util.Set {}");

    check(rewrited).hasToString(
        LINES.join("import java.util.Set;",
            "class My extends Set {}"));

    rewrited = LegacyJavaPostprocessing.rewrite(
        LINES.join(
            "package start;",
            "class My extends java.util.Set {}"));

    check(rewrited).hasToString(
        LINES.join("package start;",
            "import java.util.Set;",
            "class My extends Set {}"));
  }
}
