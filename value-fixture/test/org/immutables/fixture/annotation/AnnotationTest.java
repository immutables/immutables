/*
    Copyright 2014 Immutables Authors and Contributors

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
package org.immutables.fixture.annotation;

import org.junit.Test;
import static org.immutables.check.Checkers.*;

@An(13)
public class AnnotationTest {

  @Test
  public void testName() {

    An runtime = getClass().getAnnotation(An.class);
    An immutable = ImmutableAn.of(13);

    check(immutable).is(runtime);
    check(runtime).is(immutable);
    check(ImmutableAn.of(1).annotationType() == An.class);
    check(immutable.hashCode()).is(runtime.hashCode());

    check(immutable.bees()).isOf(runtime.bees());
  }
}
