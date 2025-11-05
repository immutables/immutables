package org.immutables.value.processor.encode;

import org.junit.Test;
import static org.immutables.check.Checkers.check;

public class MapperTest {
  @Test public void annotationOnMethodReturnType() {
    CharSequence source = "//source file"
        + "\npackage org.immutables.fixture.nullable.typeuse;"
        + "\nimport org.immutables.value.Value;"
        + "\nimport org.jspecify.annotations.NullMarked;"
        + "\nimport org.jspecify.annotations.Nullable;"
        + "\n"
        + "\n@NullMarked"
        + "\n@Value.Immutable"
        + "\npublic abstract class GenericVar<V> {"
        + "\n  public abstract @Nullable V value();"
        + "\n  public abstract V @Nullable [] arr();"
        + "\n  public abstract List<@Nullable V>  list();"
        + "\n  public abstract java.util.Map<@Nullable K, @SkipNulls V> map();"
        + "\n}"
        + "\n";

    SourceMapper mapper = new SourceMapper(source);

    Structurizer.Statement statement = mapper.get.apply("GenericVar.value");

    check(Code.join(statement.annotations())).is("@Nullable");
    check(Code.join(statement.returnType())).is("V");

    Structurizer.Statement statement2 = mapper.get.apply("GenericVar.map");

    check(Code.join(statement2.signature()))
        .is("publicabstractjava.util.Map<@Nullable K, @SkipNulls V>map");

    Structurizer.Statement statement3 = mapper.get.apply("GenericVar.arr");

    check(Code.join(statement3.signature()))
        .is("publicabstractV@Nullable[]arr");
    check(Code.join(statement3.returnType()))
        .is("V[]");
  }
}
