package org.immutables.fixture;

import com.google.common.base.Optional;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
public interface Wildcards {

  List<?> wilcards();

  Optional<? super String> optional();
}
