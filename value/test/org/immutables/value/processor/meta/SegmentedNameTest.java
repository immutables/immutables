package org.immutables.value.processor.meta;

import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class SegmentedNameTest {

  @Test
  public void parsing() {
    verify("packages.of.Fata", "[packages.of].[].[Fata]");
    verify("packages.Fata", "[packages].[].[Fata]");
    verify("xx.ss.Mm", "[xx.ss].[].[Mm]");
    verify("xx.fg.Ss.Mm", "[xx.fg].[Ss].[Mm]");
    verify("Mm", "[].[].[Mm]");
    verify("Mss.Mm", "[].[Mss].[Mm]");
    verify("Mss.Zdd.Mm", "[].[Mss.Zdd].[Mm]");
    verify("sss.ss", "[sss.ss].[].[]");
  }

  void verify(String qualifiedName, String expectedToString) {
    check(SegmentedName.from(qualifiedName)).hasToString(expectedToString);
  }
}
