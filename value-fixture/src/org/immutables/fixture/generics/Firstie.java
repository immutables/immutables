package org.immutables.fixture.generics;

import java.util.List;
import org.immutables.value.Value;
import java.io.Serializable;

//@Value.Immutable
public interface Firstie<T, V extends Runnable & Serializable> {
  T ref();
  List<V> commands();
}
