package org.immutables.fixture.jackson3.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.immutables.value.Value;

import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 * Same shape as {@code packall.StrictStyle}, but leaves {@code get} at its default, so attributes
 * are taken from prefix-less accessors. Jackson cannot discover those by itself, so a value using
 * this style only round-trips if the processor also generates {@code @JsonProperty} for them.
 */
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
@JsonDeserialize
@JsonSerialize
@Value.Style(typeAbstract = "_*", typeImmutable = "Immutable_*")
public @interface PlainStyle {}
