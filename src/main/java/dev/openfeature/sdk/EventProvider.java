package dev.openfeature.sdk;

/**
 * Interface for attaching event handlers.
 * 
 * @see SimpleEventProvider for a basic implementation.
 */
public interface EventProvider {

    /**
     * Return the EventEmitter interface for this provider.
     * The same instance should be returned from this method at each invocation.
     * 
     * @return the EventEmitter instance for this EventProvider. 
     */
    EventEmitter getEventEmitter();

    static boolean isEventProvider(FeatureProvider provider) {
        return EventProvider.class.isInstance(provider);
    }
}
