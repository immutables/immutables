/*
   Copyright 2023 Immutables Authors and Contributors

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
package org.immutables.processor;

/**
 * Provides classes (and not just class names) of the call stack.
 * <p>
 * NOTE: Extending SecurityManager is a convenient hack that works for Java 8 through 19.
 * StackWalker would be preferred when the compatibility level gets raised to Java 9. 
 */
public class CallStack extends SecurityManager {

  public Class<?>[] getCallingClasses() {
    return getClassContext();
  }
}