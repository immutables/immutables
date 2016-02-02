/*
   Copyright 2016 Immutables Authors and Contributors

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

import com.sun.tools.javac.code.Symbol.ClassSymbol;
import java.util.EnumSet;
import org.eclipse.jdt.internal.compiler.apt.model.ElementImpl;

enum Compiler {
  JAVAC, ECJ;

  boolean isPresent() {
    return COMPILERS.contains(this);
  }

  private static final EnumSet<Compiler> COMPILERS = detectCompilers();

  private static EnumSet<Compiler> detectCompilers() {
    EnumSet<Compiler> compilers = EnumSet.noneOf(Compiler.class);
    try {
      // Specific method is not essential, we just
      // forcing to load class here
      ClassSymbol.class.getCanonicalName();
      compilers.add(Compiler.JAVAC);
    } catch (Throwable ex) {
    }
    try {
      // just forcing the loading of class
      ElementImpl.class.getCanonicalName();
      compilers.add(Compiler.ECJ);
    } catch (Throwable ex) {
    }
    return compilers;
  }
}
