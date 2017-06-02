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
package org.immutables.gson;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.ServiceLoader;
import org.immutables.gson.adapter.ExpectedSubtypesAdapter;

/**
 * Gson umbrella annotation used to group nested Gson-related annotations.
 * This class named in a good hope it will not clash with the usages of {@link com.google.gson.Gson}
 * .
 * @see TypeAdapters
 * @see Named
 * @see Ignore
 * @see ExpectedSubtypes
 */
@Retention(RetentionPolicy.SOURCE)
public @interface Gson {
  /**
   * Use on a top level class to generate type adapted factory supporting directly annotated and all
   * nested immutable types.
   * <p>
   * Type adapter factories are generated in the same package, named
   * {@code GsonAdapters[name_of_annotated_type]} and registered statically as service providers in
   * {@code META-INF/services/com.google.gson.TypeAdapterFactory}. The most easy way to register all
   * such factories using {@link ServiceLoader}.
   * 
   * <pre>
   * com.google.gson.GsonBuilder gsonBuilder = new com.google.gson.GsonBuilder();
   * for (TypeAdapterFactory factory : ServiceLoader.load(TypeAdapterFactory.class)) {
   *   gsonBuilder.registerTypeAdapterFactory(factory);
   * }
   * 
   * // Manual registration is also an option
   * gsonBuilder.registerTypeAdapterFactory(new GsonAdaptersMyDocument());
   * 
   * com.google.gson.Gson gson = gsonBuilder.create();
   * </pre>
   * <p>
   * Certain Gson options are supported for immutable objects in deliberate fashion:
   * <ul>
   * <li>{@link GsonBuilder#serializeNulls()} - When enabled, {@code null} fields and empty array
   * fields will be included, otherwise omited</li>
   * <li>{@link GsonBuilder#setFieldNamingStrategy(FieldNamingStrategy)} - Naming strategy could be
   * used if {@code @TypeAdapters(fieldNamingStrategy=true)}. See {@link #fieldNamingStrategy()} for
   * more information.</li>
   * </ul>
   * @see #metainfService()
   */
  @Documented
  @Target({ElementType.TYPE, ElementType.PACKAGE})
  public @interface TypeAdapters {
    /**
     * When {@code namingStrategy=true}, somewhat more involved code is generated to apply naming
     * strategies extracted from configured {@link com.google.gson.Gson} instance.
     * <p>
     * @see FieldNamingStrategy
     * @see FieldNamingPolicy
     * @see GsonBuilder#setFieldNamingPolicy(FieldNamingPolicy)
     * @return {@code true} if enabled, by default is {@code false}
     */
    boolean fieldNamingStrategy() default false;

    /**
     * When {@code emptyAsNulls=true}, empty arrays and objects will be omitted from output as
     * if they where {@code null}, both field name and value will be omited.
     * <p>
     * Note that {@code null} skipping behaviour is controlled by
     * {@link GsonBuilder#serializeNulls()}, which forces all nulls and empty arrays/objects to be
     * serialized.
     * @return {@code true} if enabled, by default is {@code false}
     */
    boolean emptyAsNulls() default false;

    /**
     * Enables treating {@code null} value as a marker that value is absent and default value should
     * be used for default attributes. This workds during reading/deserialization. If default value
     * is also nullable, then {@code null} will be set regardless of a default value.
     * @return {@code true} if enabled, by default is {@code false}
     */
    boolean nullAsDefault() default false;

    /**
     * You can set {@code metainfService = false} to disable generation of meta-inf services.
     * @return {@code true} if metainf services are enabled, by default is {@code true}
     */
    boolean metainfService() default true;
  }

  /**
   * Expected subtypes for serialization could be specified on attribute level or an abstract
   * supertype directly, however the former declaration site has precedence. It enables polymorphic
   * marshaling by structure. Subtype that matches JSON value will be returned, for the details
   * please see {@link ExpectedSubtypesAdapter}.
   * <p>
   * Note: when this annotation is used with {@link Map} attribute, it refers to types of values,
   * not keys.
   * <p>
   * <em>This functionality uses runtime support class and requires that this Gson integration
   * module
   * jar will be available at runtime.</em>
   * @see ExpectedSubtypesAdapter
   * @see #value()
   * @see Named
   */
  @Target({ElementType.METHOD, ElementType.TYPE})
  public @interface ExpectedSubtypes {

    /**
     * Specifies expected subclasses of an abstract type that is matched during parsing by
     * structural compatibility of settable attributes.
     * If all attributes of subclasses are the same, then it will result in error due to undecidable
     * situation.
     * @return subclasses of an abstract type
     */
    Class<?>[] value();
  }

  /**
   * Specify attribute's custom name in JSON representation.
   * <p>
   * This example used to define JSON attribute name as "_id" during marshaling and unmarshaling.
   * 
   * <pre>
   * &#064;Gson.Named(&quot;_id&quot;)
   * public abstract String id();
   * </pre>
   * <p>
   * <em>If you use Gson versions 2.5.0 and higher it's better to use {@link SerializedName}
   * instead.
   * This annotation is analogous to {@code SerializedName} but was created when
   * {@code SerializedName}
   * was applicable only to fields in older versions of Gson. You can think of this
   * {@code Gson.Named}
   * annotation as deprecated now.
   * </em>
   * @see SerializedName
   */
  @Documented
  @Target(ElementType.METHOD)
  public @interface Named {
    /**
     * @return custom name string.
     */
    String value();
  }

  /**
   * Indicates if marshaler should skip this attribute during marshaling.
   * Applies only to non-mandatory attributes.
   */
  @Documented
  @Target(ElementType.METHOD)
  public @interface Ignore {}
}
