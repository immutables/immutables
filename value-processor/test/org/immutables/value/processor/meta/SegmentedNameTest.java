/*
    Copyright 2014 Ievgen Lukash

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
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
