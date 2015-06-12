package org.immutables.fixture.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.immutables.value.Value;

@Retention(RetentionPolicy.RUNTIME)
@interface A1 {}

@Retention(RetentionPolicy.RUNTIME)
@interface A2 {}

@Retention(RetentionPolicy.RUNTIME)
@interface B1 {}

// Should copy @A1 and @A2, but not other annotations
// to the implementation class
@A1
@A2
@B1
@Value.Immutable
@Value.Style(passAnnotations = {A1.class, A2.class})
interface AbstractValForPass {}
