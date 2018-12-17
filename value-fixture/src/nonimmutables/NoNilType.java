package nonimmutables;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

// Type annotation to check for sloppy typechecks which can take
// type mirror's toString which can include printed type annotation.
@Target(ElementType.TYPE_USE)
public @interface NoNilType {}
