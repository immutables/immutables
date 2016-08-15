package nonimmutables.bravo;

import nonimmutables.alpha.Charlie;
import org.immutables.value.Value;

@Value.Immutable
public interface Echo extends Charlie {
  public abstract ImmutableDelta getBuelta();
}
