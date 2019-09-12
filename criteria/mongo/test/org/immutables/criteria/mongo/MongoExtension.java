/*
 * Copyright 2019 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.criteria.mongo;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoDatabase;
import io.reactivex.Flowable;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.util.Objects;

/**
 * JUnit5 extension which allows to test repository access backed by a real database (embedded/fake or remote MongoDB). It
 * is a good habit to run tests on different versions of the database. By default embedded (in memory) java server
 * is used.
 *
 * <p>If you want to connect to external mongo database use system property {@code mongo}.
 * With maven it will look something like this:
 * <pre>
 * {@code $ mvn test -DargLine="-Dmongo=mongodb://localhost"}
 * </pre>
 *
 * @see <a href="https://github.com/bwaldvogel/mongo-java-server">Mongo Java Server</a>
 **/
public class MongoExtension implements BeforeTestExecutionCallback, ParameterResolver  {

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(MongoExtension.class);

  private static final String KEY = "mongoDB";

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    final Class<?> type = parameterContext.getParameter().getType();
    return MongoDatabase.class.isAssignableFrom(type) || MongoClient.class.isAssignableFrom(type);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    final Class<?> type = parameterContext.getParameter().getType();
    if (MongoDatabase.class.isAssignableFrom(type)) {
      return getOrCreate(extensionContext).instance.database();
    } else if (MongoClient.class.isAssignableFrom(type)) {
      return getOrCreate(extensionContext).instance.client();
    }

    throw new ExtensionConfigurationException(String.format("%s supports only %s or %s but yours was %s", MongoExtension.class.getSimpleName(),
            MongoDatabase.class.getName(), MongoClient.class.getName(), type.getName()));
  }

  private MongoResource getOrCreate(ExtensionContext context) {
    return context.getRoot().getStore(NAMESPACE).getOrComputeIfAbsent(KEY, key -> new MongoResource(MongoInstance.create()), MongoResource.class);
  }

  @Override
  public void beforeTestExecution(ExtensionContext context) throws Exception {
    getOrCreate(context).clear();
  }

  private static class MongoResource implements ExtensionContext.Store.CloseableResource {

    private final MongoInstance instance;

    private MongoResource(MongoInstance instance) {
      this.instance = Objects.requireNonNull(instance, "instance");
    }

    private void clear() {
      // drop all collections
      MongoDatabase database = instance.database();
      Flowable.fromPublisher(database.listCollectionNames())
              .flatMap(col -> database.getCollection(col).drop())
              .toList()
              .blockingGet();
    }

    @Override
    public void close() throws Exception {
      instance.close();
    }
  }
}
