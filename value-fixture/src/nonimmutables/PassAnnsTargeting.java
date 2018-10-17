/*
   Copyright 2018 Immutables Authors and Contributors

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
package nonimmutables;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.immutables.value.Value;

@Value.Style(passAnnotations = {
    A1.class,
    A2.class
})
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@interface PassAnns {}

// This tests verifies
@PassAnns
@A1
@A2
@Value.Immutable
public interface PassAnnsTargeting {
  @A1
  int a();

  @A2
  @Value.Default
  default int b() {
    return 111;
  }

  @A1
  @Value.Default
  default String c() {
    return "abc";
  }
}
