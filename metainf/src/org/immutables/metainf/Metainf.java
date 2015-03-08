/*
    Copyright 2015 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.metainf;

import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.Documented;

/**
 * @see Metainf.Service
 */
@Retention(RetentionPolicy.SOURCE)
@Target({})
public @interface Metainf {
  /**
   * {@code META-INF/services/<interface.fully.qualified.name>} entries in classpath will be
   * generated with fully qualified name of annotated class as content line. Multiple meta-services
   * by the same interface will end up on separate lines in the same meta-inf service file.
   * <p>
   * Most other such generators either allow only single service type to be specified or just do
   * autodetection of all interfaces. This generator supports autodetection of implemented inter
   * (the default) or to override with one or more specific interfaces or classes.
   * <p>
   * 
   * <pre>
   * package a;
   * &#064;Provider
   * &#064;Metainf.Service
   * public class JaxrsMessageBodyProvider implements MessageBodyReader&lt;Object&gt;, MessageBodyWriter&lt;Object&gt; {
   * ...
   * }
   * </pre>
   * 
   * Both {@code META-INF/services/javax.ws.rs.ext.MessageBodyReader} and
   * {@code META-INF/services/javax.ws.rs.ext.MessageBodyWriter} will be generated and will contain
   * line with fully qualified class name of {@code a.JaxrsMessageBodyProvider}.
   * 
   * <pre>
   * package b;
   * &#064;Provider
   * &#064;Metainf.Service(javax.ws.rs.ext.MessageBodyReader.class)
   * public class JaxrsMessageBodyReader extends JaxrsMessageBodyProvider {
   * ...
   * }
   * </pre>
   * 
   * Only {@code META-INF/services/javax.ws.rs.ext.MessageBodyReader} will be generated and will
   * contain {@code b.JaxrsMessageBodyReader} line. If {@code JaxrsMessageBodyReader} and
   * {@code JaxrsMessageBodyProvider} will be compiled together, then
   * {@code META-INF/services/javax.ws.rs.ext.MessageBodyReader} file will have merged content, i.e.
   * it will contain two lines:
   * 
   * <pre>
   * a.JaxrsMessageBodyProvider
   * b.JaxrsMessageBodyReader
   * </pre>
   * <p>
   * @see #value()
   * @see java.util.ServiceLoader
   * @see java.util.ServiceLoader#load(Class)
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  public @interface Service {
    /**
     * If {@link #value()} attribute is empty (as by default) all implemented interfaces will
     * be taken into account, otherwise only specified implemented classes will be used.
     * @return service interfaces or abstract classes.
     */
    Class<?>[] value() default {};
  }
}
