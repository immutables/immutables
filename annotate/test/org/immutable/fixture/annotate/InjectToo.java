package org.immutable.fixture.annotate;

import org.immutables.annotate.InjectAnnotation;
import org.immutables.annotate.InjectAnnotation.Where;

@InjectAnnotation(type = TooThese.class, target = {Where.CONSTRUCTOR})
public @interface InjectToo {
}
