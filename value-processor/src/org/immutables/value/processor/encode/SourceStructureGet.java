package org.immutables.value.processor.encode;

public final class SourceStructureGet {
  private final SourceMapper mapper;

  public SourceStructureGet(CharSequence input) {
    this.mapper = new SourceMapper(input);
  }

  public String getReturnType(String path) {
    return Code.join(mapper.getReturnType(path));
  }

  @Override
  public String toString() {
    return mapper.definitions.toString();
  }
}
