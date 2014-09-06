package org.immutables.generate.silly;

import java.util.List;
import org.immutables.annotation.GenerateAuxiliary;
import org.immutables.annotation.GenerateConstructorParameter;
import org.immutables.annotation.GenerateImmutable;

@GenerateImmutable
public interface IfaceValue {

  @GenerateConstructorParameter
  int getNumber();

  @GenerateAuxiliary
  List<String> auxiliary();
}
