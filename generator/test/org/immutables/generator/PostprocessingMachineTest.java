
package org.immutables.generator;

import com.google.common.base.Joiner;
import org.junit.Ignore;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class PostprocessingMachineTest {
  private static final Joiner LINES = Joiner.on('\n');

  @Test
  @Ignore
  public void imports() {
    CharSequence rewrited = PostprocessingMachine.rewrite(
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
  @Ignore
  public void generatedImportsPlaceholder() {
    CharSequence rewrited = PostprocessingMachine.rewrite(
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
  @Ignore
  public void javaLangImports() {
    CharSequence rewrited = PostprocessingMachine.rewrite(
        LINES.join("package start;",
            "class My extends java.lang.Throwable {}"));

    check(rewrited).hasToString(
        LINES.join("package start;",
            "class My extends Throwable {}"));
  }

  @Test
  @Ignore
  public void importsNoPlaceholders() {
    CharSequence rewrited = PostprocessingMachine.rewrite(
        "class My extends java.util.Set {}");

    check(rewrited).hasToString(
        LINES.join("import java.util.Set;",
            "class My extends Set {}"));

    rewrited = PostprocessingMachine.rewrite(
        LINES.join(
            "package start;",
            "class My extends java.util.Set {}"));

    check(rewrited).hasToString(
        LINES.join("package start;",
            "import java.util.Set;",
            "class My extends Set {}"));
  }

}
