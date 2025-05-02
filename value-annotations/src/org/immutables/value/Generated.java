/*
   Copyright 2018 Immutables Authors and Contributors

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
package org.immutables.value;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.immutables.value.Value.Style;

/**
 * Annotation to mark generated classes. This annotations was introduced as class retained
 * annotation and is often needed to differentiate to include exclude generated
 * classes when processing class files, in particular to exclude during code coverage instrumentation
 * and similar. It is not made runtime-retailed because problems were reported with some systems.
 * In order to introduce custom runtime-retained annotation, please try to
 * use {@code org.immutables:annotate} module to inject annotations at various places in generated code.
 * Can be disabled by {@link Style#allowedClasspathAnnotations()}.
 */
@Documented
@Target(ElementType.TYPE)
public @interface Generated {
  /**
   * Used to include reference (usually package relative) to class which was an annotated model
   * from which this class was generated from.
   * @return relative class name (can be package name or method reference).
   */
  String from() default "";

  /**
   * Symbolic name of generator/template used to emit file.
   * @return name of generator
   */
  String generator() default "";
}
