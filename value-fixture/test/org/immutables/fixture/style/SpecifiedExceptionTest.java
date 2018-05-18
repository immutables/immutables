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
package org.immutables.fixture.style;

import nonimmutables.SampleRuntimeException;
import org.junit.Test;
import static org.immutables.check.Checkers.check;

@SuppressWarnings("CheckReturnValue")
public class SpecifiedExceptionTest {
  @Test(expected = SampleRuntimeException.class)
  public void itThrowsExpectedConfiguredException() {
    ImmutableSpecifiedException.builder().build();
  }

  @Test(expected = SampleRuntimeException.class)
  public void throwsSampleExceptionInsteadOfNullPointer() {
    ImmutableSpecifiedException.builder().someRequiredString(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void itThrowsSpecifiedExceptionOnBuild() {
    ImmutableSpecifiedException.builder().buildOrThrow(IllegalArgumentException::new);
  }

  @Test
  public void itThrowsExceptionWithMissingAttributeNames() {
    try {
      ImmutableSpecifiedExceptionWithNamesConstructor.builder().build();
      check(false);
    } catch (SpecifiedExceptionWithNamesConstructor.Exc ex) {
      check(ex.names).isOf("someRequiredInteger", "someRequiredString");
    }
  }
}
