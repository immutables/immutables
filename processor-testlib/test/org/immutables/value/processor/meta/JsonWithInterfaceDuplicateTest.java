package org.immutables.value.processor.meta;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.immutables.value.Value;
import org.immutables.value.processor.Processor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.immutables.check.Checkers.check;

/**
 * Verifies that a Jackson-mapped value type which explicitly extends its own generated
 * With-interface compiles cleanly on incremental builds (where the With-interface is already
 * on the classpath). Two compilation passes simulate that condition.
 */
public class JsonWithInterfaceDuplicateTest {

  private static final String SOURCE = ""
      + "package repro;\n"
      + "import com.fasterxml.jackson.databind.annotation.JsonDeserialize;\n"
      + "import java.util.Map;\n"
      + "import org.immutables.value.Value;\n"
      + "@Value.Immutable\n"
      + "@Value.Style(jdkOnly = true)\n"
      + "@JsonDeserialize(as = ImmutableWithMapAndJson.class)\n"
      + "public interface WithMapAndJson extends WithWithMapAndJson {\n"
      + "  Map<String, Integer> items();\n"
      + "}\n";

  @Rule
  public final TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void noJsonDuplicateWitherOnIncrementalBuild() throws IOException {
    File annotations = codeSourceOf(Value.class);
    File jacksonDatabind = codeSourceOf(ObjectMapper.class);
    File jacksonAnnotations = codeSourceOf(JsonProperty.class);

    // Pass 1 — generates WithWithMapAndJson alongside ImmutableWithMapAndJson.
    File pass1Classes = folder.newFolder("pass1-classes");
    List<String> pass1Errors = compile(SOURCE, "repro.WithMapAndJson",
        pass1Classes, folder.newFolder("pass1-sources"),
        Arrays.asList(annotations, jacksonDatabind, jacksonAnnotations));
    check(pass1Errors.toString(), pass1Errors.isEmpty());

    // Pass 2 — WithWithMapAndJson is now on the classpath, so isGenerateWithInterface()
    // resolves via FQN. The generated Json class must still compile without duplicate methods.
    List<String> pass2Errors = compile(SOURCE, "repro.WithMapAndJson",
        folder.newFolder("pass2-classes"), folder.newFolder("pass2-sources"),
        Arrays.asList(annotations, jacksonDatabind, jacksonAnnotations, pass1Classes));
    check(pass2Errors.toString(), pass2Errors.isEmpty());
  }

  private static List<String> compile(
      String source,
      String typeName,
      File classOutput,
      File sourceOutput,
      List<File> classpath) throws IOException {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

    try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, null, null)) {
      files.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singleton(classOutput));
      files.setLocation(StandardLocation.SOURCE_OUTPUT, Collections.singleton(sourceOutput));
      files.setLocation(StandardLocation.CLASS_PATH, classpath);

      JavaCompiler.CompilationTask task = compiler.getTask(null, files, diagnostics,
          Collections.<String>emptyList(), null,
          Collections.singletonList(new StringSource(typeName, source)));
      task.setProcessors(Collections.singletonList(new Processor()));
      task.call();
    }

    List<String> errors = new ArrayList<>();
    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
      if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
        errors.add(diagnostic.toString());
      }
    }
    return errors;
  }

  private static File codeSourceOf(Class<?> type) {
    try {
      return new File(type.getProtectionDomain().getCodeSource().getLocation().toURI());
    } catch (URISyntaxException e) {
      throw new AssertionError(e);
    }
  }

  private static final class StringSource extends SimpleJavaFileObject {
    private final String source;

    StringSource(String typeName, String source) {
      super(URI.create("string:///" + typeName.replace('.', '/') + Kind.SOURCE.extension),
          Kind.SOURCE);
      this.source = source;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
      return source;
    }
  }
}
