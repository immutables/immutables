package org.immutables.value.processor.meta;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import org.immutables.generator.ExtensionLoader;

/**
 * Holder for some META-INF extension configured obscure features, which are problematic or not yet
 * ready to add to styles to add to {@code Value.Style}
 */
public final class ObscureFeatures {
  private static final String NO_DIAMONDS = "no-diamonds";

  private ObscureFeatures() {}

  private static final Supplier<ImmutableSet<String>> FEATURES =
      ExtensionLoader.findExtensions("META-INF/extensions/org.immutables.features");

  public static boolean noDiamonds() {
    return FEATURES.get().contains(NO_DIAMONDS);
  }
}
