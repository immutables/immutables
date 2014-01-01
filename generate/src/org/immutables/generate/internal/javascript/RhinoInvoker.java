/*
    Copyright 2013-2014 Immutables.org authors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.generate.internal.javascript;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import com.google.common.io.InputSupplier;
import com.google.common.io.Resources;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrappedException;
import org.mozilla.javascript.commonjs.module.Require;
import org.mozilla.javascript.commonjs.module.RequireBuilder;
import org.mozilla.javascript.commonjs.module.provider.ModuleSourceProvider;

public class RhinoInvoker {

  private static final int LEVEL_INTERPRET_UNOPTIMIZED = -1;
  private static final int LEVEL_COMPILE_OPTIMIZED = 9;

  private static final String MODULE_SOURCE_PROVIDER_VARIABLE_NAME = "__moduleSourceProvider__";
  private static final String PARAMETERS_VARIABLE_NAME = "parameters";
  private static final int FIRST_LINE_NUMBER = 0;

  private static final URL PREDEFINED_SCRIPT_RESOURCE =
      RhinoInvoker.class.getResource("predefined.js");

  private static final InputSupplier<InputStreamReader> PREDEFINED_SCRIPT_READER =
      Resources.newReaderSupplier(PREDEFINED_SCRIPT_RESOURCE, Charsets.UTF_8);

  private static final Object NULL_SECURITY_DOMAIN = null;

  private PrecompilingScriptProvider scriptProvider;
  private RhinoContextFactory contextFactory;
  private Scriptable sharedScope;
  private final ModuleSourceProvider sourceProvider;

  public RhinoInvoker(ModuleSourceProvider sourceProvider) {
    this(sourceProvider, ImmutableMap.<String, Object>of());
  }

  public RhinoInvoker(ModuleSourceProvider sourceProvider, Map<String, Object> globals) {
    this.sourceProvider = sourceProvider;
    this.scriptProvider = new PrecompilingScriptProvider(sourceProvider, false);
    this.contextFactory = new RhinoContextFactory();
    this.sharedScope = createSharedTopLevelScope(globals);
  }

  private Scriptable parametrizedScope(Context context, Map<String, Object> parameters) {
    if (parameters.isEmpty()) {
      return sharedScope;
    }
    Scriptable scopeObject = newScope(context, sharedScope);
    scopeObject.put(PARAMETERS_VARIABLE_NAME, scopeObject, parameters);
    return scopeObject;
  }

  private Scriptable newScope(Context context, Scriptable prototype) {
    Scriptable scope = context.newObject(prototype);
    scope.setPrototype(prototype);
    return scope;
  }

  private Scriptable createSharedTopLevelScope(Map<String, Object> globals) {
    Context context = contextFactory.enterContext();
    context.setOptimizationLevel(LEVEL_COMPILE_OPTIMIZED);
    try {
      ScriptableObject standardObjectsScope = context.initStandardObjects();
      standardObjectsScope.sealObject();

      Scriptable scopeObject = newScope(context, standardObjectsScope);

      Require requireScriptFunction = new RequireBuilder()
          .setSandboxed(false)
          .setModuleScriptProvider(scriptProvider)
          .createRequire(context, scopeObject);

      requireScriptFunction.install(scopeObject);

      scopeObject.put(MODULE_SOURCE_PROVIDER_VARIABLE_NAME, scopeObject, loadJsResourceFunction());

      putVariablesInScope(globals, scopeObject);

      context.evaluateReader(scopeObject,
          PREDEFINED_SCRIPT_READER.getInput(),
          PREDEFINED_SCRIPT_RESOURCE.toString(),
          FIRST_LINE_NUMBER,
          NULL_SECURITY_DOMAIN);

      return scopeObject;
    } catch (RhinoException ex) {
      throw newEvaluationException(ex);
    } catch (IOException ex) {
      throw Throwables.propagate(ex);
    } finally {
      Context.exit();
    }
  }

  private void putVariablesInScope(Map<String, Object> variables, Scriptable scopeObject) {
    for (Entry<String, Object> e : variables.entrySet()) {
      scopeObject.put(e.getKey(), scopeObject, e.getValue());
    }
  }

  private Function<Object, Object> loadJsResourceFunction() {
    return new Function<Object, Object>() {
      @Override
      public Object apply(Object input) {
        try {
          return CharStreams.readLines(sourceProvider.loadSource(input.toString(), null, null).getReader());
        } catch (Exception ex) {
          throw Throwables.propagate(ex);
        }
      }
    };
  }

  private void executeModuleCustom(String module, Map<String, Object> parameters, String moduleScript) {
    executeCustom(
        "invokemodule:" + module,
        parameters,
        "{ let moduleExports = require('" + module + "'); " + moduleScript + " }", Object.class);
  }

  public Object executeModuleAsJavaAdapter(String module, Class<?> javaInterface) {
    return executeCustom(
        "invokemodulebean:" + module,
        ImmutableMap.<String, Object>of(),
        "new Packages." + javaInterface.getName() + "(require('" + module + "'))", javaInterface);
  }

  public Object executeCustom(
      String scriptName,
      Map<String, Object> parameters,
      String processingScript,
      Class<?> desiredType) {
    Context context = contextFactory.enterContext();
    try {
      return Context.jsToJava(
          context.evaluateString(
              parametrizedScope(context, parameters),
              processingScript,
              scriptName,
              FIRST_LINE_NUMBER,
              NULL_SECURITY_DOMAIN),
          desiredType);

    } catch (RhinoException ex) {
      throw newEvaluationException(ex);
    } finally {
      Context.exit();
    }
  }

  public void executeModuleMain(String module, Object... mainArguments) {
    executeModuleCustom(module,
        ImmutableMap.<String, Object>of("mainArguments", mainArguments),
        "moduleExports.main.apply(moduleExports, parameters.mainArguments)");
  }

  public void executeModule(String module) {
    executeModuleCustom(module, ImmutableMap.<String, Object>of(), "");
  }

  private class RhinoContextFactory extends ContextFactory {

    @Override
    protected Context makeContext() {
      Context context = super.makeContext();
      context.setLanguageVersion(Context.VERSION_1_8);
      context.setOptimizationLevel(LEVEL_INTERPRET_UNOPTIMIZED);
      context.setWrapFactory(new DefaultWrapFactory());
      return context;
    }

    @Override
    protected boolean hasFeature(Context cx, int featureIndex) {
      switch (featureIndex) {
      case Context.FEATURE_TO_STRING_AS_SOURCE:
      case Context.FEATURE_E4X:
      case Context.FEATURE_STRICT_MODE:
      case Context.FEATURE_LOCATION_INFORMATION_IN_ERROR:
        return true;
      default:
        return super.hasFeature(cx, featureIndex);
      }
    }
  }

  // TODO move all this error printing stuff this to exception class or special diagnostic class

  private RhinoEvaluationException newEvaluationException(RhinoException ex) {
    RhinoEvaluationException evaluationException = new RhinoEvaluationException(
        formatMessageWithSourceLocation(ex),
        ex instanceof WrappedException ? ((WrappedException) ex).getWrappedException() : ex);
    evaluationException.setStackTrace(new StackTraceElement[0]);
    return evaluationException;
  }

  private String formatMessageWithSourceLocation(RhinoException ex) {
    String lineSource = insertColumnMarkIfPossible(ex, prepareLineSource(ex));

    String message = "" + ex.details()
        + "\n line " + ex.lineNumber() + " in " + extractFilename(ex.sourceName())
        + "\n ...\t\t" + lineSource
        + "\n"
        + "\n" + ex.getScriptStackTrace();

    return message;
  }

  private String extractFilename(String sourceName) {
    int indexOfLastPathSegment = sourceName.lastIndexOf('/') + 1;
    if (indexOfLastPathSegment > 0 && indexOfLastPathSegment < sourceName.length()) {
      return sourceName.substring(indexOfLastPathSegment);
    }
    return sourceName;
  }

  private String prepareLineSource(RhinoException ex) {
    String lineSource = ex.lineSource();

    if (lineSource == null && ex.lineNumber() > 0) {
      ex.initLineSource(lineSource = extractLineSource(ex, ex.sourceName()));
    }

    return ex.lineSource();
  }

  private String extractLineSource(RhinoException e, String scriptUri) {
    try {
      return Resources.readLines(new URI(scriptUri).toURL(), Charsets.UTF_8).get(e.lineNumber() - 1);
    } catch (Exception ex) {
      return "";
    }
  }

  private String insertColumnMarkIfPossible(RhinoException e, String lineOfSource) {
    if (!Strings.isNullOrEmpty(lineOfSource)) {
      try {
        int columnNumber = e.columnNumber();
        if (columnNumber > 0) {
          lineOfSource = lineOfSource.substring(0, columnNumber) + "_" +
              lineOfSource.substring(columnNumber);
        }
      } catch (Exception columnMarkMismatch) {
      }
    }
    return lineOfSource;
  }
}
