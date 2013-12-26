package org.immutables.service;

import org.immutables.service.Configurations;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class SillyConfigTest {

  private static final String CONFIG_CLASS = SillyConfig.class.getName();
  private static final String CONFIG_RESOURCE = CONFIG_CLASS.replace('.', '/') + ".conf";

  @Test
  public void readConfig() {
    check(Configurations.loadModule(CONFIG_RESOURCE, CONFIG_CLASS)).notNull();
  }

  @Test(expected = RuntimeException.class)
  public void wrongConfig() {
    Configurations.loadModule(CONFIG_RESOURCE + "nosuch", CONFIG_CLASS);
  }

  @Test(expected = RuntimeException.class)
  public void wrongClass() {
    Configurations.loadModule(CONFIG_RESOURCE, CONFIG_CLASS + "NoSuch");
  }
}
