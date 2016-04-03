package nonimmutables;

import org.immutables.value.Value;

// Compilation test for capitalized attributes
// via disabled import postprocessing
@Value.Immutable
public interface WithCapitalizedAttributes {
  int Id();

  String Name();
}
