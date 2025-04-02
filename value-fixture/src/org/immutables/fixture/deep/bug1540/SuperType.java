package org.immutables.fixture.deep.bug1540;

import java.util.List;

public interface SuperType<T extends SuperType<T>> {
  List<T> getNestedChildrenTypes();
}
