/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

/*
 * The code was inspired by the similarly named JCTools class:
 * https://github.com/JCTools/JCTools/blob/master/jctools-core/src/main/java/org/jctools/queues/atomic
 */

package org.immutables.criteria.internal.reactive;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * A Single-Producer-Single-Consumer queue backed by a pre-allocated buffer.
 * <p>
 * This implementation is a mashup of the <a href="http://sourceforge.net/projects/mc-fastflow/">Fast Flow</a>
 * algorithm with an optimization of the offer method taken from the <a
 * href="http://staff.ustc.edu.cn/~bhua/publications/IJPP_draft.pdf">BQueue</a> algorithm (a variation on Fast
 * Flow), and adjusted to comply with Queue.offer semantics with regards to capacity.<br>
 * For convenience the relevant papers are available in the resources folder:<br>
 * <i>2010 - Pisa - SPSC Queues on Shared Cache Multi-Core Systems.pdf<br>
 * 2012 - Junchang- BQueue- Efficient and Practical Queuing.pdf <br>
 * </i> This implementation is wait free.
 *
 * @param <E> the element type of the queue
 */
final class SpscArrayQueue<E> extends AtomicReferenceArray<E> implements SimpleQueue<E> {
  private static final long serialVersionUID = -1296597691183856449L;
  private static final Integer MAX_LOOK_AHEAD_STEP = Integer.getInteger("jctools.spsc.max.lookahead.step", 4096);
  final int mask;
  final AtomicLong producerIndex;
  long producerLookAhead;
  final AtomicLong consumerIndex;
  final int lookAheadStep;

  SpscArrayQueue(int capacity) {
    super(roundToPowerOfTwo(capacity));
    this.mask = length() - 1;
    this.producerIndex = new AtomicLong();
    this.consumerIndex = new AtomicLong();
    lookAheadStep = Math.min(capacity / 4, MAX_LOOK_AHEAD_STEP);
  }

  /**
   * Find the next larger positive power of two value up from the given value. If value is a power of two then
   * this value will be returned.
   *
   * @param value from which next positive power of two will be found.
   * @return the next positive power of 2 or this value if it is a power of 2.
   */
  private static int roundToPowerOfTwo(final int value) {
    return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
  }

  @Override
  public boolean offer(E e) {
    if (null == e) {
      throw new NullPointerException("Null is not a valid element");
    }
    // local load of field to avoid repeated loads after volatile reads
    final int mask = this.mask;
    final long index = producerIndex.get();
    final int offset = calcElementOffset(index, mask);
    if (index >= producerLookAhead) {
      int step = lookAheadStep;
      if (null == lvElement(calcElementOffset(index + step, mask))) { // LoadLoad
        producerLookAhead = index + step;
      } else if (null != lvElement(offset)) {
        return false;
      }
    }
    soElement(offset, e); // StoreStore
    soProducerIndex(index + 1); // ordered store -> atomic and ordered for size()
    return true;
  }

  @Override
  public boolean offer(E v1, E v2) {
    // FIXME
    return offer(v1) && offer(v2);
  }

  @Override
  public E poll() {
    final long index = consumerIndex.get();
    final int offset = calcElementOffset(index);
    // local load of field to avoid repeated loads after volatile reads
    final E e = lvElement(offset); // LoadLoad
    if (null == e) {
      return null;
    }
    soConsumerIndex(index + 1); // ordered store -> atomic and ordered for size()
    soElement(offset, null); // StoreStore
    return e;
  }

  @Override
  public boolean isEmpty() {
    return producerIndex.get() == consumerIndex.get();
  }

  void soProducerIndex(long newIndex) {
    producerIndex.lazySet(newIndex);
  }

  void soConsumerIndex(long newIndex) {
    consumerIndex.lazySet(newIndex);
  }

  @Override
  public void clear() {
    // we have to test isEmpty because of the weaker poll() guarantee
    while (poll() != null || !isEmpty()) { } // NOPMD
  }

  int calcElementOffset(long index, int mask) {
    return (int)index & mask;
  }

  int calcElementOffset(long index) {
    return (int)index & mask;
  }

  void soElement(int offset, E value) {
    lazySet(offset, value);
  }

  E lvElement(int offset) {
    return get(offset);
  }
}

