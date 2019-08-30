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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.client.RestClient;
import org.immutables.criteria.backend.Backend;
import org.immutables.value.Value;

/**
 * Setup configuration to connect to Elastic search cluster
 */
@Value.Style(visibility = Value.Style.ImplementationVisibility.PACKAGE)
@Value.Immutable
public interface ElasticsearchSetup extends Backend.Setup {

  @Value.Parameter
  RestClient restClient();

  @Value.Default
  default ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  @Value.Default
  default IndexResolver resolver() {
    return IndexResolver.defaultResolver();
  }

  @Value.Default
  default int scrollSize() {
    return 1024;
  }

  static Builder builder(RestClient restClient) {
    return new Builder().restClient(restClient);
  }

  class Builder extends ImmutableElasticsearchSetup.Builder {

  }

}
