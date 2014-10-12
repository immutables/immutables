package org.immutables.generate.silly;

import org.immutables.value.Value;

@Value.Immutable(singleton = true, builder = false, intern = true)
public abstract class SillyEmpty {}
