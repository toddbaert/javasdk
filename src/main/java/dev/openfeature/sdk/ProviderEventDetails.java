package dev.openfeature.sdk;

import java.util.List;

import javax.annotation.Nullable;

import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * Interface for attaching event handlers.
 */
@Data @SuperBuilder(toBuilder = true)
public class ProviderEventDetails {
    @Nullable private List<String> flagsChanged;
    @Nullable private String message;
    @Nullable private FlagMetadata flagMetadata; // TODO: rename this?
}
