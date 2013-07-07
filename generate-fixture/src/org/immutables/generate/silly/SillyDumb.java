package org.immutables.generate.silly;

import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateMarshaledAs;
import org.immutables.annotation.GenerateMarshaler;
import com.google.common.base.Optional;
import java.util.List;

@GenerateImmutable
@GenerateMarshaler
public abstract class SillyDumb {

  @GenerateMarshaledAs(value = "a", forceEmpty = true)
  public abstract Optional<Integer> a1();

  @GenerateMarshaledAs(value = "b", forceEmpty = true)
  public abstract List<String> b2();

  @GenerateMarshaledAs(value = "c", forceEmpty = false)
  public abstract Optional<Integer> c3();

  @GenerateMarshaledAs(value = "d", forceEmpty = false)
  public abstract List<String> d4();
}
