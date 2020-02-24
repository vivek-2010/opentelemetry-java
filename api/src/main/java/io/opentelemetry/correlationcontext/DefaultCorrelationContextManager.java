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

package io.opentelemetry.correlationcontext;

import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.B3HeaderFormat;
import io.opentelemetry.context.propagation.BinaryFormat;
import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.correlationcontext.unsafe.ContextUtils;
import io.opentelemetry.internal.Utils;
import java.util.Collections;
import java.util.List;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * No-op implementations of {@link CorrelationContextManager}.
 *
 * @since 0.1.0
 */
@ThreadSafe
public final class DefaultCorrelationContextManager implements CorrelationContextManager {
  private static final DefaultCorrelationContextManager INSTANCE =
      new DefaultCorrelationContextManager();
  private static final BinaryFormat<CorrelationContext> BINARY_FORMAT = new NoopBinaryFormat();
  private static final HttpTextFormat<CorrelationContext> HTTP_TEXT_FORMAT =
      new NoopHttpTextFormat();
  private static final B3HeaderFormat<CorrelationContext> B3_FORMAT = new NoopB3HeaderFormat();

  /**
   * Returns a {@code CorrelationContextManager} singleton that is the default implementation for
   * {@link CorrelationContextManager}.
   *
   * @return a {@code CorrelationContextManager} singleton that is the default implementation for
   *     {@link CorrelationContextManager}.
   * @since 0.1.0
   */
  public static CorrelationContextManager getInstance() {
    return INSTANCE;
  }

  @Override
  public CorrelationContext getCurrentContext() {
    return ContextUtils.getValue();
  }

  @Override
  public CorrelationContext.Builder contextBuilder() {
    return new NoopCorrelationContextBuilder();
  }

  @Override
  public Scope withContext(CorrelationContext distContext) {
    return ContextUtils.withCorrelationContext(distContext);
  }

  @Override
  public BinaryFormat<CorrelationContext> getBinaryFormat() {
    return BINARY_FORMAT;
  }

  @Override
  public HttpTextFormat<CorrelationContext> getHttpTextFormat() {
    return HTTP_TEXT_FORMAT;
  }

  @Override
  public B3HeaderFormat<CorrelationContext> getB3Format() { return B3_FORMAT; }

  @Immutable
  private static final class NoopCorrelationContextBuilder implements CorrelationContext.Builder {
    @Override
    public CorrelationContext.Builder setParent(CorrelationContext parent) {
      Utils.checkNotNull(parent, "parent");
      return this;
    }

    @Override
    public CorrelationContext.Builder setNoParent() {
      return this;
    }

    @Override
    public CorrelationContext.Builder put(
        EntryKey key, EntryValue value, EntryMetadata tagMetadata) {
      Utils.checkNotNull(key, "key");
      Utils.checkNotNull(value, "value");
      Utils.checkNotNull(tagMetadata, "tagMetadata");
      return this;
    }

    @Override
    public CorrelationContext.Builder remove(EntryKey key) {
      Utils.checkNotNull(key, "key");
      return this;
    }

    @Override
    public CorrelationContext build() {
      return EmptyCorrelationContext.getInstance();
    }
  }

  @Immutable
  private static final class NoopBinaryFormat implements BinaryFormat<CorrelationContext> {
    static final byte[] EMPTY_BYTE_ARRAY = {};

    @Override
    public byte[] toByteArray(CorrelationContext distContext) {
      Utils.checkNotNull(distContext, "distContext");
      return EMPTY_BYTE_ARRAY;
    }

    @Override
    public CorrelationContext fromByteArray(byte[] bytes) {
      Utils.checkNotNull(bytes, "bytes");
      return EmptyCorrelationContext.getInstance();
    }
  }

  @Immutable
  private static final class NoopHttpTextFormat implements HttpTextFormat<CorrelationContext> {
    @Override
    public List<String> fields() {
      return Collections.emptyList();
    }

    @Override
    public <C> void inject(CorrelationContext distContext, C carrier, Setter<C> setter) {
      Utils.checkNotNull(distContext, "distContext");
      Utils.checkNotNull(carrier, "carrier");
      Utils.checkNotNull(setter, "setter");
    }

    @Override
    public <C> CorrelationContext extract(C carrier, Getter<C> getter) {
      Utils.checkNotNull(carrier, "carrier");
      Utils.checkNotNull(getter, "getter");
      return EmptyCorrelationContext.getInstance();
    }
  }

  @Immutable
  private static final class NoopB3HeaderFormat implements B3HeaderFormat<CorrelationContext> {
    @Override
    public List<String> fields() {
      return Collections.emptyList();
    }

    @Override
    public <C> void inject(CorrelationContext distContext, C carrier, Setter<C> setter) {
      Utils.checkNotNull(distContext, "distContext");
      Utils.checkNotNull(carrier, "carrier");
      Utils.checkNotNull(setter, "setter");
    }

    @Override
    public <C> CorrelationContext extract(C carrier, Getter<C> getter) {
      Utils.checkNotNull(carrier, "carrier");
      Utils.checkNotNull(getter, "getter");
      return EmptyCorrelationContext.getInstance();
    }
  }
}
