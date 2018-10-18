/*
   Copyright 2013-2018 Immutables Authors and Contributors

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

/**
 * This package adds support for <a href="http://bsonspec.org/">BSON</a> to
 * <a href="https://github.com/google/gson">Gson</a> library.
 *
 * <p>Historically immutables mongo adapter required Gson to properly encode / decode POJOs, since
 * migration to v3 driver this functionality has been delegated to a better suited
 * {@link org.bson.codecs.configuration.CodecRegistry} which can be provided during repository setup.
 */
package org.immutables.mongo.bson4gson;
