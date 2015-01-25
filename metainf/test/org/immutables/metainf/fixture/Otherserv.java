package org.immutables.metainf.fixture;

import java.io.Serializable;
import org.immutables.metainf.Metainf;

@Metainf.Service(Runnable.class)
public class Otherserv implements Runnable, Serializable {
  @Override
  public void run() {

  }
  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
