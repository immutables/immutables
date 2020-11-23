/*
 * Copyright 2020 Immutables Authors and Contributors
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

package org.immutables.criteria.reflect;

import com.google.common.collect.ImmutableList;

import javax.annotation.concurrent.ThreadSafe;
import java.lang.reflect.Member;
import java.util.Iterator;
import java.util.List;

/**
 * Fetches all members during construction, returning cached view afterwards.
 */
@ThreadSafe
class PrecachedClassScanner implements ClassScanner {
  private final List<Member> members;

  PrecachedClassScanner(ClassScanner delegate) {
    // get members snapshot
    this.members = ImmutableList.copyOf(delegate.iterator());
  }

  @Override
  public Iterator<Member> iterator() {
    return members.iterator();
  }

  static ClassScanner of(ClassScanner delegate) {
    if (delegate instanceof PrecachedClassScanner) {
      return delegate;
    }

    return new PrecachedClassScanner(delegate);
  }
}
