/*
   Copyright 2017 Immutables Authors and Contributors

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
package org.immutables.fixture;

import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public interface Redacted {
  long id();

  @Value.Redacted
  int code();

  @Value.Redacted
  String ssn();

  @Value.Redacted
  String[] data();

  /** Attribute is not completely removed, but it's value replaced with a mask. */
  @Value.Immutable
  @Value.Style(redactedMask = "####")
  public interface RedactedMask {
    @Value.Redacted
    int code();
  }

  /**
   * For JDK only string generation strategy is different: concatenation is used.
   */
  @Value.Immutable
  @Value.Style(redactedMask = "$$$$", jdkOnly = true)
  public interface RedactedMaskJdkOnly {
    @Value.Redacted
    int code();
  }

  /**
   * For JDK only and in the presense of optional elements generation strategy is different:
   * StringBuilder is used.
   */
  @Value.Immutable
  @Value.Style(redactedMask = "????", jdkOnly = true)
  public interface RedactedMaskJdkOnlyOpt {
    @Value.Redacted
    int code();

    Optional<String> opt();
  }
}
