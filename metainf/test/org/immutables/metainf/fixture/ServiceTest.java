/*
   Copyright 2015 Immutables Authors and Contributors

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
package org.immutables.metainf.fixture;

import com.google.common.base.Functions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import java.io.Serializable;
import java.util.ServiceLoader;
import java.util.TreeSet;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class ServiceTest {

  @Test
  public void serviceSets() {
    check(sortedToStringsFrom(ServiceLoader.load(Runnable.class))).isOf("Otherserv", "Servrun");
    check(sortedToStringsFrom(ServiceLoader.load(Serializable.class))).isOf("Serserv");
  }

  private TreeSet<String> sortedToStringsFrom(Iterable<?> iterable) {
    return Sets.newTreeSet(FluentIterable.from(iterable).transform(Functions.toStringFunction()));
  }
}
