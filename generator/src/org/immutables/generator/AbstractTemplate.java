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

import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

/**
 * Abstract template to be (optionally) extended by classes which are annotated
 * by {@literal @}{@link Generator.Template}
 */
public abstract class AbstractTemplate extends Builtins {

  protected final ProcessingEnvironment processing() {
    return StaticEnvironment.processing();
  }

  protected final RoundEnvironment round() {
    return StaticEnvironment.round();
  }

  protected final Set<TypeElement> annotations() {
    return StaticEnvironment.annotations();
  }
}
