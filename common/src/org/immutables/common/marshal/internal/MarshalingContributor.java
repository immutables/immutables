package org.immutables.common.marshal.internal;

import org.immutables.common.marshal.Marshaler;
import com.google.common.collect.ImmutableMap;

public interface MarshalingContributor {

  void putMarshalers(ImmutableMap.Builder<Class<?>, Marshaler<?>> builder);
}
