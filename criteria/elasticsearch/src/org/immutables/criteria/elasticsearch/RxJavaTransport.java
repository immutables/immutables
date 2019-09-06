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

import io.reactivex.Single;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;

import java.util.Objects;

/**
 * Wrapper for {@link RestClient} which uses RxJava types
 */
class RxJavaTransport {

  private final RestClient restClient;

  RxJavaTransport(RestClient restClient) {
    this.restClient = Objects.requireNonNull(restClient, "restClient");
  }

  Single<Response> execute(Request request) {
    Objects.requireNonNull(request, "request");
    return Single.create(source -> {
      restClient.performRequestAsync(request, new ResponseListener() {
        @Override
        public void onSuccess(Response response) {
          source.onSuccess(response);
        }

        @Override
        public void onFailure(Exception exception) {
          source.onError(exception);
        }
      });
    });
  }

}
