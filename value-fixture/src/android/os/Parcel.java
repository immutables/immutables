/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//THIS IS STUB!!!!
package android.os;

import java.io.FileDescriptor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Container for a message (data and object references) that can
 * be sent through an IBinder. A Parcel can contain both flattened data
 * that will be unflattened on the other side of the IPC (using the various
 * methods here for writing specific types, or the general {@link Parcelable} interface), and
 * references to live {@link IBinder} objects that will result in the other side receiving a proxy
 * IBinder
 * connected with the original IBinder in the Parcel.
 * <p class="note">
 * Parcel is <strong>not</strong> a general-purpose serialization mechanism. This class (and the
 * corresponding {@link Parcelable} API for placing arbitrary objects into a Parcel) is designed as
 * a high-performance IPC transport. As such, it is not appropriate to place any Parcel data in to
 * persistent storage: changes in the underlying implementation of any of the data in the Parcel can
 * render older data unreadable.
 * </p>
 * <p>
 * The bulk of the Parcel API revolves around reading and writing data of various types. There are
 * six major classes of such functions available.
 * </p>
 * <h3>Primitives</h3>
 * <p>
 * The most basic data functions are for writing and reading primitive data types:
 * {@link #writeByte}, {@link #readByte}, {@link #writeDouble}, {@link #readDouble},
 * {@link #writeFloat}, {@link #readFloat}, {@link #writeInt}, {@link #readInt}, {@link #writeLong},
 * {@link #readLong}, {@link #writeString}, {@link #readString}. Most other data operations are
 * built on top of these. The given data is written and read using the endianess of the host CPU.
 * </p>
 * <h3>Primitive Arrays</h3>
 * <p>
 * There are a variety of methods for reading and writing raw arrays of primitive objects, which
 * generally result in writing a 4-byte length followed by the primitive data items. The methods for
 * reading can either read the data into an existing array, or create and return a new array. These
 * available types are:
 * </p>
 * <ul>
 * <li> {@link #writeBooleanArray(boolean[])}, {@link #readBooleanArray(boolean[])},
 * {@link #createBooleanArray()}
 * <li> {@link #writeByteArray(byte[])}, {@link #writeByteArray(byte[], int, int)},
 * {@link #readByteArray(byte[])}, {@link #createByteArray()}
 * <li> {@link #writeCharArray(char[])}, {@link #readCharArray(char[])}, {@link #createCharArray()}
 * <li> {@link #writeDoubleArray(double[])}, {@link #readDoubleArray(double[])},
 * {@link #createDoubleArray()}
 * <li> {@link #writeFloatArray(float[])}, {@link #readFloatArray(float[])},
 * {@link #createFloatArray()}
 * <li> {@link #writeIntArray(int[])}, {@link #readIntArray(int[])}, {@link #createIntArray()}
 * <li> {@link #writeLongArray(long[])}, {@link #readLongArray(long[])}, {@link #createLongArray()}
 * <li> {@link #writeStringArray(String[])}, {@link #readStringArray(String[])},
 * {@link #createStringArray()}.
 * <li> {@link #writeSparseBooleanArray(SparseBooleanArray)}, {@link #readSparseBooleanArray()}.
 * </ul>
 * <h3>Parcelables</h3>
 * <p>
 * The {@link Parcelable} protocol provides an extremely efficient (but low-level) protocol for
 * objects to write and read themselves from Parcels. You can use the direct methods
 * {@link #writeParcelable(Parcelable, int)} and {@link #readParcelable(ClassLoader)} or
 * {@link #writeParcelableArray} and {@link #readParcelableArray(ClassLoader)} to write or read.
 * These methods write both the class type and its data to the Parcel, allowing that class to be
 * reconstructed from the appropriate class loader when later reading.
 * </p>
 * <p>
 * There are also some methods that provide a more efficient way to work with Parcelables:
 * {@link #writeTypedArray}, {@link #writeTypedList(List)}, {@link #readTypedArray} and
 * {@link #readTypedList}. These methods do not write the class information of the original object:
 * instead, the caller of the read function must know what type to expect and pass in the
 * appropriate {@link Parcelable.Creator Parcelable.Creator} instead to properly construct the new
 * object and read its data. (To more efficient write and read a single Parceable object, you can
 * directly call {@link Parcelable#writeToParcel Parcelable.writeToParcel} and
 * {@link Parcelable.Creator#createFromParcel Parcelable.Creator.createFromParcel} yourself.)
 * </p>
 * <h3>Bundles</h3>
 * <p>
 * A special type-safe container, called {@link Bundle}, is available for key/value maps of
 * heterogeneous values. This has many optimizations for improved performance when reading and
 * writing data, and its type-safe API avoids difficult to debug type errors when finally
 * marshalling the data contents into a Parcel. The methods to use are {@link #writeBundle(Bundle)},
 * {@link #readBundle()}, and {@link #readBundle(ClassLoader)}.
 * <h3>Active Objects</h3>
 * <p>
 * An unusual feature of Parcel is the ability to read and write active objects. For these objects
 * the actual contents of the object is not written, rather a special token referencing the object
 * is written. When reading the object back from the Parcel, you do not get a new instance of the
 * object, but rather a handle that operates on the exact same object that was originally written.
 * There are two forms of active objects available.
 * </p>
 * <p>
 * {@link Binder} objects are a core facility of Android's general cross-process communication
 * system. The {@link IBinder} interface describes an abstract protocol with a Binder object. Any
 * such interface can be written in to a Parcel, and upon reading you will receive either the
 * original object implementing that interface or a special proxy implementation that communicates
 * calls back to the original object. The methods to use are {@link #writeStrongBinder(IBinder)},
 * {@link #writeStrongInterface(IInterface)}, {@link #readStrongBinder()},
 * {@link #writeBinderArray(IBinder[])}, {@link #readBinderArray(IBinder[])},
 * {@link #createBinderArray()}, {@link #writeBinderList(List)}, {@link #readBinderList(List)},
 * {@link #createBinderArrayList()}.
 * </p>
 * <p>
 * FileDescriptor objects, representing raw Linux file descriptor identifiers, can be written and
 * {@link ParcelFileDescriptor} objects returned to operate on the original file descriptor. The
 * returned file descriptor is a dup of the original file descriptor: the object and fd is
 * different, but operating on the same underlying file stream, with the same position, etc. The
 * methods to use are {@link #writeFileDescriptor(FileDescriptor)}, {@link #readFileDescriptor()}.
 * <h3>Untyped Containers</h3>
 * <p>
 * A final class of methods are for writing and reading standard Java containers of arbitrary types.
 * These all revolve around the {@link #writeValue(Object)} and {@link #readValue(ClassLoader)}
 * methods which define the types of objects allowed. The container methods are
 * {@link #writeArray(Object[])}, {@link #readArray(ClassLoader)}, {@link #writeList(List)},
 * {@link #readList(List, ClassLoader)}, {@link #readArrayList(ClassLoader)}, {@link #writeMap(Map)}, {@link #readMap(Map, ClassLoader)}, {@link #writeSparseArray(SparseArray)},
 * {@link #readSparseArray(ClassLoader)}.
 */
public abstract class Parcel {
  public final static Parcelable.Creator<String> STRING_CREATOR = new Parcelable.Creator<String>() {
    @Override
    public String createFromParcel(Parcel source) {
      return source.readString();
    }

    @Override
    public String[] newArray(int size) {
      return new String[size];
    }
  };

  /**
   * Put a Parcel object back into the pool. You must not touch
   * the object after this call.
   */
  public abstract void recycle();

  /**
   * Returns the total amount of data contained in the parcel.
   */
  public abstract int dataSize();

  /**
   * Returns the amount of data remaining to be read from the
   * parcel. That is, {@link #dataSize}-{@link #dataPosition}.
   */
  public abstract int dataAvail();

  /**
   * Returns the current position in the parcel data. Never
   * more than {@link #dataSize}.
   */
  public abstract int dataPosition();

  /**
   * Returns the total amount of space in the parcel. This is always
   * >= {@link #dataSize}. The difference between it and dataSize() is the
   * amount of room left until the parcel needs to re-allocate its
   * data buffer.
   */
  public abstract int dataCapacity();

  /**
   * Change the amount of data in the parcel. Can be either smaller or
   * larger than the current size. If larger than the current capacity,
   * more memory will be allocated.
   * @param size The new number of bytes in the Parcel.
   */
  public abstract void setDataSize(int size);

  /**
   * Move the current read/write position in the parcel.
   * @param pos New offset in the parcel; must be between 0 and {@link #dataSize}.
   */
  public abstract void setDataPosition(int pos);

  /**
   * Change the capacity (current available space) of the parcel.
   * @param size The new capacity of the parcel, in bytes. Can not be
   *          less than {@link #dataSize} -- that is, you can not drop existing data
   *          with this method.
   */
  public abstract void setDataCapacity(int size);

  /** @hide */
  public abstract boolean pushAllowFds(boolean allowFds);

  /** @hide */
  public abstract void restoreAllowFds(boolean lastValue);

  /**
   * Returns the raw bytes of the parcel.
   * <p class="note">
   * The data you retrieve here <strong>must not</strong> be placed in any kind of persistent
   * storage (on local disk, across a network, etc). For that, you should use standard serialization
   * or another kind of general serialization mechanism. The Parcel marshalled representation is
   * highly optimized for local IPC, and as such does not attempt to maintain compatibility with
   * data created in different versions of the platform.
   */
  public abstract byte[] marshall();

  /**
   * Set the bytes in data to be the raw bytes of this Parcel.
   */
  public abstract void unmarshall(byte[] data, int offset, int length);

  public abstract void appendFrom(Parcel parcel, int offset, int length);

  /**
   * Report whether the parcel contains any marshalled file descriptors.
   */
  public abstract boolean hasFileDescriptors();

  /**
   * Store or read an IBinder interface token in the parcel at the current {@link #dataPosition}.
   * This is used to validate that the marshalled
   * transaction is intended for the target interface.
   */
  public abstract void writeInterfaceToken(String interfaceName);

  public abstract void enforceInterface(String interfaceName);

  /**
   * Write a byte array into the parcel at the current {@link #dataPosition},
   * growing {@link #dataCapacity} if needed.
   * @param b Bytes to place into the parcel.
   */
  public abstract void writeByteArray(byte[] b);

  /**
   * Write a byte array into the parcel at the current {@link #dataPosition},
   * growing {@link #dataCapacity} if needed.
   * @param b Bytes to place into the parcel.
   * @param offset Index of first byte to be written.
   * @param len Number of bytes to write.
   */
  public abstract void writeByteArray(byte[] b, int offset, int len);

  /**
   * Write a blob of data into the parcel at the current {@link #dataPosition},
   * growing {@link #dataCapacity} if needed.
   * @param b Bytes to place into the parcel. {@hide} {@SystemApi}
   */
  public abstract void writeBlob(byte[] b);

  /**
   * Write an integer value into the parcel at the current dataPosition(),
   * growing dataCapacity() if needed.
   */
  public abstract void writeInt(int val);

  /**
   * Write a long integer value into the parcel at the current dataPosition(),
   * growing dataCapacity() if needed.
   */
  public abstract void writeLong(long val);

  /**
   * Write a floating point value into the parcel at the current
   * dataPosition(), growing dataCapacity() if needed.
   */
  public abstract void writeFloat(float val);

  /**
   * Write a double precision floating point value into the parcel at the
   * current dataPosition(), growing dataCapacity() if needed.
   */
  public abstract void writeDouble(double val);

  /**
   * Write a string value into the parcel at the current dataPosition(),
   * growing dataCapacity() if needed.
   */
  public abstract void writeString(String val);

  /**
   * Write a CharSequence value into the parcel at the current dataPosition(),
   * growing dataCapacity() if needed.
   * @hide
   */
  public abstract void writeCharSequence(CharSequence val);

  /**
   * Write an object into the parcel at the current dataPosition(),
   * growing dataCapacity() if needed.
   */
  public abstract void writeStrongBinder(IBinder val);

  /**
   * Write an object into the parcel at the current dataPosition(),
   * growing dataCapacity() if needed.
   */
  public abstract void writeStrongInterface(IInterface val);

  /**
   * Write a FileDescriptor into the parcel at the current dataPosition(),
   * growing dataCapacity() if needed.
   * <p class="caution">
   * The file descriptor will not be closed, which may result in file descriptor leaks when objects
   * are returned from Binder calls. Use {@link ParcelFileDescriptor#writeToParcel} instead, which
   * accepts contextual flags and will close the original file descriptor if
   * {@link Parcelable#PARCELABLE_WRITE_RETURN_VALUE} is set.
   * </p>
   */
  public abstract void writeFileDescriptor(FileDescriptor val);

  /**
   * Write a byte value into the parcel at the current dataPosition(),
   * growing dataCapacity() if needed.
   */
  public abstract void writeByte(byte val);

  /**
   * Please use {@link #writeBundle} instead. Flattens a Map into the parcel
   * at the current dataPosition(),
   * growing dataCapacity() if needed. The Map keys must be String objects.
   * The Map values are written using {@link #writeValue} and must follow
   * the specification there.
   * <p>
   * It is strongly recommended to use {@link #writeBundle} instead of this method, since the Bundle
   * class provides a type-safe API that allows you to avoid mysterious type errors at the point of
   * marshalling.
   */
  public abstract void writeMap(Map val);

  /**
   * @hide For testing only.
   */
  public abstract void writeArrayMap(Object val);

  /**
   * Flatten a Bundle into the parcel at the current dataPosition(),
   * growing dataCapacity() if needed.
   */
  public abstract void writeBundle(Bundle val);

  /**
   * Flatten a PersistableBundle into the parcel at the current dataPosition(),
   * growing dataCapacity() if needed.
   */
  public abstract void writePersistableBundle(PersistableBundle val);

  /**
   * Flatten a Size into the parcel at the current dataPosition(),
   * growing dataCapacity() if needed.
   */
  public abstract void writeSize(Size val);

  /**
   * Flatten a SizeF into the parcel at the current dataPosition(),
   * growing dataCapacity() if needed.
   */
  public abstract void writeSizeF(SizeF val);

  /**
   * Flatten a List into the parcel at the current dataPosition(), growing
   * dataCapacity() if needed. The List values are written using {@link #writeValue} and must follow
   * the specification there.
   */
  public abstract void writeList(List val);

  /**
   * Flatten an Object array into the parcel at the current dataPosition(),
   * growing dataCapacity() if needed. The array values are written using {@link #writeValue} and
   * must follow the specification there.
   */
  public abstract void writeArray(Object[] val);

  /**
   * Flatten a generic SparseArray into the parcel at the current
   * dataPosition(), growing dataCapacity() if needed. The SparseArray
   * values are written using {@link #writeValue} and must follow the
   * specification there.
   */
  public abstract void writeSparseArray(SparseArray<Object> val);

  public abstract void writeSparseBooleanArray(SparseBooleanArray val);

  public abstract void writeBooleanArray(boolean[] val);

  public abstract boolean[] createBooleanArray();

  public abstract void readBooleanArray(boolean[] val);

  public abstract void writeCharArray(char[] val);

  public abstract char[] createCharArray();

  public abstract void readCharArray(char[] val);

  public abstract void writeIntArray(int[] val);

  public abstract int[] createIntArray();

  public abstract void readIntArray(int[] val);

  public abstract void writeLongArray(long[] val);

  public abstract long[] createLongArray();

  public abstract void readLongArray(long[] val);

  public abstract void writeFloatArray(float[] val);

  public abstract float[] createFloatArray();

  public abstract void readFloatArray(float[] val);

  public abstract void writeDoubleArray(double[] val);

  public abstract double[] createDoubleArray();

  public abstract void readDoubleArray(double[] val);

  public abstract void writeStringArray(String[] val);

  public abstract String[] createStringArray();

  public abstract void readStringArray(String[] val);

  public abstract void writeBinderArray(IBinder[] val);

  /**
   * @hide
   */
  public abstract void writeCharSequenceArray(CharSequence[] val);

  public abstract IBinder[] createBinderArray();

  public abstract void readBinderArray(IBinder[] val);

  /**
   * Flatten a List containing a particular object type into the parcel, at
   * the current dataPosition() and growing dataCapacity() if needed. The
   * type of the objects in the list must be one that implements Parcelable.
   * Unlike the generic writeList() method, however, only the raw data of the
   * objects is written and not their type, so you must use the corresponding
   * readTypedList() to unmarshall them.
   * @param val The list of objects to be written.
   * @see #createTypedArrayList
   * @see #readTypedList
   * @see Parcelable
   */
  public abstract <T extends Parcelable> void writeTypedList(List<T> val);

  /**
   * Flatten a List containing String objects into the parcel, at
   * the current dataPosition() and growing dataCapacity() if needed. They
   * can later be retrieved with {@link #createStringArrayList} or {@link #readStringList}.
   * @param val The list of strings to be written.
   * @see #createStringArrayList
   * @see #readStringList
   */
  public abstract void writeStringList(List<String> val);

  /**
   * Flatten a List containing IBinder objects into the parcel, at
   * the current dataPosition() and growing dataCapacity() if needed. They
   * can later be retrieved with {@link #createBinderArrayList} or {@link #readBinderList}.
   * @param val The list of strings to be written.
   * @see #createBinderArrayList
   * @see #readBinderList
   */
  public abstract void writeBinderList(List<IBinder> val);

  /**
   * Flatten a heterogeneous array containing a particular object type into
   * the parcel, at
   * the current dataPosition() and growing dataCapacity() if needed. The
   * type of the objects in the array must be one that implements Parcelable.
   * Unlike the {@link #writeParcelableArray} method, however, only the
   * raw data of the objects is written and not their type, so you must use {@link #readTypedArray}
   * with the correct corresponding {@link Parcelable.Creator} implementation to unmarshall them.
   * @param val The array of objects to be written.
   * @param parcelableFlags Contextual flags as per {@link Parcelable#writeToParcel(Parcel, int)
   *          Parcelable.writeToParcel()}.
   * @see #readTypedArray
   * @see #writeParcelableArray
   * @see Parcelable.Creator
   */
  public abstract <T extends Parcelable> void writeTypedArray(T[] val,
      int parcelableFlags);

  /**
   * Flatten a generic object in to a parcel. The given Object value may
   * currently be one of the following types:
   * <ul>
   * <li>null
   * <li>String
   * <li>Byte
   * <li>Short
   * <li>Integer
   * <li>Long
   * <li>Float
   * <li>Double
   * <li>Boolean
   * <li>String[]
   * <li>boolean[]
   * <li>byte[]
   * <li>int[]
   * <li>long[]
   * <li>Object[] (supporting objects of the same type defined here).
   * <li> {@link Bundle}
   * <li>Map (as supported by {@link #writeMap}).
   * <li>Any object that implements the {@link Parcelable} protocol.
   * <li>Parcelable[]
   * <li>CharSequence (as supported by {@link TextUtils#writeToParcel}).
   * <li>List (as supported by {@link #writeList}).
   * <li> {@link SparseArray} (as supported by {@link #writeSparseArray(SparseArray)}).
   * <li> {@link IBinder}
   * <li>Any object that implements Serializable (but see {@link #writeSerializable} for caveats).
   * Note that all of the previous types have relatively efficient implementations for writing to a
   * Parcel; having to rely on the generic serialization approach is much less efficient and should
   * be avoided whenever possible.
   * </ul>
   * <p class="caution">
   * {@link Parcelable} objects are written with {@link Parcelable#writeToParcel} using contextual
   * flags of 0. When serializing objects containing {@link ParcelFileDescriptor}s, this may result
   * in file descriptor leaks when they are returned from Binder calls (where
   * {@link Parcelable#PARCELABLE_WRITE_RETURN_VALUE} should be used).
   * </p>
   */
  public abstract void writeValue(Object v);

  /**
   * Flatten the name of the class of the Parcelable and its contents
   * into the parcel.
   * @param p The Parcelable object to be written.
   * @param parcelableFlags Contextual flags as per {@link Parcelable#writeToParcel(Parcel, int)
   *          Parcelable.writeToParcel()}.
   */
  public abstract void writeParcelable(Parcelable p, int parcelableFlags);

  /** @hide */
  public abstract void writeParcelableCreator(Parcelable p);

  /**
   * Write a generic serializable object in to a Parcel. It is strongly
   * recommended that this method be avoided, since the serialization
   * overhead is extremely large, and this approach will be much slower than
   * using the other approaches to writing data in to a Parcel.
   */
  public abstract void writeSerializable(Serializable s);

  /**
   * Special function for writing an exception result at the header of
   * a parcel, to be used when returning an exception from a transaction.
   * Note that this currently only supports a few exception types; any other
   * exception will be re-thrown by this function as a RuntimeException
   * (to be caught by the system's last-resort exception handling when
   * dispatching a transaction).
   * <p>
   * The supported exception types are:
   * <ul>
   * <li>{@link BadParcelableException}
   * <li>{@link IllegalArgumentException}
   * <li>{@link IllegalStateException}
   * <li>{@link NullPointerException}
   * <li>{@link SecurityException}
   * <li>{@link NetworkOnMainThreadException}
   * </ul>
   * @param e The Exception to be written.
   * @see #writeNoException
   * @see #readException
   */
  public abstract void writeException(Exception e);

  /**
   * Special function for writing information at the front of the Parcel
   * indicating that no exception occurred.
   * @see #writeException
   * @see #readException
   */
  public abstract void writeNoException();

  /**
   * Special function for reading an exception result from the header of
   * a parcel, to be used after receiving the result of a transaction. This
   * will throw the exception for you if it had been written to the Parcel,
   * otherwise return and let you read the normal result data from the Parcel.
   * @see #writeException
   * @see #writeNoException
   */
  public abstract void readException();

  /**
   * Parses the header of a Binder call's response Parcel and
   * returns the exception code. Deals with lite or fat headers.
   * In the common successful case, this header is generally zero.
   * In less common cases, it's a small negative number and will be
   * followed by an error string.
   * This exists purely for android.database.DatabaseUtils and
   * insulating it from having to handle fat headers as returned by
   * e.g. StrictMode-induced RPC responses.
   * @hide
   */
  public abstract int readExceptionCode();

  /**
   * Throw an exception with the given message. Not intended for use
   * outside the Parcel class.
   * @param code Used to determine which exception class to throw.
   * @param msg The exception message.
   */
  public abstract void readException(int code, String msg);

  /**
   * Read an integer value from the parcel at the current dataPosition().
   */
  public abstract int readInt();

  /**
   * Read a long integer value from the parcel at the current dataPosition().
   */
  public abstract long readLong();

  /**
   * Read a floating point value from the parcel at the current
   * dataPosition().
   */
  public abstract float readFloat();

  /**
   * Read a double precision floating point value from the parcel at the
   * current dataPosition().
   */
  public abstract double readDouble();

  /**
   * Read a string value from the parcel at the current dataPosition().
   */
  public abstract String readString();

  /**
   * Read a CharSequence value from the parcel at the current dataPosition().
   * @hide
   */
  public abstract CharSequence readCharSequence();

  /**
   * Read an object from the parcel at the current dataPosition().
   */
  public abstract IBinder readStrongBinder();

  /**
   * Read a FileDescriptor from the parcel at the current dataPosition().
   */
  public abstract ParcelFileDescriptor readFileDescriptor();

  /** {@hide} */
  public abstract FileDescriptor readRawFileDescriptor();

  /**
   * Read a byte value from the parcel at the current dataPosition().
   */
  public abstract byte readByte();

  /**
   * Please use {@link #readBundle(ClassLoader)} instead (whose data must have
   * been written with {@link #writeBundle}. Read into an existing Map object
   * from the parcel at the current dataPosition().
   */
  public abstract void readMap(Map outVal, ClassLoader loader);

  /**
   * Read into an existing List object from the parcel at the current
   * dataPosition(), using the given class loader to load any enclosed
   * Parcelables. If it is null, the default class loader is used.
   */
  public abstract void readList(List outVal, ClassLoader loader);

  /**
   * Please use {@link #readBundle(ClassLoader)} instead (whose data must have
   * been written with {@link #writeBundle}. Read and return a new HashMap
   * object from the parcel at the current dataPosition(), using the given
   * class loader to load any enclosed Parcelables. Returns null if
   * the previously written map object was null.
   */
  public abstract HashMap readHashMap(ClassLoader loader);

  /**
   * Read and return a new Bundle object from the parcel at the current
   * dataPosition(). Returns null if the previously written Bundle object was
   * null.
   */
  public abstract Bundle readBundle();

  /**
   * Read and return a new Bundle object from the parcel at the current
   * dataPosition(), using the given class loader to initialize the class
   * loader of the Bundle for later retrieval of Parcelable objects.
   * Returns null if the previously written Bundle object was null.
   */
  public abstract Bundle readBundle(ClassLoader loader);

  /**
   * Read and return a new Bundle object from the parcel at the current
   * dataPosition(). Returns null if the previously written Bundle object was
   * null.
   */
  public abstract PersistableBundle readPersistableBundle();

  /**
   * Read and return a new Bundle object from the parcel at the current
   * dataPosition(), using the given class loader to initialize the class
   * loader of the Bundle for later retrieval of Parcelable objects.
   * Returns null if the previously written Bundle object was null.
   */
  public abstract PersistableBundle readPersistableBundle(ClassLoader loader);

  /**
   * Read a Size from the parcel at the current dataPosition().
   */
  public abstract Size readSize();

  /**
   * Read a SizeF from the parcel at the current dataPosition().
   */
  public abstract SizeF readSizeF();

  /**
   * Read and return a byte[] object from the parcel.
   */
  public abstract byte[] createByteArray();

  /**
   * Read a byte[] object from the parcel and copy it into the
   * given byte array.
   */
  public abstract void readByteArray(byte[] val);

  /**
   * Read a blob of data from the parcel and return it as a byte array. {@hide} {@SystemApi
   * 
   * 
   * 
   * 
   * 
   * 
   * 
   * 
   * }
   */
  public abstract byte[] readBlob();

  /**
   * Read and return a String[] object from the parcel. {@hide}
   */
  public abstract String[] readStringArray();

  /**
   * Read and return a CharSequence[] object from the parcel. {@hide}
   */
  public abstract CharSequence[] readCharSequenceArray();

  /**
   * Read and return a new ArrayList object from the parcel at the current
   * dataPosition(). Returns null if the previously written list object was
   * null. The given class loader will be used to load any enclosed
   * Parcelables.
   */
  public abstract ArrayList readArrayList(ClassLoader loader);

  /**
   * Read and return a new Object array from the parcel at the current
   * dataPosition(). Returns null if the previously written array was
   * null. The given class loader will be used to load any enclosed
   * Parcelables.
   */
  public abstract Object[] readArray(ClassLoader loader);

  /**
   * Read and return a new SparseArray object from the parcel at the current
   * dataPosition(). Returns null if the previously written list object was
   * null. The given class loader will be used to load any enclosed
   * Parcelables.
   */
  public abstract SparseArray readSparseArray(ClassLoader loader);

  /**
   * Read and return a new SparseBooleanArray object from the parcel at the current
   * dataPosition(). Returns null if the previously written list object was
   * null.
   */
  public abstract SparseBooleanArray readSparseBooleanArray();

  /**
   * Read and return a new ArrayList containing a particular object type from
   * the parcel that was written with {@link #writeTypedList} at the
   * current dataPosition(). Returns null if the
   * previously written list object was null. The list <em>must</em> have
   * previously been written via {@link #writeTypedList} with the same object
   * type.
   * @return A newly created ArrayList containing objects with the same data
   *         as those that were previously written.
   * @see #writeTypedList
   */
  public abstract <T> ArrayList<T> createTypedArrayList(Parcelable.Creator<T> c);

  /**
   * Read into the given List items containing a particular object type
   * that were written with {@link #writeTypedList} at the
   * current dataPosition(). The list <em>must</em> have
   * previously been written via {@link #writeTypedList} with the same object
   * type.
   * @return A newly created ArrayList containing objects with the same data
   *         as those that were previously written.
   * @see #writeTypedList
   */
  public abstract <T> void readTypedList(List<T> list, Parcelable.Creator<T> c);

  /**
   * Read and return a new ArrayList containing String objects from
   * the parcel that was written with {@link #writeStringList} at the
   * current dataPosition(). Returns null if the
   * previously written list object was null.
   * @return A newly created ArrayList containing strings with the same data
   *         as those that were previously written.
   * @see #writeStringList
   */
  public abstract ArrayList<String> createStringArrayList();

  /**
   * Read and return a new ArrayList containing IBinder objects from
   * the parcel that was written with {@link #writeBinderList} at the
   * current dataPosition(). Returns null if the
   * previously written list object was null.
   * @return A newly created ArrayList containing strings with the same data
   *         as those that were previously written.
   * @see #writeBinderList
   */
  public abstract ArrayList<IBinder> createBinderArrayList();

  /**
   * Read into the given List items String objects that were written with {@link #writeStringList}
   * at the current dataPosition().
   * @return A newly created ArrayList containing strings with the same data
   *         as those that were previously written.
   * @see #writeStringList
   */
  public abstract void readStringList(List<String> list);

  /**
   * Read into the given List items IBinder objects that were written with {@link #writeBinderList}
   * at the current dataPosition().
   * @return A newly created ArrayList containing strings with the same data
   *         as those that were previously written.
   * @see #writeBinderList
   */
  public abstract void readBinderList(List<IBinder> list);

  /**
   * Read and return a new array containing a particular object type from
   * the parcel at the current dataPosition(). Returns null if the
   * previously written array was null. The array <em>must</em> have
   * previously been written via {@link #writeTypedArray} with the same
   * object type.
   * @return A newly created array containing objects with the same data
   *         as those that were previously written.
   * @see #writeTypedArray
   */
  public abstract <T> T[] createTypedArray(Parcelable.Creator<T> c);

  public abstract <T> void readTypedArray(T[] val, Parcelable.Creator<T> c);

  /**
   * @deprecated
   * @hide
   */
  @Deprecated
  public abstract <T> T[] readTypedArray(Parcelable.Creator<T> c);

  /**
   * Write a heterogeneous array of Parcelable objects into the Parcel.
   * Each object in the array is written along with its class name, so
   * that the correct class can later be instantiated. As a result, this
   * has significantly more overhead than {@link #writeTypedArray}, but will
   * correctly handle an array containing more than one type of object.
   * @param value The array of objects to be written.
   * @param parcelableFlags Contextual flags as per {@link Parcelable#writeToParcel(Parcel, int)
   *          Parcelable.writeToParcel()}.
   * @see #writeTypedArray
   */
  public abstract <T extends Parcelable> void writeParcelableArray(T[] value,
      int parcelableFlags);

  /**
   * Read a typed object from a parcel. The given class loader will be
   * used to load any enclosed Parcelables. If it is null, the default class
   * loader will be used.
   */
  public abstract Object readValue(ClassLoader loader);

  /**
   * Read and return a new Parcelable from the parcel. The given class loader
   * will be used to load any enclosed Parcelables. If it is null, the default
   * class loader will be used.
   * @param loader A ClassLoader from which to instantiate the Parcelable
   *          object, or null for the default class loader.
   * @return Returns the newly created Parcelable, or null if a null
   *         object has been written.
   * @throws BadParcelableException Throws BadParcelableException if there
   *           was an error trying to instantiate the Parcelable.
   */
  public abstract <T extends Parcelable> T readParcelable(ClassLoader loader);

  /** @hide */
  public abstract <T extends Parcelable> T readCreator(Parcelable.Creator<T> creator,
      ClassLoader loader);

  /** @hide */
  public abstract <T extends Parcelable> Parcelable.Creator<T> readParcelableCreator(
      ClassLoader loader);

  /**
   * Read and return a new Parcelable array from the parcel.
   * The given class loader will be used to load any enclosed
   * Parcelables.
   * @return the Parcelable array, or null if the array is null
   */
  public abstract Parcelable[] readParcelableArray(ClassLoader loader);

  /**
   * Read and return a new Serializable object from the parcel.
   * @return the Serializable object, or null if the Serializable name
   *         wasn't found in the parcel.
   */
  public abstract Serializable readSerializable();

  /**
   * @hide For testing only.
   */
  public abstract void readArrayMap(ArrayMap outVal, ClassLoader loader);

  /**
   * Retrieve a new Parcel object from the pool.
   */
  public static Parcel obtain() {
    return null;
  }

  /** @hide */
  public static native long getGlobalAllocSize();

  /** @hide */
  public static native long getGlobalAllocCount();
}
