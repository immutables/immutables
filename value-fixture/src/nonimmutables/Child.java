package nonimmutables;

import nonimmutables._underscore.Parent;
import org.immutables.value.Value;

@Value.Immutable
public interface Child extends Parent {
  int b();
}
