/*
 * Copyright 2019 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.criteria.elasticsearch;

import org.immutables.criteria.backend.ContainerNaming;

public interface IndexResolver {

  /**
   * Resolve elastic index name given a class
   * @param entityType
   */
  String resolve(Class<?> entityType);

  /**
   * Default resolver which uses lower case and simple name of the class: {@code org.MyClass -> myclass}
   *
   * <p>In elastic, indexes have to be lowercase. From <a href="https://www.elastic.co/guide/en/elasticsearch/guide/current/_document_metadata.html#_index">documentation</a>:
   * {@code This name must be lowercase, cannot begin with an underscore, and cannot contain commas.}
   * </p>
   */
  static IndexResolver defaultResolver() {
    return entity -> ContainerNaming.DEFAULT.name(entity).toLowerCase();
  }

}
