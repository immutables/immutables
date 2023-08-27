package org.immutables.fixture.generics;

import java.util.List;
import java.util.Map;
import javax.validation.constraints.Email;
import javax.validation.constraints.Size;
import org.immutables.value.Value;

@Value.Immutable
public interface HasTypeAnnotationWithAttributes {
  Map<@Size(min = 1, max = 99) String, Integer> map();
  List<@Size(min = 1, max = 99) @Email String> list();

  // This below uses another parsing routines(?)
  interface Container<T> {}
  Container<@Size(min = 1, max = 99) @Email String> container();
}
