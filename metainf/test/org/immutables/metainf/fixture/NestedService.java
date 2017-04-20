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

import org.immutables.metainf.Metainf;

public class NestedService {
  @Metainf.Service
  public static class Service implements AutoCloseable {
    @Override
    public void close() {}

    @Override
    public String toString() {
      String className = getClass().getCanonicalName();
      String packageName = getClass().getPackage().getName();
      return className.substring(packageName.length() + 1);
    }
  }
}
