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
package org.immutables.builder.fixture;

import java.lang.reflect.Modifier;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class FactoryBuilderTest {

  @Test
  public void generatedPublicAsOfStyleAnnotation() {
    check(Modifier.isPublic(SumBuilder.class.getModifiers()));
    check(Modifier.isPublic(ConcatBuilder.class.getModifiers()));
  }

  @Test(expected = IllegalStateException.class)
  public void missingAttributesCheck() {
    new SumBuilder().build();
  }

  @Test(expected = Exception.class)
  public void throwingFactory() throws Exception {
    new ThrowingBuilder().build();
  }

  @Test
  public void invokesStaticFactory() {
    check(new SumBuilder().a(2).b(3).build()).is(5);
    check(new ConcatBuilder().addNumbers(1, 2).addStrings("a", "b").build()).isOf("a", "b", 1, 2);
  }
}
