package org.immutables.common.web;

import com.google.inject.AbstractModule;
import org.immutables.generate.silly.ImmutableSillySub1;
import org.immutables.generate.silly.SillyAbstract;

public class SillyWebModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(SillyAbstract.class).toInstance(ImmutableSillySub1.builder().a(1).build());
    bind(SillyTopLevelResource.class);
  }
}
