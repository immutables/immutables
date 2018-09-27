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
package org.immutables.fixture.annotation;

import org.immutables.value.Value;

// Compilation should not fail when equals use hashCode
// field to short circuit attribute comparison
@Value.Immutable(prehash = true)
public @interface PrehashAnnotation {}

// No attibutes, no prehashing, just zero
@Value.Immutable(prehash = true)
@interface PrehashAnnotationEmpty {}

//No attibutes, no prehashing
@Value.Immutable(prehash = true)
interface PrehashRegularEmpty {}
