package nonimmutables.huge;

import org.immutables.value.Value;

@Value.Immutable
public interface ChildOfHugeParents extends HugeParentOne, HugeParentTwo {
}
