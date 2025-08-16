package nonimmutables;

import org.immutables.value.Value;

// Custom immutable annotations configured via -Aimmutables.annotation=nonimmutables.MyVal
// style meta-annotation
@Value.Style(typeImmutable = "MyVal*")
public @interface MyVal {}
