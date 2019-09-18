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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.immutables.check.Checkers.check;

class VersionTest {

  @Test
  void version() {
    check(Version.of("1.2.3").major).is(1);
    check(Version.of("2.2.3").major).is(2);
    check(Version.of("7").major).is(7);
    check(Version.of("7.1").major).is(7);
    check(Version.of("7.1.2").major).is(7);
    check(Version.of("7.1-pre").major).is(7);
    check(Version.of("3-beta").major).is(3);
    check(Version.of("200-ea").major).is(200);
    check(Version.of("2014.12.12").major).is(2014);

    Assertions.assertThrows(IllegalArgumentException.class, () -> Version.of(""));
    Assertions.assertThrows(IllegalArgumentException.class, () -> Version.of("aa"));
    Assertions.assertThrows(IllegalArgumentException.class, () -> Version.of("a.b"));
    Assertions.assertThrows(IllegalArgumentException.class, () -> Version.of("."));
    Assertions.assertThrows(IllegalArgumentException.class, () -> Version.of(".."));
    Assertions.assertThrows(IllegalArgumentException.class, () -> Version.of(".2"));
  }
}
