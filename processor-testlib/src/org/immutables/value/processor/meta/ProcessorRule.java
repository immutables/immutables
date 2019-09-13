/*
 * Copyright 2019 Immutables Authors and Contributors
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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.immutables.generator.AbstractGenerator;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * JUnit4 {@link org.junit.Rule} which executes test with real (in-memory) compiler and custom immutables processor.
 * It gives access to {@link Elements} and {@link Types} as well as {@link ValueType} (for a given class).
 *
 * <p>A much better alternative to mocking {@code javax.lang.model} classes.
 *
 * <h3>Usage example</h3>
 * <p>
 * <pre>
 * {@code
 * @Rule
 * public final ProcessorRule rule = new ProcessorRule();
 *
 * @ProcessorRule.TestImmutable
 * interface MyClass {
 *   String attr();
 * }
 *
 * @Test
 * public void basic() {
 *   ValueType type = rule.value(MyClass.class);
 *    check(type.attributes.get(0).name()).is("attr");
 * }
 * }
 * </pre>
 * </p>
 * <p>TODO: This rule has to be migrated to JUnit5 as <a href="https://junit.org/junit5/docs/current/user-guide/#extensions">extension</a>.
 * For some examples see:
 *  <ol>
 *    <li><a href="https://github.com/Kiskae/compile-testing-extension">compile testing extension</a></li>
 *    <li><a href="https://github.com/google/compile-testing/pull/155">Add JUnit5 implementation of CompilationRule PR</a></li>
 *  </ol>
 * </p>
 */
public class ProcessorRule implements TestRule  {

  private static final Class<?> DEFAULT_ANNOTATION_CLASS = TestImmutable.class;

  /**
   * Simple "file" to trigger compilation.
   */
  private static final JavaFileObject EMPTY = JavaFileObjects.forSourceLines("Empty", "final class Empty {}");

  private final ValueTypeComposer composer = new ValueTypeComposer();

  private Elements elements;
  private Types types;
  private Round round;

  /**
   * Annotation to be used instead of {@literal @}{@code Value.Immutable}. Avoids generating
   * unnecessary classes.
   */
  public @interface TestImmutable {}

  /**
   * Returns {@link Elements} instance associated with the current execution.
   * @throws IllegalStateException if invoked outside the rule.
   */
  public Elements elements() {
    Preconditions.checkState(elements != null, "not running as part of %s", ProcessorRule.class.getSimpleName());
    return elements;
  }

  /**
   * Returns {@link Types} instance associated with the current execution.
   * @throws IllegalStateException if invoked outside the rule.
   */
  public Types types() {
    Preconditions.checkState(types != null, "not running as part of %s", ProcessorRule.class.getSimpleName());
    return types;
  }

  /**
   * Return single {@link ValueType} instance associated with immutable class {@code type}.
   * @throws IllegalArgumentException if multiple {@link ValueType}s are associated with {@code type}
   */
  public ValueType value(Class<?> type) {
    final List<ValueType> values = values(type);
    if (values.size() != 1) {
      throw new IllegalArgumentException(String.format("Expected 1 values but got %d for %s", values.size(), type));
    }
    return values.get(0);
  }

  /**
   * Return multiple {@link ValueType} instances associated with immutable class {@code type}. Multiple
   * instances can be due to several nested immutable classes.
   */
  public List<ValueType> values(Class<?> type) {
    Preconditions.checkNotNull(type, "type");
    Preconditions.checkState(round != null, "not running as part of %s", ProcessorRule.class.getSimpleName());
    final TypeElement element = elements().getTypeElement(type.getCanonicalName());
    final ImmutableList<Proto.Protoclass> protos = round.protoclassesFrom(Collections.singleton(element));
    final List<ValueType> values = new ArrayList<>();
    for (Proto.Protoclass proto: protos) {
      final ValueType value = new ValueType();
      composer.compose(value, proto);
      values.add(value);
    }
    return ImmutableList.copyOf(values);
  }

  @Override
  public Statement apply(final Statement base, final Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        final LocalProcessor processor = new LocalProcessor(base);
        final Compilation compilation = Compiler.javac().withProcessors(processor).compile(EMPTY);

        if (!compilation.status().equals(Compilation.Status.SUCCESS)) {
          throw new AssertionError(String.format("Compilation failed (status:%s): %s", compilation.status(), compilation.diagnostics()));
        }

        if (!processor.wasEvaluated) {
          throw new AssertionError(String.format("%s was not evaluated. Check that annotation processor %s was triggered " +
                          "(eg. %s annotation is correctly registered)",
                  description.getDisplayName(), processor.getClass().getSimpleName(), DEFAULT_ANNOTATION_CLASS.getCanonicalName()));
        }

        processor.rethrowIfError();
      }
    };
  }

  /**
   * Simple annotation processor which saves environment information. It is then used by this rule
   * to instantiate internal classes like {@link Round} which gives access to {@link ValueType}.
   */
  private class LocalProcessor extends AbstractGenerator {

    private final Statement statement;
    private final Class<?> annotation = DEFAULT_ANNOTATION_CLASS;

    // saved exception which is potentially rethrown after compilation phase
    private Throwable thrown;

    /**
     * Flag to track if test statement was executed or not. Fail fast if annotation processor
     * was not triggered. IE detected false (no-op) test executions
     */
    private boolean wasEvaluated;

    private LocalProcessor(Statement statement) {
      this.statement = statement;
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
      super.init(processingEnv);
      elements = processingEnv.getElementUtils();
      types = processingEnv.getTypeUtils();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
      // for some reason annotation.getCanonicalName() is not found
      // using wildcard here
      return Collections.singleton("*");
    }

    @Override
    protected void process() {
      round = ImmutableRound.builder()
              .processing(processing())
              .round(round())
              .addCustomImmutableAnnotations(annotation.getCanonicalName())
              .build();

      try {
        statement.evaluate();
      } catch (Throwable e) {
        // means test failed
        thrown = e;
      }

      // mark that statement was executed
      wasEvaluated = true;
    }

    void rethrowIfError() throws Throwable {
      if (thrown != null) {
        throw thrown;
      }
    }
  }

}
