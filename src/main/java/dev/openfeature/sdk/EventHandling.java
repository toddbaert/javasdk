package dev.openfeature.sdk;

import java.util.function.Consumer;

/**
 * Interface for attaching event handlers.
 */
public interface EventHandling<T> {
    
    T onProviderReady(Consumer<EventDetails> handler);
    
    T onProviderConfigurationChanged(Consumer<EventDetails> handler);
    
    T onProviderError(Consumer<EventDetails> handler);
    
    T onProviderStale(Consumer<EventDetails> handler);
}
