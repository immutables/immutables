package org.immutables.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This meta-annotation indicated (currently only documents) that annotation effect will be applied
 * to abstract value types that nested.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.ANNOTATION_TYPE)
@interface AppliesToNested {}
