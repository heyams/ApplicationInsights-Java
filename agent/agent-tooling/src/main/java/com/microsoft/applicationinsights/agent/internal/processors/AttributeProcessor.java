package com.microsoft.applicationinsights.agent.internal.processors;

import java.util.List;
import java.util.regex.Matcher;

import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorAction;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorConfig;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.apache.commons.codec.digest.DigestUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

// structure which only allows valid data
// normalization has to occur before construction
public class AttributeProcessor extends AgentProcessor {

    private final List<ProcessorAction> actions;

    private AttributeProcessor(
            List<ProcessorAction> actions,
            @Nullable IncludeExclude include,
            @Nullable IncludeExclude exclude) {
        super(include, exclude);
        this.actions = actions;
    }

    // Creates a Span Processor object
    public static AttributeProcessor create(ProcessorConfig config) {
        IncludeExclude normalizedInclude = config.include != null ? getNormalizedIncludeExclude(config.include) : null;
        IncludeExclude normalizedExclude = config.exclude != null ? getNormalizedIncludeExclude(config.exclude) : null;
        return new AttributeProcessor(config.actions, normalizedInclude, normalizedExclude);
    }

    // Function to process actions
    public SpanData processActions(SpanData span) {
        SpanData updatedSpan = span;
        for (ProcessorAction actionObj : actions) {
            updatedSpan = processAction(updatedSpan, actionObj);
        }
        return updatedSpan;
    }

    private SpanData processAction(SpanData span, ProcessorAction actionObj) {
        switch (actionObj.action) {
            case insert:
                return processInsertAction(span, actionObj);
            case update:
                return processUpdateAction(span, actionObj);
            case delete:
                return processDeleteAction(span, actionObj);
            case hash:
                return procesHashAction(span, actionObj);
            case extract:
                return processExtractAction(span, actionObj);
            default:
                return span;
        }
    }

    private SpanData processInsertAction(SpanData span, ProcessorAction actionObj) {
        Attributes existingSpanAttributes = span.getAttributes();
        //Update from existing attribute
        if (actionObj.value != null) {
            //update to new value
            final AttributesBuilder builder = Attributes.builder();
            builder.put(actionObj.key, actionObj.value);
            builder.putAll(existingSpanAttributes);
            return new MySpanData(span, builder.build());
        }
        String fromAttributeValue = getAttribute(existingSpanAttributes, AttributeKey.stringKey(actionObj.fromAttribute));
        if (fromAttributeValue != null) {
            final AttributesBuilder builder = Attributes.builder();
            builder.put(actionObj.key, fromAttributeValue);
            builder.putAll(existingSpanAttributes);
            return new MySpanData(span, builder.build());
        }
        return span;
    }

    private SpanData processUpdateAction(SpanData span, ProcessorAction actionObj) {
        // Currently we only support String
        // TODO don't instantiate new AttributeKey every time
        String existingValue = getAttribute(span.getAttributes(), AttributeKey.stringKey(actionObj.key));
        if (existingValue == null) {
            return span;
        }
        //Update from existing attribute
        if (actionObj.value != null) {
            //update to new value
            AttributesBuilder builder = span.getAttributes().toBuilder();
            builder.put(actionObj.key, actionObj.value);
            return new MySpanData(span, builder.build());
        }
        String fromAttributeValue = getAttribute(span.getAttributes(), AttributeKey.stringKey(actionObj.fromAttribute));
        if (fromAttributeValue != null) {
            AttributesBuilder builder = span.getAttributes().toBuilder();
            builder.put(actionObj.key, fromAttributeValue);
            return new MySpanData(span, builder.build());
        }
        return span;
    }

    private SpanData processDeleteAction(SpanData span, ProcessorAction actionObj) {
        // Currently we only support String
        // TODO don't instantiate new AttributeKey every time
        String existingValue = getAttribute(span.getAttributes(), AttributeKey.stringKey(actionObj.key));
        if (existingValue == null) {
            return span;
        }
        AttributesBuilder builder = Attributes.builder();
        span.getAttributes().forEach((key, value) -> {
            if (!key.getKey().equals(actionObj.key)) {
                putIntoBuilder(builder, key, value);
            }
        });
        return new MySpanData(span, builder.build());
    }

    private SpanData procesHashAction(SpanData span, ProcessorAction actionObj) {
        // Currently we only support String
        // TODO don't instantiate new AttributeKey every time
        String existingValue = getAttribute(span.getAttributes(), AttributeKey.stringKey(actionObj.key));
        AttributesBuilder builderCopy;
        if (existingValue == null) {
            return span;
        }
        builderCopy = span.getAttributes().toBuilder();
        builderCopy.put(actionObj.key, DigestUtils.sha1Hex(existingValue));
        return new MySpanData(span, builderCopy.build());
    }

    private SpanData processExtractAction(SpanData span, ProcessorAction actionObj) {
        // Currently we only support String
        // TODO don't instantiate new AttributeKey every time
        String existingValue = getAttribute(span.getAttributes(), AttributeKey.stringKey(actionObj.key));
        if (existingValue == null) {
            return span;
        }
        Matcher matcher = actionObj.extractAttribute.pattern.matcher(existingValue);
        if (!matcher.matches()) {
            return span;
        }
        AttributesBuilder builder = span.getAttributes().toBuilder();
        for (String groupName : actionObj.extractAttribute.groupNames) {
            builder.put(groupName, matcher.group(groupName));
        }
        return new MySpanData(span, builder.build());
    }

    // this won't be needed once we update to 0.13.0
    // see https://github.com/open-telemetry/opentelemetry-java/pull/2284
    public static String getAttribute(Attributes attributes, AttributeKey<String> key) {
        Object existingValueObj = attributes.get(key);
        // checking the return type won't be needed once we update to 0.13.0
        // see https://github.com/open-telemetry/opentelemetry-java/pull/2284
        if (existingValueObj instanceof String) {
            return (String) existingValueObj;
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private void putIntoBuilder(AttributesBuilder builder, AttributeKey<?> key, Object value) {
        switch (key.getType()) {
            case STRING:
                builder.put((AttributeKey<String>) key, (String) value);
                break;
            case LONG:
                builder.put((AttributeKey<Long>) key, (Long) value);
                break;
            case BOOLEAN:
                builder.put((AttributeKey<Boolean>) key, (Boolean) value);
                break;
            case DOUBLE:
                builder.put((AttributeKey<Double>) key, (Double) value);
                break;
            case STRING_ARRAY:
            case LONG_ARRAY:
            case BOOLEAN_ARRAY:
            case DOUBLE_ARRAY:
                builder.put((AttributeKey<List<?>>) key, (List<?>) value);
                break;
            default:
                // TODO log at least a debug level message
                break;
        }
    }
}
