package org.immutables.modeling.templating;

import com.google.common.base.Optional;

public interface TypeScope {

  Binding root();

  Binding declare(String typeReference);

  public interface Binding {
    Optional<Binding> access(String identifier);
  }
}
