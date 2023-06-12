package dev.openfeature.sdk;

import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * Interface for attaching event handlers.
 */
@Data @SuperBuilder(toBuilder = true)
public class EventDetails extends ProviderEventDetails {
    private String clientName;
}
