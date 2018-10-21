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
package org.immutables.ordinal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public abstract class InterningOrdinalDomain<S, E extends OrdinalValue<E>> extends OrdinalDomain<E> {

  private final List<E> values = new CopyOnWriteArrayList<>();

  @GuardedBy("internedInstances")
  private final Map<S, E> internedInstances = new HashMap<>();

  public final E internOrdinal(S valueSample) {
    synchronized (internedInstances) {
      @Nullable E value = internedInstances.get(valueSample);
      if (value == null) {
        value = extractValue(valueSample, internedInstances.size());
        internedInstances.put(valueSample, value);
        values.add(value);
      }
      return value;
    }
  }

  protected abstract E extractValue(S valueSample, int ordinal);

  @Override
  public Iterator<E> iterator() {
    return values.iterator();
  }

  @Override
  public final E get(int ordinal) {
    return values.get(ordinal);
  }

  @Override
  public final int length() {
    return values.size();
  }
}
