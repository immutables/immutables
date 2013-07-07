package org.immutables.generate.silly;

import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateMarshaledAs;
import org.immutables.annotation.GenerateMarshaler;
import org.immutables.annotation.GenerateRepository;
import com.google.common.base.Optional;
import java.util.List;

@GenerateImmutable
@GenerateRepository
@GenerateMarshaler
public abstract class SillyStructureWithId {

  @GenerateMarshaledAs("_id")
  public abstract String id();

  public abstract String attr1();

  public abstract boolean flag2();

  public abstract Optional<Integer> opt3();

  public abstract long very4();

  public abstract double wet5();

  public abstract List<SillySubstructure> subs6();

  public abstract SillySubstructure nest7();

  public abstract Optional<SillyTuplie> tup3();

  public abstract int int9();
}
