package org.immutables.fixture.nullable;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierNickname;
import javax.annotation.meta.When;

@TypeQualifierNickname
@Nonnull(when = When.MAYBE)
@interface CheckForNull {

}
