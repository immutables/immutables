package org.immutables.service.concurrent;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Stage;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.inject.Inject;
import javax.inject.Named;
import org.immutables.common.concurrent.FluentFutures;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class EventualProviderTest extends AbstractModule {

  private final SettableFuture<String> input = SettableFuture.create();
  private final List<Integer> tracker = new CopyOnWriteArrayList<>();

  @Test
  public void asyncComputationGraph() {
    Injector injector = Guice.createInjector(Stage.DEVELOPMENT, this);
    Verifiers verifiers = injector.getInstance(Verifiers.class);

    check(FluentFutures.from(verifiers.separator).getUnchecked()).is(":");
    check(tracker).isOf(0);
    check(!verifiers.output.isDone());

    // Without any input, only parameter-less computations are resolved
    input.set("true");

    check(FluentFutures.from(verifiers.output).getUnchecked()).is("first:second=true");
    check(tracker).isOf(0, 1, 2, 2, 3);
  }

  /** Checks injection of exposed futures */
  static class Verifiers {
    @Inject
    @Named("output")
    ListenableFuture<String> output;
    @Inject
    @Named("separator")
    ListenableFuture<String> separator;
  }

  @Override
  protected void configure() {
    install(EventualProvidersModule.from(SampleEventuality.class));
  }

  @Provides
  @Named("input")
  ListenableFuture<String> input() {
    return input;
  }

  @Provides
  List<Integer> tracker() {
    return tracker;
  }

}
