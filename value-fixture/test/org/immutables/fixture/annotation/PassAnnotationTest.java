/*
   Copyright 2016 Immutables Authors and Contributors

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

import java.lang.reflect.Parameter;
import java.lang.reflect.Constructor;
import nonimmutables.A1;
import nonimmutables.A2;
import nonimmutables.B1;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class PassAnnotationTest {
  @Test
  public void passAnnotations() throws Exception {
    check(ImmutableValForPass.class.getAnnotation(A1.class)).notNull();
    check(ImmutableValForPass.class.getAnnotation(A2.class)).notNull();
    check(ImmutableValForPass.class.getAnnotation(B1.class)).isNull();
    Parameter parameter = ImmutableValForPass.class.getConstructor(int.class).getParameters()[0];
    check(parameter.getAnnotation(A1.class)).notNull();
    check(parameter.getAnnotation(A2.class)).notNull();
  }
}
