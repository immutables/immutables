package nonimmutables.shallow.deep;

import nonimmutables.shallow.Bravo;
import org.immutables.value.Value;

//for #505
@Value.Immutable
abstract class AbstractAlpha extends Bravo<ImmutableCharlie> {
}