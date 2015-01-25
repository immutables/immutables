package org.immutables.metainf.fixture;

import org.immutables.metainf.Metainf;

@Metainf.Service
public class Servrun implements Runnable {
  @Override
  public void run() {}

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
