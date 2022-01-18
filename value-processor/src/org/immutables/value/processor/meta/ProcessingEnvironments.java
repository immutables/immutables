/*
 * Copyright 2022 Immutables Authors and Contributors
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

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

/**
 * Internal utils relating to {@link ProcessingEnvironment}.
 */
class ProcessingEnvironments {

  /**
   * Detect if current compiler is <a href="https://wiki.eclipse.org/JDT_Core_Programmer_Guide/ECJ">ECJ</a> (Eclipse Compiler).
   *
   * @param environment runtime context of the compilation
   * @return true if current annotation processor is executed as part of ECJ, false otherwise.
   */
  static boolean isEclipseImplementation(ProcessingEnvironment environment) {
    Class<?> lookupClass = String.class;
    TypeElement element = environment.getElementUtils().getTypeElement(lookupClass.getCanonicalName());
    return element.getClass().getCanonicalName().startsWith("org.eclipse");
  }

  private ProcessingEnvironments() {}
}
