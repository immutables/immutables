
package org.immutables.generator;

import com.google.common.base.Joiner;
import org.junit.Ignore;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class PostprocessingMachineTest {
  private static final Joiner LINES = Joiner.on('\n');

  @Test
  public void imports() {
    CharSequence rewrited = PostprocessingMachine.rewrite(
        LINES.join("package start;",
            "import java.util.List;",
            "class My extends java.util.Set {}"));

    check(rewrited).hasToString(
        LINES.join("package start;",
            "import java.util.List;",
            "import java.util.Set;",
            "class My extends Set {}"));
  }

  @Test
  public void lineComment() {
    CharSequence rewrited = PostprocessingMachine.rewrite(
        LINES.join("package start;",
            "import java.util.List;",
            "class My extends java.util.Set {",
            "// comment",
            "// comment with fully qualified class name java.until.Map",
            "}"));

    check(rewrited).hasToString(
        LINES.join("package start;",
            "import java.util.List;",
            "import java.util.Set;",
            "class My extends Set {",
            "// comment",
            "// comment with fully qualified class name java.until.Map",
            "}"));
  }

  @Test
  public void blockComment() {
    CharSequence rewrited = PostprocessingMachine.rewrite(
        LINES.join("package start;",
            "import java.util.List;",
            "class My extends java.util.Set {",
            "/* class name in block comment java.until.Map.get()*/",
            "/**",
            "class name in block comment java.until.Map.get()",
            "**/",
            "}"));

    check(rewrited).hasToString(
        LINES.join("package start;",
            "import java.util.List;",
            "import java.util.Set;",
            "class My extends Set {",
            "/* class name in block comment java.until.Map.get()*/",
            "/**",
            "class name in block comment java.until.Map.get()",
            "**/",
            "}"));
  }

  @Test
  public void javaLangImports() {
    CharSequence rewrited = PostprocessingMachine.rewrite(
        LINES.join("package start;",
            "class My extends java.lang.Throwable {}"));

    check(rewrited).hasToString(
        LINES.join("package start;",
            "class My extends Throwable {}"));
  }

  @Test
  public void simpleImport() {
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

  @Test
  @Ignore
  public void staticImport() {
    CharSequence rewrited = PostprocessingMachine.rewrite(LINES.join(
        "import static org.immutables.check.Checkers.*;",
        "class My extends java.util.Set {}"));

    check(rewrited).hasToString(LINES.join(
            "import java.util.Set;",
            "import static org.immutables.check.Checkers.*;",
            "class My extends Set {}"));
  }

  @Test
  @Ignore
  public void conflictResolution() {
    CharSequence rewrited = PostprocessingMachine.rewrite(
        "class Set extends java.util.Set {}");

    check(rewrited).hasToString(
        "class Set extends java.util.Set {}");

    rewrited = PostprocessingMachine.rewrite(LINES.join(
        "import my.Set;",
        "class X {",
        "  my.Set same(Set set);",
        "}"));

    check(rewrited).hasToString(LINES.join(
            "import my.Set;",
            "class X {",
            "  Set same(Set set);",
            "}"));
  }
}
