package org.immutables.fixture.j17;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import static org.immutables.check.Checkers.check;

public class RecordStagedNullMarkedTest {
  @Test void builderStagesIsNullMarked() {
    check(RecordStagedNullMarkedBuilderStages.class.getAnnotation(NullMarked.class)).notNull();
  }
}
