package org.immutables.fixture.j17;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import static org.immutables.check.Checkers.check;

public class RecordStagedNullMarkedTest {
  @Test void builderStagesIsNullMarked() {
    check(RecordStagedNullMarkedBuilderStages.class.getAnnotation(NullMarked.class)).notNull();
  }

  @Test void mandatoryStageParamIsNotNullable() throws Exception {
    var method = RecordStagedNullMarkedBuilderStages.BuildStart.class.getDeclaredMethod("name", String.class);
    check(method.getParameters()[0].getAnnotatedType().getAnnotation(Nullable.class)).isNull();
  }

  @Test void optionalParamIsNullable() throws Exception {
    var method = RecordStagedNullMarkedBuilder.class.getDeclaredMethod("nickname", String.class);
    check(method.getParameters()[0].getAnnotatedType().getAnnotation(Nullable.class)).notNull();
  }
}
