package org.immutables.fixture.encoding.defs;

import java.util.Optional;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.encode.Encoding;

@Encoding
class OptionalMap<K, V> {

  // implemented as nullable map field
  @Encoding.Impl
  private final ImmutableMap<K, V> map = null;

  // Expose (and detect) the field as Optional of Map
  @Encoding.Expose
  public Optional<Map<K, V>> get() {
    return Optional.<Map<K, V>>ofNullable(map);
  }

  // alternatively it can be exposed as Optional of ImmutableMap
  @Encoding.Expose
  public Optional<ImmutableMap<K, V>> getImmutable() {
    return Optional.ofNullable(map);
  }

  // as field is nullable, so null-safe hashcode required
  @Override
  public int hashCode() {
    return java.util.Objects.hashCode(map);
  }

  @Override
  public String toString() {
    return get().toString();
  }

  boolean equals(OptionalMap<K, V> obj) {
    return java.util.Objects.equals(this.map, obj.map);
  }

  @Encoding.Init
  static @Nullable <K, V> ImmutableMap<K, V> init(Optional<? extends Map<K, V>> map) {
    return map.isPresent()
        ? ImmutableMap.copyOf(map.get())
        : null;
  }

  @Encoding.Builder
  static class Builder<K, V> {
    private @Nullable ImmutableMap.Builder<K, V> builder = null;

    @Encoding.Naming("put*")
    @Encoding.Init
    void put(K row, V value) {
      nonnullBuilder().put(row, value);
    }

    @Encoding.Naming("putAll*")
    @Encoding.Init
    void putAll(Map<? extends K, ? extends V> entries) {
      nonnullBuilder().putAll(entries);
    }

    @Encoding.Naming("set*")
    @Encoding.Init
    @Encoding.Copy
    void set(Optional<? extends Map<K, V>> entries) {
      this.builder = null;
      if (entries.isPresent()) {
        nonnullBuilder().putAll(entries.get());
      }
    }

    private ImmutableMap.Builder<K, V> nonnullBuilder() {
      return builder == null ? builder = ImmutableMap.<K, V>builder() : builder;
    }

    @Encoding.Build
    @Nullable
    ImmutableMap<K, V> build() {
      return builder != null ? builder.build() : null;
    }
  }
}
