import org.immutables.value.Value;

@Value.Immutable
interface InDefaultPackage {
  @Value.Immutable
  static class ButNested {}
}
