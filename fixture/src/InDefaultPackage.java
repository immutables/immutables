import org.immutables.annotation.GenerateImmutable;

@GenerateImmutable
interface InDefaultPackage {
  @GenerateImmutable
  static class ButNested {}
}
