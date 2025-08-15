import org.immutables.value.Value;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked // JSpecify type use annotation
@Value.Builder
public record RecInUnnamed(int a,
    @Nullable ImmutablePlainInUnnamed ma
    //ImmutablePlainInUnnamedGeneric<String> mm
) {}
