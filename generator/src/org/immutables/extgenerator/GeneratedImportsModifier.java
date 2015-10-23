package org.immutables.extgenerator;

import java.util.ServiceLoader;
import java.util.Set;

/**
 * Ad-hoc tool to change imports in generated source files after imports have been processed.
 * The annotation processor with load implementations of this
 * interface using classpath service loading, see {@link ServiceLoader} for the details.
 * Make sure it will be present of the annotation processor classpath rather than compile classpath
 * if they are not the same in your build tool. Implementations should take special care of safety,
 * i.e. they should neither throw exception during construction or processing nor leave set of
 * imports in an improperly modified state.
 * @since 2.1
 */
public interface GeneratedImportsModifier {
  /**
   * Modify imports set after import processing/extraction but before writing generated files.
   * Important to note that there are no way to change classnames as other parts of source file
   * refers to class by simple name. However package-name part of imports may be rewritten.
   * @param packageOfGeneratedFile informative package name of the generated file, may be used to
   *          employ different strategies for different packages.
   * @param imports modifiables set of imports
   */
  void modify(String packageOfGeneratedFile, Set<String> imports);
}
