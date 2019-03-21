/*
   Copyright 2013-2018 Immutables Authors and Contributors

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

package org.immutables.criteria;

/**
 * Base class for direct attribute comparison (like {@code foo = 'bar'}).
 *
 * @param <C> Criteria self-type, allowing {@code this}-returning methods to avoid needing subclassing
 * @param <T> type of the document being evaluated by this criteria
 */
public interface ValueCriteria<C extends DocumentCriteria<C, T>, T> {

}
