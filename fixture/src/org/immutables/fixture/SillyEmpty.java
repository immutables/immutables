package org.immutables.fixture;

import org.immutables.value.Value;

@Value.Immutable(singleton = true, builder = false, intern = true)
public abstract class SillyEmpty {}
