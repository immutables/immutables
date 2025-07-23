package org.immutables.fixture.builder.from;

import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Style(mergeFromSupertypesDynamically = false)
@Value.Enclosing
public interface StaticSuperTypes {
  interface StParentOne {
    int getFirst();

    int getSecond();

    int getThird();

    int getFourth();

    int getFifth();

    int getSixth();

    int getSeventh();

    @Nullable String nully();
  }

  interface StParentTwo<T> {
    String getAlpha();

    String getBeta();

    String getGamma();

    String getDelta();

    String getEpsilon();

    String getZeta();

    List<T> data();
  }

  @Value.Immutable
  interface StChild extends StParentOne, StParentTwo<String> {
    int getFirst();

    int getSecond();
  }
}
