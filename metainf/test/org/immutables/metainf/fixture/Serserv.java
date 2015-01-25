package org.immutables.metainf.fixture;

import org.immutables.metainf.Metainf;
import java.io.Serializable;

@Metainf.Service
public class Serserv implements Serializable {
  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
