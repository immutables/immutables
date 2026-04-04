package org.immutables.fixture.jackson3.packall;

import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.immutables.value.Value;

@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
@JsonDeserialize
@JsonSerialize
@Value.Style(
    get = {"is*", "get*"},
    init = "set*",
    addAll = "set*",
    putAll = "set*",
    typeAbstract = {"_*"},
    typeImmutable = "Immutable_*",
    forceJacksonPropertyNames = false,
    defaultAsDefault = false,
    validationMethod = Value.Style.ValidationMethod.SIMPLE,
    jakarta = true,
    strictBuilder = true,
    visibility = Value.Style.ImplementationVisibility.PUBLIC)
public @interface StrictStyle {}
