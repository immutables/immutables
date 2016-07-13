@Value.Style(
		strictBuilder = true,
		attributelessSingleton = true,
		overshadowImplementation = true,
		visibility = Value.Style.ImplementationVisibility.PACKAGE,
		defaults = @Value.Immutable(builder = false))
package org.immutables.value.processor.encode;

import org.immutables.value.Value;
