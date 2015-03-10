/*
    Copyright 2015 Immutables Authors and Contributors

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

import com.google.common.base.Joiner;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class PostprocessingMachineTest {
  private static final Joiner LINES = Joiner.on('\n');

  @Test
  public void imports() {
    CharSequence rewrited = PostprocessingMachine.rewrite(
        LINES.join("package start;",
            "import java.util.List;",
            "import org.junit.*;",
            "import some.Some.Nested;",
            "final class My extends java.util.Set {",
            "  private java.util.Map<java.lang.String, Integer> map = com.google.common.collect.Maps.newHashMap();",
            "}"));

    check(rewrited).hasToString(
        LINES.join("package start;",
            "",
            "import com.google.common.collect.Maps;",
            "import java.util.List;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import org.junit.*;",
            "import some.Some.Nested;",
            "final class My extends Set {",
            "  private Map<String, Integer> map = Maps.newHashMap();",
            "}"));
  }

  @Test
  public void lineComment() {
    CharSequence rewrited = PostprocessingMachine.rewrite(
        LINES.join("package start;",
            "import java.util.List;",
            "// comment with fully qualified class name java.util.Map",
            "class My extends java.util.Set {",
            "// comment",
            "// comment with fully qualified class name java.util.Map",
            "}"));

    check(rewrited).hasToString(
        LINES.join("package start;",
            "",
            "import java.util.List;",
            "import java.util.Set;",
            "// comment with fully qualified class name java.util.Map",
            "class My extends Set {",
            "// comment",
            "// comment with fully qualified class name java.util.Map",
            "}"));
  }

  @Test
  public void blockComment() {
    CharSequence rewrited = PostprocessingMachine.rewrite(
        LINES.join("package start;",
            "import java.util.List;",
            "/**",
            "class name in block comment java.util.Map.get()",
            "**/",
            "class My extends java.util.Set {",
            "/* class name in block comment java.util.Map.get()*/",
            "/**",
            "class name in block comment java.util.Map.get()",
            "**/",
            "}"));

    check(rewrited).hasToString(
        LINES.join("package start;",
            "",
            "import java.util.List;",
            "import java.util.Set;",
            "/**",
            "class name in block comment java.util.Map.get()",
            "**/",
            "class My extends Set {",
            "/* class name in block comment java.util.Map.get()*/",
            "/**",
            "class name in block comment java.util.Map.get()",
            "**/",
            "}"));
  }

  @Test
  public void stringLiteral() {
    CharSequence rewrited = PostprocessingMachine.rewrite(
        LINES.join("package start;",
            "import java.util.List;",
            "class My extends java.util.Set {",
            "\" class name in string literal java.util.Map.get() \"",
            "}"));

    check(rewrited).hasToString(
        LINES.join("package start;",
            "",
            "import java.util.List;",
            "import java.util.Set;",
            "class My extends Set {",
            "\" class name in string literal java.util.Map.get() \"",
            "}"));
  }

  @Test
  public void javaLangImports() {
    CharSequence rewrited = PostprocessingMachine.rewrite(
        LINES.join("package start;",
            "class My extends java.lang.Throwable {}"));

    check(rewrited).hasToString(
        LINES.join("package start;",
            "",
            "class My extends Throwable {}"));

    rewrited = PostprocessingMachine.rewrite(
        LINES.join("package start;",
            "class Throwable extends java.lang.Throwable {}"));

    check(rewrited).hasToString(
        LINES.join("package start;",
            "",
            "class Throwable extends java.lang.Throwable {}"));

    rewrited = PostprocessingMachine.rewrite(
        LINES.join("package start;",
            "class Throwable extends java.lang.annotation.Retention {}"));

    check(rewrited).hasToString(
        LINES.join("package start;",
            "",
            "import java.lang.annotation.Retention;",
            "class Throwable extends Retention {}"));
  }

  @Test
  public void javaLangInOriginalImports() {
    CharSequence rewrited = PostprocessingMachine.rewrite(LINES.join(
        "import java.lang.String;",
        "class X {",
        "  java.lang.String method(String s);",
        "}"));

    check(rewrited).hasToString(LINES.join(
        "class X {",
        "  String method(String s);",
        "}"));

  }

  @Test
  public void currentPackageImport() {
    CharSequence rewrited = PostprocessingMachine.rewrite(
        LINES.join("package start;",
            "class My extends start.Utils {}"));

    check(rewrited).hasToString(
        LINES.join("package start;",
            "",
            "class My extends Utils {}"));

    rewrited = PostprocessingMachine.rewrite(
        LINES.join("package start;",
            "class Throwable extends start.Utils {",
            "  private class Utils {}",
            "}"));

    check(rewrited).hasToString(
        LINES.join("package start;",
            "",
            "class Throwable extends start.Utils {",
            "  private class Utils {}",
            "}"));
  }

  @Test
  public void currentPackageInOriginalImports() {
    CharSequence rewrited = PostprocessingMachine.rewrite(LINES.join(
        "package my.pack;",
        "import my.pack.Y;",
        "class X {",
        "  my.pack.Y method(Y s);",
        "}"));

    check(rewrited).hasToString(LINES.join(
        "package my.pack;",
        "",
        "class X {",
        "  Y method(Y s);",
        "}"));

  }

  @Test
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
  public void annotationImportInsideClass() {
    CharSequence rewrited = PostprocessingMachine.rewrite(
        "class Set { @javax.annotation.Generated int x; }");

    check(rewrited).hasToString(LINES.join(
        "import javax.annotation.Generated;",
        "class Set { @Generated int x; }"));
  }

  @Test
  public void annotationTypeImport() {
    check(PostprocessingMachine.rewrite(
        "@interface Set { @javax.annotation.Generated int x; }"))
        .hasToString(LINES.join(
            "import javax.annotation.Generated;",
            "@interface Set { @Generated int x; }"));
  }

  @Test
  public void annotationBeforeClass() {
    check(PostprocessingMachine.rewrite(
        "@javax.annotation.Generated class Set {}"))
        .hasToString(LINES.join(
            "import javax.annotation.Generated;",
            "@Generated class Set {}"));
  }

  @Test
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

  @Test
  public void noConflictInDifferentPackages() {
    CharSequence rewrited = PostprocessingMachine.rewrite(LINES.join(
        "import my.other.Set;",
        "class X {",
        "  my.Set same(Set set);",
        "}"));

    check(rewrited).hasToString(LINES.join(
        "import my.other.Set;",
        "class X {",
        "  my.Set same(Set set);",
        "}"));
  }

  @Test
  public void fullyQualifiedWithSpaces() {
    CharSequence rewrited = PostprocessingMachine.rewrite(LINES.join(
        "class X {",
        "  private java.util.Map<java.lang.String, Integer> map = ",
        "    com.google.common.collect",
        "      .Maps.newHashMap();",
        "}"));

    check(rewrited).hasToString(LINES.join(
        "import java.util.Map;",
        "class X {",
        "  private Map<String, Integer> map = ",
        "    com.google.common.collect",
        "      .Maps.newHashMap();",
        "}"));

  }

  @Test
  public void keepClassModifiers() {
    CharSequence rewrited = PostprocessingMachine.rewrite(LINES.join(
        "package mypack;",
        "private final class My{}"));

    check(rewrited).hasToString(LINES.join(
        "package mypack;",
        "",
        "private final class My{}"));

    rewrited = PostprocessingMachine.rewrite(LINES.join(
        "import java.util.List;",
        "abstract class My{}"));

    check(rewrited).hasToString(LINES.join(
        "import java.util.List;",
        "abstract class My{}"));

    rewrited = PostprocessingMachine.rewrite(LINES.join(
        "package mypack;",
        "import java.util.List;",
        "abstract class My{}"));

    check(rewrited).hasToString(LINES.join(
        "package mypack;",
        "",
        "import java.util.List;",
        "abstract class My{}"));

    rewrited = PostprocessingMachine.rewrite("public final class My{}");

    check(rewrited).hasToString("public final class My{}");
  }

  @Test
  public void multipleOccurrences() {
    CharSequence rewrited = PostprocessingMachine.rewrite(LINES.join(
        "import java.utils.Set;",
        "class X extends java.utils.List {",
        "  java.utils.List add(int key);",
        "  my.List add(int key);",
        "}"));

    check(rewrited).hasToString(LINES.join(
        "import java.utils.List;",
        "import java.utils.Set;",
        "class X extends List {",
        "  List add(int key);",
        "  my.List add(int key);",
        "}"));
  }

  @Test
  public void nestedClass() {
    CharSequence rewrited = PostprocessingMachine.rewrite(LINES.join(
        "import com.google.common.collect.ImmutableList;",
        "class X {",
        "ImmutableList.Builder<Mirror> builder = ImmutableList.builder();",
        "}"));

    check(rewrited).hasToString(LINES.join(
        "import com.google.common.collect.ImmutableList;",
        "class X {",
        "ImmutableList.Builder<Mirror> builder = ImmutableList.builder();",
        "}"));

    rewrited = PostprocessingMachine.rewrite(LINES.join(
        "class X {",
        "com.google.common.collect.ImmutableList.Builder<Mirror> builder = ",
        "  com.google.common.collect.ImmutableList.builder();",
        "}"));

    check(rewrited).hasToString(LINES.join(
        "import com.google.common.collect.ImmutableList;",
        "class X {",
        "ImmutableList.Builder<Mirror> builder = ",
        "  ImmutableList.builder();",
        "}"));

    rewrited = PostprocessingMachine.rewrite(LINES.join(
        "import com.google.common.collect.ImmutableList;",
        "class X {",
        "com.google.common.collect.ImmutableList.Builder<Mirror> builder = ",
        "  com.google.common.collect.ImmutableList.builder();",
        "}"));

    check(rewrited).hasToString(LINES.join(
        "import com.google.common.collect.ImmutableList;",
        "class X {",
        "ImmutableList.Builder<Mirror> builder = ",
        "  ImmutableList.builder();",
        "}"));
  }
}
