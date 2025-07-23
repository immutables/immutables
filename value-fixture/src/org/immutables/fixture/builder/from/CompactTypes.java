package org.immutables.fixture.builder.from;

import org.immutables.value.Value;

public interface CompactTypes {
  interface SmallParentOne {
    int getFirst();

    int getSecond();

    int getThird();

    int getFourth();

    int getFifth();

    int getSixth();

    int getSeventh();
  }

  interface SmallParentTwo {
    String getAlpha();

    String getBeta();

    String getGamma();

    String getDelta();

    String getEpsilon();

    String getZeta();
  }

  @Value.Immutable
  interface CompactChild extends SmallParentOne, SmallParentTwo {
    int getFirst();

    int getSecond();
  }
}
