package org.immutables.fixture.module.subpackage;

import org.immutables.fixture.module.Module;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(headerComments = true)
public interface HasModuleField {
  Module getModule();
}
