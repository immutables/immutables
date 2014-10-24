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

import com.google.common.base.Joiner;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

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
