package nonimmutables.imprt;

import nonimmutables.Uppercase.nested.It;
import nonimmutables.Uppercase.Just;
import org.immutables.value.Value;

@Value.Immutable
public interface ImportFromUppercasePackage {
  Just just();

  It it();
}
