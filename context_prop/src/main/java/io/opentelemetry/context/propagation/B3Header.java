/*
 * Copyright 2019, OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.context.propagation;

import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Injects and extracts a value as text into carriers that travel in-band across process boundaries.
 * Encoding is expected to conform to the HTTP Header Field semantics. Values are often encoded as
 * RPC/HTTP request headers.
 *
 * <p>The carrier of propagated data on both the client (injector) and server (extractor) side is
 * usually an http request. Propagation is usually implemented via library- specific request
 * interceptors, where the client-side injects values and the server-side extracts them.
 *
 * @since 0.1.0
 */
@ThreadSafe
public interface B3Header<V> {
    /**
     * The propagation fields defined. If your carrier is reused, you should delete the fields here
     * before calling {@link #inject(Object, Object, Setter)} )}.
     *
     * <p>For example, if the carrier is a single-use or immutable request object, you don't need to
     * clear fields as they couldn't have been set before. If it is a mutable, retryable object,
     * successive calls should clear these fields first.
     *
     * @return list of fields that will be used by this formatter.
     * @since 0.1.0
     */
    // The use cases of this are:
    // * allow pre-allocation of fields, especially in systems like gRPC Metadata
    // * allow a single-pass over an iterator
    List<String> fields();

    /**
     * Injects the value downstream. For example, as http headers.
     *
     * @param value the value to be injected.
     * @param carrier holds propagation fields. For example, an outgoing message or http request.
     * @param setter invoked for each propagation key to add or remove.
     * @param <C> carrier of propagation fields, such as an http request
     * @since 0.1.0
     */
    <C> void inject(V value, C carrier, Setter<C> setter);

    /**
     * Class that allows a {@code HttpTextFormat} to set propagated fields into a carrier.
     *
     * <p>{@code Setter} is stateless and allows to be saved as a constant to avoid runtime
     * allocations.
     *
     * @param <C> carrier of propagation fields, such as an http request
     * @since 0.1.0
     */
    interface Setter<C> {

        /**
         * Replaces a propagated field with the given value.
         *
         * <p>For example, a setter for an {@link java.net.HttpURLConnection} would be the method
         * reference {@link java.net.HttpURLConnection#addRequestProperty(String, String)}
         *
         * @param carrier holds propagation fields. For example, an outgoing message or http request.
         * @param key the key of the field.
         * @param value the value of the field.
         * @since 0.1.0
         */
        void set(C carrier, String key, String value);
    }

    /**
     * Extracts the value from upstream. For example, as http headers.
     *
     * <p>If the value could not be parsed, the underlying implementation will decide to return an
     * object representing either an empty value, an invalid value, or a valid value. Implementation
     * must not return {@code null}.
     *
     * @param carrier holds propagation fields. For example, an outgoing message or http request.
     * @param getter invoked for each propagation key to get.
     * @param <C> carrier of propagation fields, such as an http request.
     * @return the extracted value or an invalid span context if getter returned {@code null}, never
     *     {@code null}.
     * @since 0.1.0
     */
    <C> V extract(C carrier, Getter<C> getter);

    /**
     * Interface that allows a {@code HttpTextFormat} to read propagated fields from a carrier.
     *
     * <p>{@code Getter} is stateless and allows to be saved as a constant to avoid runtime
     * allocations.
     *
     * @param <C> carrier of propagation fields, such as an http request.
     * @since 0.1.0
     */
    interface Getter<C> {

        /**
         * Returns the first value of the given propagation {@code key} or returns {@code null}.
         *
         * @param carrier carrier of propagation fields, such as an http request.
         * @param key the key of the field.
         * @return the first value of the given propagation {@code key} or returns {@code null}.
         * @since 0.1.0
         */
        @Nullable
        String get(C carrier, String key);
    }
}
