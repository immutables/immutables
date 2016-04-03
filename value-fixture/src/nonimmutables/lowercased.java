package nonimmutables;

import nonimmutables.Uppercase.Just;
import org.immutables.value.Value;

// Compilation test for lowercased type and included uppercase package file
// via disabled import postprocessing
@Value.Immutable
@Value.Include(Just.class)
public interface lowercased {}
