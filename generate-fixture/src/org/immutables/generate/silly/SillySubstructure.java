package org.immutables.generate.silly;

import org.immutables.annotation.GenerateAsDefault;
import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateMarshaledAs;
import org.immutables.annotation.GenerateMarshaler;
import org.immutables.annotation.GenerateRepository;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

@GenerateImmutable
@GenerateMarshaler
@GenerateRepository
public abstract class SillySubstructure {

  @GenerateAsDefault
  @GenerateMarshaledAs("e1")
  public RetentionPolicy enum1() {
    return RetentionPolicy.SOURCE;
  }

  public abstract Set<ElementType> set2();

  public abstract Set<Integer> set3();

  public abstract List<Float> floats4();
}
