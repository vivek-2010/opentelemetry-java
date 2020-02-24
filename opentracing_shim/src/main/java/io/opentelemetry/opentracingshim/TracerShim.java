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

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Binary;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtract;
import io.opentracing.propagation.TextMapInject;
import java.util.logging.Level;
import java.util.logging.Logger;

final class TracerShim extends BaseShimObject implements Tracer {
  private static final Logger logger = Logger.getLogger(TracerShim.class.getName());

  private final ScopeManager scopeManagerShim;
  private final Propagation propagation;

  TracerShim(TelemetryInfo telemetryInfo) {
    super(telemetryInfo);
    this.scopeManagerShim = new ScopeManagerShim(telemetryInfo);
    this.propagation = new Propagation(telemetryInfo);
  }

  @Override
  public ScopeManager scopeManager() {
    return scopeManagerShim;
  }

  @Override
  public Span activeSpan() {
    return scopeManagerShim.activeSpan();
  }

  @Override
  public Scope activateSpan(Span span) {
    return scopeManagerShim.activate(span);
  }

  @Override
  public SpanBuilder buildSpan(String operationName) {
    return new SpanBuilderShim(telemetryInfo, operationName);
  }

  @Override
  public <C> void inject(SpanContext context, Format<C> format, C carrier) {
    if (context == null) {
      logger.log(Level.INFO, "Cannot inject a null span context.");
      return;
    }

    SpanContextShim contextShim = getContextShim(context);

    if (format == Format.Builtin.TEXT_MAP
        || format == Format.Builtin.TEXT_MAP_INJECT
        || format == Format.Builtin.HTTP_HEADERS) {
      propagation.injectTextFormat(contextShim, (TextMapInject) carrier);
    } else if (format == Format.Builtin.BINARY) {
      propagation.injectBinaryFormat(contextShim, (Binary) carrier);
    } else {
      propagation.injectB3Format(contextShim, , carrier);
    }
  }

  @SuppressWarnings("ReturnMissingNullable")
  @Override
  public <C> SpanContext extract(Format<C> format, C carrier) {
    try {
      if (format == Format.Builtin.TEXT_MAP
          || format == Format.Builtin.TEXT_MAP_EXTRACT
          || format == Format.Builtin.HTTP_HEADERS) {
        return propagation.extractTextFormat((TextMapExtract) carrier);
      } else if (format == Format.Builtin.BINARY) {
        return propagation.extractBinaryFormat((Binary) carrier);
      }
    } catch (Exception e) {
      logger.log(
          Level.INFO,
          "Exception caught while extracting span context; returning null. "
              + "Exception: [{0}] Message: [{1}]",
          new String[] {e.getClass().getName(), e.getMessage()});
    }

    return null;
  }

  @Override
  public void close() {
    // TODO
  }

  static SpanContextShim getContextShim(SpanContext context) {
    if (!(context instanceof SpanContextShim)) {
      throw new IllegalArgumentException("context is not a valid SpanContextShim object");
    }

    return (SpanContextShim) context;
  }
}
