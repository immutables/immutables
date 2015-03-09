/**
 * This package tests how one abstract value type references other,
 * yet to be generated, immutable implementation class.
 */
@Value.Style(
    visibility = ImplementationVisibility.PUBLIC,
    typeAbstract = "Abstract*",
    typeImmutable = "*")
package org.immutables.fixture.couse;

import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

