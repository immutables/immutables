package org.immutables.generator.fixture;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.immutables.generator.Generator;
import org.immutables.generator.AbstractTemplate;

@Generator.Template
public class Whitespacer extends AbstractTemplate {
  public final List<Integer> points = ImmutableList.of(1, 2, 3, 5, 8, 13);
}
