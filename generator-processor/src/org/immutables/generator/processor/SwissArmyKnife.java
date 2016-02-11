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

import com.google.common.collect.ImmutableMap;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Temporary "global context" class while interactions and structure is not sorted out well.
 * While there is no injection
 */
public class SwissArmyKnife {

  public final ImmutableMap<String, TypeMirror> imports;
  public final Accessors accessors;
  public final Accessors.Binder binder;
  public final Elements elements;
  public final Types types;
  public final TypeElement type;
  public final ProcessingEnvironment environment;

  public SwissArmyKnife(ProcessingEnvironment environment, TypeElement type) {
    this.environment = environment;
    this.type = type;
    this.elements = environment.getElementUtils();
    this.types = environment.getTypeUtils();
    this.imports = new Imports(environment).importsIn(type);
    this.accessors = new Accessors(environment);
    this.binder = accessors.binder();
  }

}
