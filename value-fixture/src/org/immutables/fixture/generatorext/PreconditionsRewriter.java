package org.immutables.fixture.generatorext;

import org.immutables.extgenerator.GeneratedImportsModifier;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.immutables.metainf.Metainf;

@Metainf.Service
public class PreconditionsRewriter implements GeneratedImportsModifier {
  private static final String THIS_PACKAGE_NAME = PreconditionsRewriter.class.getPackage().getName();
  private static final String GUAVA_PRECONDITIONS = Preconditions.class.getCanonicalName();

  @Override
  public void modify(String packageOfGeneratedFile, Set<String> imports) {
    if (packageOfGeneratedFile.equals(THIS_PACKAGE_NAME)) {
      List<String> newImports = new ArrayList<>(imports.size());
      for (String importString : imports) {
        if (!importString.equals(GUAVA_PRECONDITIONS)) {
          newImports.add(importString);
        }
      }
      imports.clear();
      imports.addAll(newImports);
    }
  }
}
