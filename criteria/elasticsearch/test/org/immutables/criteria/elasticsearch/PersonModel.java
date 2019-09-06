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

import com.google.common.collect.ImmutableMap;

import java.util.Map;

class PersonModel {

  // TODO automatically create mapping based on reflection
  static final Map<String, String> MAPPING = ImmutableMap.<String, String>builder()
          .put("id", "keyword")
          .put("isActive", "boolean")
          .put("fullName", "keyword")
          .put("nickName", "keyword")
          .put("age", "integer")
          .put("dateOfBirth", "date")
          .put("address.street", "keyword")
          .put("address.state", "keyword")
          .put("address.zip", "keyword")
          .put("address.city", "keyword")
          .build();
}
