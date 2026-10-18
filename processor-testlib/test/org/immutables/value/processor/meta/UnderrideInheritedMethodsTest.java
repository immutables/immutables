/*
 * Copyright 2026 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.value.processor.meta;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import javax.tools.JavaFileObject;
import org.immutables.value.processor.Processor;
import org.junit.Test;

import static org.immutables.check.Checkers.check;

/**
 * Underride methods which are not declared on the abstract value type itself, but inherited
 * from a supertype (#1527). A {@code default} method can only be invoked via {@code Type.super}
 * when {@code Type} is a direct superinterface of the generated class, which an interface
 * further up the hierarchy is not; from an abstract class it has to be a plain {@code super}.
 */
public class UnderrideInheritedMethodsTest {

  private static final JavaFileObject ENTITY = JavaFileObjects.forSourceLines("repro.Entity",
      "package repro;",
      "public interface Entity {",
      "  int id();",
      "  default int hash() { return id(); }",
      "  default String stringify() { return \"Entity\" + id(); }",
      "  default boolean equalTo(Entity other) { return id() == other.id(); }",
      "}");

  @Test
  public void inheritedByInterface() {
    assertCompiles(ENTITY, JavaFileObjects.forSourceLines("repro.DemoEntity",
        "package repro;",
        "import org.immutables.value.Value;",
        "@Value.Immutable",
        "@Value.Modifiable",
        "@Value.Style(underrideHashCode = \"hash\", underrideEquals = \"equalTo\", underrideToString = \"stringify\")",
        "public interface DemoEntity extends Entity {}"));
  }

  @Test
  public void inheritedByAbstractClass() {
    assertCompiles(ENTITY, JavaFileObjects.forSourceLines("repro.AbstractDemoEntity",
        "package repro;",
        "import org.immutables.value.Value;",
        "@Value.Immutable",
        "@Value.Modifiable",
        "@Value.Style(underrideHashCode = \"hash\", underrideEquals = \"equalTo\", underrideToString = \"stringify\")",
        "public abstract class AbstractDemoEntity implements Entity {}"));
  }

  @Test
  public void declaredOnAbstractClass() {
    assertCompiles(JavaFileObjects.forSourceLines("repro.AbstractDemo",
        "package repro;",
        "import org.immutables.value.Value;",
        "@Value.Immutable",
        "@Value.Modifiable",
        "@Value.Style(underrideHashCode = \"hash\", underrideEquals = \"equalTo\", underrideToString = \"stringify\")",
        "public abstract class AbstractDemo {",
        "  public abstract int id();",
        "  public int hash() { return id(); }",
        "  public String stringify() { return \"Demo\" + id(); }",
        "  public boolean equalTo(AbstractDemo other) { return id() == other.id(); }",
        "}"));
  }

  private static void assertCompiles(JavaFileObject... sources) {
    Compilation compilation = Compiler.javac()
        .withProcessors(new Processor())
        .compile(sources);
    check(compilation.errors().toString(), compilation.errors().isEmpty());
  }
}
