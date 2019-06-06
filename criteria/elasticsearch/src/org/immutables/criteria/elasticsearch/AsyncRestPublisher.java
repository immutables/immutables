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

import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Objects;

/**
 * Primitive implementation of a reactive publisher for a particular request
 */
class AsyncRestPublisher implements Publisher<Response> {

  private final RestClient restClient;
  private final Request request;

  AsyncRestPublisher(RestClient restClient, org.elasticsearch.client.Request request) {
    this.restClient = Objects.requireNonNull(restClient, "restClient");
    this.request = Objects.requireNonNull(request, "request");
  }

  @Override
  public void subscribe(final Subscriber<? super Response> s) {
    new SubscriptionImpl(s).init();
  }

  final class SubscriptionImpl implements Subscription  {

    private final Subscriber<? super Response> subscriber;

    private boolean subscribed = false;
    private boolean cancelled = false;

    SubscriptionImpl(final Subscriber<? super Response> subscriber) {
      this.subscriber = Objects.requireNonNull(subscriber, "subscriber");;
    }

    private void init() {
      subscriber.onSubscribe(this);
    }

    @Override
    public void request(long n) {
      if (n < 1) {
        subscriber.onError(new IllegalArgumentException(subscriber + " violated the Reactive Streams rule 3.9 by requesting a non-positive number of elements."));
        return;
      }

      if (subscribed || cancelled) {
        return;
      }

      subscribed = true;

      restClient.performRequestAsync(request, new ResponseListener() {
        @Override
        public void onSuccess(Response response) {
          doSend(response);
          subscriber.onComplete();
        }

        @Override
        public void onFailure(Exception exception) {
          terminateDueTo(exception);
        }
      });
    }

    private void doSend(Response response) {
      if (!cancelled) {
        try {
          subscriber.onNext(response);
        } catch (Throwable t) {
          cancel();
          (new IllegalStateException(subscriber + " violated the Reactive Streams rule 2.13 by throwing an exception from onError.", t)).printStackTrace(System.err);
        }
      }
    }

    private void terminateDueTo(final Throwable t) {
      cancel();
      try {
        subscriber.onError(t);
      } catch (final Throwable t2) {
        (new IllegalStateException(subscriber + " violated the Reactive Streams rule 2.13 by throwing an exception from onError.", t2)).printStackTrace(System.err);
      }
    }

    @Override
    public void cancel() {
      cancelled = true;
    }
  };
}