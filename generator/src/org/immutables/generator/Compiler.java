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
