package org.immutables.fixture.style;

import org.immutables.fixture.ImmutablePrimitiveDefault;
import org.immutables.fixture.PrimitiveDefault;
import static org.immutables.check.Checkers.*;
import java.lang.reflect.Modifier;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class StyleTest {
  @Test
  public void publicVisibility() {
    check(!Modifier.isPublic(AbstractValueNamingDetected.class.getModifiers()));
    check(Modifier.isPublic(ValueNamingDetected.class.getModifiers()));
  }

  @Test
  public void packageVisibility() {
    check(Modifier.isPublic(PrimitiveDefault.class.getModifiers()));
    check(!Modifier.isPublic(ImmutablePrimitiveDefault.class.getModifiers()));
  }
}
