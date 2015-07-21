package org.immutables.fixture.annotation;

import nonimmutables.A1;
import nonimmutables.A2;
import nonimmutables.B1;
import org.immutables.value.Value;

// Should copy @A1 and @A2, but not other annotations
// to the implementation class
@A1
@A2
@B1
@Value.Immutable
@Value.Style(passAnnotations = {A1.class, A2.class})
interface AbstractValForPass {}
