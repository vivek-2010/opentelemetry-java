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

package io.opentelemetry.sdk.correlationcontext;

import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.BinaryFormat;
import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.correlationcontext.CorrelationContext;
import io.opentelemetry.correlationcontext.CorrelationContextManager;
import io.opentelemetry.correlationcontext.DefaultCorrelationContextManager;
import io.opentelemetry.correlationcontext.unsafe.ContextUtils;

/**
 * {@link CorrelationContextManagerSdk} is SDK implementation of {@link CorrelationContextManager}.
 */
public class CorrelationContextManagerSdk implements CorrelationContextManager {

  @Override
  public CorrelationContext getCurrentContext() {
    return ContextUtils.getValue();
  }

  @Override
  public CorrelationContext.Builder contextBuilder() {
    return new CorrelationContextSdk.Builder();
  }

  @Override
  public Scope withContext(CorrelationContext distContext) {
    return ContextUtils.withCorrelationContext(distContext);
  }

  @Override
  public BinaryFormat<CorrelationContext> getBinaryFormat() {
    // TODO: Implement this.
    return DefaultCorrelationContextManager.getInstance().getBinaryFormat();
  }

  @Override
  public HttpTextFormat<CorrelationContext> getHttpTextFormat() {
    // TODO: Implement this.
    return DefaultCorrelationContextManager.getInstance().getHttpTextFormat();
  }

  @Override
  public B3HeaderFormat<CorrelationContext> getB3Format() {
    // TODO: Implement this.
    return DefaultCorrelationContextManager.getInstance().getB3Format();
  }
}
