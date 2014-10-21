/*
    Copyright 2013-2014 Immutables.org authors

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
package org.immutables.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation could be applied to top level class which contains nested abstract value types.
 * Immutable implementation classes will be generated as classes nested into special "umbrella" top
 * level class, essentialy named after annotated class with "Immutable" prefix. This could mix with
 * {@link GenerateImmutable} annotation, so immutable implementation class will contains nested
 * immutable implementation classes.
 * <p>
 * Implementation classes nested under top level class with "Immutable" prefix
 * <ul>
 * <li>Have simple names without "Immutable" prefix
 * <li>Could be star-imported for easy clutter-free usage.
 * </ul>
 * <p>
 * 
 * <pre>
 * {@literal @}GenerateNested
 * class GraphPrimitives {
 *   {@literal @}GenerateImmutable
 *   interace Vertex {}
 *   {@literal @}GenerateImmutable
 *   static class Edge {}
 * }
 * ...
 * import ...ImmutableGraphPrimitives.*;
 * ...
 * Edge.builder().build();
 * Vertex.builder().build();
 * </pre>
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Deprecated
public @interface GenerateNested {}
