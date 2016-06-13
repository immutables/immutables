@Value.Style(
        typeAbstract = "_*",
        add = "*",
        defaults = @Value.Immutable(copy = false),
        depluralize = true,
        depluralizeDictionary = "status:statuses",
        put = "*",
        typeImmutable = "*",
        visibility = ImplementationVisibility.PUBLIC
)
package org.immutables.fixture.jackson.poly2;

import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;
