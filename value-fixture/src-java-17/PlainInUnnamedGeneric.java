import org.immutables.value.Value;

@Value.Immutable
public interface PlainInUnnamedGeneric<T> {
  T t();
}
