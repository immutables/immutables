package org.immutables.fixture.encoding;

import java.util.List;
import org.immutables.fixture.encoding.defs.MutableListEncodingEnabled;
import org.immutables.value.Value;

/**
 * Regression fixture for https://github.com/immutables/immutables/issues/1647 :
 * an encoded attribute with a default value (placed in the staged builder's
 * {@code BuildFinal} stage) must still expose its encoding's init methods there.
 */
@Value.Immutable
@Value.Style(stagedBuilder = true)
@MutableListEncodingEnabled
public interface UseMutableListStagedBuilder {
  String name();

  List<String> tags();
}
