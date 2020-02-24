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

package io.opentelemetry.opentracingshim;

import io.opentelemetry.context.propagation.B3HeaderFormat;
import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentracing.propagation.Binary;
import io.opentracing.propagation.TextMapExtract;
import io.opentracing.propagation.TextMapInject;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

final class Propagation extends BaseShimObject {
  Propagation(TelemetryInfo telemetryInfo) {
    super(telemetryInfo);
  }

  public void injectTextFormat(SpanContextShim contextShim, TextMapInject carrier) {
    tracer()
        .getHttpTextFormat()
        .inject(contextShim.getSpanContext(), carrier, TextMapSetter.INSTANCE);
    contextManager()
        .getHttpTextFormat()
        .inject(contextShim.getCorrelationContext(), carrier, TextMapSetter.INSTANCE);
  }

  @Nullable
  public SpanContextShim extractTextFormat(TextMapExtract carrier) {
    Map<String, String> carrierMap = new HashMap<String, String>();
    for (Map.Entry<String, String> entry : carrier) {
      carrierMap.put(entry.getKey(), entry.getValue());
    }

    io.opentelemetry.trace.SpanContext context =
        tracer().getHttpTextFormat().extract(carrierMap, TextMapGetter.INSTANCE);
    io.opentelemetry.correlationcontext.CorrelationContext distContext =
        contextManager().getHttpTextFormat().extract(carrierMap, TextMapGetter.INSTANCE);
    if (!context.isValid()) {
      return null;
    }
    return new SpanContextShim(telemetryInfo, context, distContext);
  }

  public void injectB3Format(SpanContextShim contextShim, TextMapInject carrier) {
    tracer()
            .getB3Format()
            .inject(contextShim.getSpanContext(), carrier, B3TextMapSetter.INSTANCE);
    contextManager()
            .getB3Format()
            .inject(contextShim.getCorrelationContext(), carrier, B3TextMapSetter.INSTANCE);
  }

  @Nullable
  public SpanContextShim extractB3Format(TextMapExtract carrier) {
    Map<String, String> carrierMap = new HashMap<String, String>();
    for (Map.Entry<String, String> entry : carrier) {
      carrierMap.put(entry.getKey(), entry.getValue());
    }

    io.opentelemetry.trace.SpanContext context =
            tracer().getB3Format().extract(carrierMap, B3TextMapGetter.INSTANCE);
    io.opentelemetry.correlationcontext.CorrelationContext distContext =
            contextManager().getB3Format().extract(carrierMap, B3TextMapGetter.INSTANCE);
    if (!context.isValid()) {
      return null;
    }
    return new SpanContextShim(telemetryInfo, context, distContext);
  }

  static final class TextMapSetter implements HttpTextFormat.Setter<TextMapInject> {
    private TextMapSetter() {}

    public static final TextMapSetter INSTANCE = new TextMapSetter();

    @Override
    public void set(TextMapInject carrier, String key, String value) {
      carrier.put(key, value);
    }
  }

  // We use Map<> instead of TextMap as we need to query a specified key, and iterating over
  // *all* values per key-query *might* be a bad idea.
  static final class TextMapGetter implements HttpTextFormat.Getter<Map<String, String>> {
    private TextMapGetter() {}

    public static final TextMapGetter INSTANCE = new TextMapGetter();

    @Override
    public String get(Map<String, String> carrier, String key) {
      return carrier.get(key);
    }
  }

  static final class B3TextMapSetter implements B3HeaderFormat.Setter<TextMapInject> {
    private B3TextMapSetter() {}

    public static final B3TextMapSetter INSTANCE = new B3TextMapSetter();

    @Override
    public void set(TextMapInject carrier, String key, String value) {
      carrier.put(key, value);
    }
  }

  // We use Map<> instead of TextMap as we need to query a specified key, and iterating over
  // *all* values per key-query *might* be a bad idea.
  static final class B3TextMapGetter implements B3HeaderFormat.Getter<Map<String, String>> {
    private B3TextMapGetter() {}

    public static final B3TextMapGetter INSTANCE = new B3TextMapGetter();

    @Override
    public String get(Map<String, String> carrier, String key) {
      return carrier.get(key);
    }
  }

  public void injectBinaryFormat(SpanContextShim context, Binary carrier) {
    byte[] contextBuff = tracer().getBinaryFormat().toByteArray(context.getSpanContext());
    ByteBuffer byteBuff = carrier.injectionBuffer(contextBuff.length);
    byteBuff.put(contextBuff);
  }

  public SpanContextShim extractBinaryFormat(Binary carrier) {

    ByteBuffer byteBuff = carrier.extractionBuffer();
    byte[] buff = new byte[byteBuff.remaining()];
    byteBuff.get(buff);

    return new SpanContextShim(telemetryInfo, tracer().getBinaryFormat().fromByteArray(buff));
  }
}
