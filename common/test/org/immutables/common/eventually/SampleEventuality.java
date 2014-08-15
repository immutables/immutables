package org.immutables.common.eventually;

import org.immutables.common.eventually.EventuallyProvides;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Exposed;
import com.google.inject.Singleton;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import static com.google.common.base.Preconditions.*;

@Singleton
public class SampleEventuality {

  @Inject
  List<Integer> tracker;

  @Exposed
  @EventuallyProvides
  ListenableFuture<Boolean> getInput(@Named("input") String input) {
    tracker.add(1);
    return Futures.immediateFuture(Boolean.parseBoolean(input));
  }

  @EventuallyProvides
  @Named("second")
  String getSecond(Boolean go) {
    tracker.add(2);
    return "second=" + go;
  }

  @EventuallyProvides
  @Named("first")
  String getFirst(Boolean go) {
    checkArgument(go);
    tracker.add(2);
    return "first";
  }

  @Exposed
  @EventuallyProvides
  @Named("separator")
  String separator() {
    tracker.add(0);
    return ":";
  }

  @Exposed
  @EventuallyProvides
  @Named("output")
  String output(
      @Named("first") String first,
      @Named("second") String second,
      @Named("separator") String separator) {
    tracker.add(3);
    return first + separator + second;
  }
}
