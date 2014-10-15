package org.immutables.value.processor;

import com.google.common.base.Function;
import org.immutables.generator.Generator;

@Generator.Template
abstract class Transformers extends ValuesTemplate {

  public final Function<String, String> simplifyName = new Function<String, String>() {
    @Override
    public String apply(String className) {
      int indexOfLastDot = className.lastIndexOf('.');
      return indexOfLastDot < 0 ? className : className.substring(indexOfLastDot + 1);
    }
  };
}
