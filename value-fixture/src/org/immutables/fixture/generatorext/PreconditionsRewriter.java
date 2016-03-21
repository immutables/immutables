/*
   Copyright 2016 Immutables Authors and Contributors

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
package org.immutables.fixture.generatorext;

import java.util.Objects;
import org.immutables.extgenerator.GeneratedImportsModifier;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.immutables.metainf.Metainf;

@Metainf.Service
public class PreconditionsRewriter implements GeneratedImportsModifier {
  private static final String THIS_PACKAGE_NAME = PreconditionsRewriter.class.getPackage().getName();
  private static final String UTIL_OBJECTS = Objects.class.getCanonicalName();

  @Override
  public void modify(String packageOfGeneratedFile, Set<String> imports) {
    if (packageOfGeneratedFile.equals(THIS_PACKAGE_NAME)) {
      List<String> newImports = new ArrayList<>(imports.size());
      for (String importString : imports) {
        if (!importString.equals(UTIL_OBJECTS)) {
          newImports.add(importString);
        }
      }
      imports.clear();
      imports.addAll(newImports);
    }
  }
}
