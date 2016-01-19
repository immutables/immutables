/*
    Copyright 2015 Immutables Authors and Contributors

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
package android.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

// Custom android-like nullable which does not support ElementType.LOCAL_VARIABLE
/**
 * The Interface Nullable.
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
public @interface Nullable {}
