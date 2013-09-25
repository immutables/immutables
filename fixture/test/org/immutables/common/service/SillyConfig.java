package org.immutables.common.service;

import com.google.inject.Binder;
import com.google.inject.Module;
import javax.inject.Provider;
import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateMarshaler;

@GenerateImmutable
@GenerateMarshaler
public abstract class SillyConfig implements Provider<Module> {

  public abstract String param1();

  public abstract int param2();

  @Override
  public Module get() {
    return new Module() {
      @Override
      public void configure(Binder binder) {}
    };
  }
}
