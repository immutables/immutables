package org.immutables.gson.adapter;

import org.immutables.value.Value;
import org.immutables.gson.Gson;

/**
 * Serialization of primitive arrays should be delegated to Gson
 * and can be customized by registering type adapters (or factories of thereof).
 */
@Gson.TypeAdapters
public interface PrimitiveArrays {

  @Value.Immutable(builder = false)
  interface Aa {
    @Value.Parameter
    byte[] bytes();

    @Value.Parameter
    char[] chars();

    @Value.Parameter
    boolean[] bools();

    @Value.Parameter
    int[] ints();
  }

  @Value.Immutable
  interface Bc {
    byte[] bytes();

    char[] chars();

    boolean[] bools();

    int[] ints();
  }
}
