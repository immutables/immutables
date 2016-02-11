/*
   Copyright 2014 Immutables Authors and Contributors

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
package org.immutables.generator.processor;

import com.google.common.base.Preconditions;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public abstract class Introspection {
  protected final ProcessingEnvironment environment;
  protected final Elements elements;
  protected final Types types;
  protected final boolean inEclipseCompiler;

  Introspection(ProcessingEnvironment environment) {
    this.environment = environment;
    this.elements = environment.getElementUtils();
    this.types = environment.getTypeUtils();
    this.inEclipseCompiler = environment.getClass().getName().startsWith("org.eclipse.jdt");
  }

  protected String toSimpleName(TypeMirror typeMirror) {
    return Preconditions.checkNotNull(
        types.asElement(typeMirror), "not declared type")
        .getSimpleName().toString();
  }

  protected String toName(TypeMirror typeMirror) {
    return ((TypeElement) Preconditions.checkNotNull(
        types.asElement(typeMirror), "not declared type"))
        .getQualifiedName().toString();
  }
}
