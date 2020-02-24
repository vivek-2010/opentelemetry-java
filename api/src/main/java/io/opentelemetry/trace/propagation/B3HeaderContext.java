package io.opentelemetry.trace.propagation;

import static io.opentelemetry.internal.Utils.checkArgument;
import static io.opentelemetry.internal.Utils.checkNotNull;

import io.opentelemetry.context.propagation.B3HeaderFormat;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.TraceFlags;
import io.opentelemetry.trace.TraceId;
import io.opentelemetry.trace.TraceState;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Implementation of the TraceContext propagation protocol. See <a
 * href=https://github.com/w3c/distributed-tracing>w3c/distributed-tracing</a>.
 */
public class B3HeaderContext implements B3HeaderFormat<SpanContext> {

    private final String X_B3_TRACEID = "X-B3-TraceId";
    private final String X_B3_SPANID = "X-B3-SpanId";
    private final String X_B3_PARENTSPANID = "X-B3-ParentSpanId";
    private final String X_B3_SAMPLED = "X-B3-Sampled";
    private final String X_B3_FLAGS = "X-B3-Flags";

    static final String TRACE_PARENT = "traceparent";
    static final String TRACE_STATE = "tracestate";
    private static final List<String> FIELDS =
            Collections.unmodifiableList(Arrays.asList(TRACE_PARENT, TRACE_STATE));

    private static final TraceState TRACE_STATE_DEFAULT = TraceState.builder().build();
    private static final int TRACE_ID_HEX_SIZE = 2 * TraceId.getSize();
    private static final int SPAN_ID_HEX_SIZE = 2 * SpanId.getSize();
    private static final int TRACE_OPTION_HEX_SIZE = 2 * TraceFlags.getSize();
    private static final int VERSION_SIZE = 2;
    private static final int TRACEPARENT_DELIMITER_SIZE = 1;
    private static final int TRACE_ID_OFFSET = VERSION_SIZE + TRACEPARENT_DELIMITER_SIZE;
    private static final int SPAN_ID_OFFSET =
            TRACE_ID_OFFSET + TRACE_ID_HEX_SIZE + TRACEPARENT_DELIMITER_SIZE;
    private static final int TRACE_OPTION_OFFSET =
            SPAN_ID_OFFSET + SPAN_ID_HEX_SIZE + TRACEPARENT_DELIMITER_SIZE;
    private static final int TRACESTATE_MAX_SIZE = 512;
    private static final int TRACESTATE_MAX_MEMBERS = 32;
    private static final char TRACESTATE_KEY_VALUE_DELIMITER = ':';
    private static final char TRACESTATE_ENTRY_DELIMITER = ',';
    private static final Pattern TRACESTATE_ENTRY_DELIMITER_SPLIT_PATTERN =
            Pattern.compile("[ \t]*" + TRACESTATE_ENTRY_DELIMITER + "[ \t]*");
    static final SpanContext INVALID_SPAN_CONTEXT =
            SpanContext.create(
                    TraceId.getInvalid(),
                    SpanId.getInvalid(),
                    TraceFlags.getDefault(),
                    TraceState.getDefault());

    @Override
    public List<String> fields() {
        return FIELDS;
    }

    @Override
    public <C> void inject(SpanContext spanContext, C carrier, B3HeaderFormat.Setter<C> setter) {
        checkNotNull(spanContext, "spanContext");
        checkNotNull(setter, "setter");
        checkNotNull(carrier, "carrier");

        setter.set(carrier, X_B3_TRACEID, spanContext.getTraceId().toString());
        setter.set(carrier, X_B3_SPANID, spanContext.getSpanId().toString());
        setter.set(carrier, X_B3_SAMPLED, spanContext.getTraceFlags().toString());

        List<TraceState.Entry> entries = spanContext.getTraceState().getEntries();
        if (entries.isEmpty()) {
            // No need to add an empty "tracestate" header.
            return;
        }
        setter.set(carrier, X_B3_FLAGS, entries.get(1).getValue());

        StringBuilder stringBuilder = new StringBuilder(TRACESTATE_MAX_SIZE);
        for (TraceState.Entry entry : entries) {
            stringBuilder
                    .append(entry.getKey())
                    .append(TRACESTATE_KEY_VALUE_DELIMITER)
                    .append(entry.getValue());
        }
        setter.set(carrier, TRACE_STATE, stringBuilder.toString());
    }

    @Override
    public <C> SpanContext extract(C carrier, Getter<C> getter) {
        checkNotNull(carrier, "carrier");
        checkNotNull(getter, "getter");

        TraceId traceId;
        SpanId spanId;
        TraceFlags traceFlags;
        String xB3TraceId = getter.get(carrier, X_B3_TRACEID);
        if (xB3TraceId == null){
            return INVALID_SPAN_CONTEXT;
        }
        String xB3SpanId = getter.get(carrier, X_B3_SPANID);
        String xB3Sampled = getter.get(carrier, X_B3_SAMPLED);
        traceId = TraceId.fromLowerBase16(xB3TraceId, 0);
        spanId = SpanId.fromLowerBase16(xB3SpanId, 0);
        traceFlags = TraceFlags.fromLowerBase16(xB3Sampled, 0);

        try {
            traceId.isValid();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid traceId: " + traceId, e);
        }

        try {
            spanId.isValid();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid spanId: " + spanId, e);
        }

        String xB3Flags = getter.get(carrier, X_B3_FLAGS);
        TraceState.Builder traceStateBuilder = TraceState.builder();
        if (xB3Flags != null) {
            if (xB3Flags.isEmpty() || xB3Flags == "0")
                traceStateBuilder.set("debug", "0");
            else
                traceStateBuilder.set("debug", "1");
        }
        String traceState = getter.get(carrier, TRACE_STATE);
        try {
            if (traceState == null || traceState.isEmpty()) {
                return SpanContext.createFromRemoteParent(traceId, spanId, traceFlags, TRACE_STATE_DEFAULT);
            }
            String[] listMembers = TRACESTATE_ENTRY_DELIMITER_SPLIT_PATTERN.split(traceState);
            checkArgument(

                    listMembers.length <= TRACESTATE_MAX_MEMBERS, "TraceState has too many elements.");
            // Iterate in reverse order because when call builder set the elements is added in the
            // front of the list.
            for (int i = listMembers.length - 1; i >= 0; i--) {
                String listMember = listMembers[i];
                int index = listMember.indexOf(TRACESTATE_KEY_VALUE_DELIMITER);
                checkArgument(index != -1, "Invalid TraceState list-member format.");
                traceStateBuilder.set(listMember.substring(0, index), listMember.substring(index + 1));
            }
            return SpanContext.createFromRemoteParent(
                    traceId, spanId, traceFlags, traceStateBuilder.build());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid tracestate: " + traceState, e);
        }
    }
}
