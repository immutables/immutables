package nonimmutables._underscore;

import nonimmutables.NonUnderscored;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

// Regression for the not properly processed _underscore segment
// resulting in missing import for nonimmutables.NonUnderscored
@Value.Immutable
public interface SomeUnderscored {
  Optional<String> hello();
  List<Integer> nums();
  NonUnderscored nonUnderscored();
}
