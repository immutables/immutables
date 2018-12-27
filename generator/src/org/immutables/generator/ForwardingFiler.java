package org.immutables.generator;

import java.io.IOException;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileManager.Location;

public abstract class ForwardingFiler implements Filer {
  protected abstract Filer delegate();

  @Override
  public JavaFileObject createSourceFile(CharSequence name, Element... originatingElements) throws IOException {
    return delegate().createSourceFile(name, originatingElements);
  }

  @Override
  public JavaFileObject createClassFile(CharSequence name, Element... originatingElements) throws IOException {
    return delegate().createClassFile(name, originatingElements);
  }

  @Override
  public FileObject createResource(
      Location location,
      CharSequence pkg,
      CharSequence relativeName,
      Element... originatingElements)
      throws IOException {
    return delegate().createResource(location, pkg, relativeName, originatingElements);
  }

  @Override
  public FileObject getResource(Location location, CharSequence pkg, CharSequence relativeName) throws IOException {
    return delegate().getResource(location, pkg, relativeName);
  }
}