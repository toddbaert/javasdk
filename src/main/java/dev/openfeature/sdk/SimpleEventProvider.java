package dev.openfeature.sdk;

/**
 * Abstract EventProvider encapsulating an EventEmitter.
 * 
 * @see EventProvider
 */
public abstract class SimpleEventProvider implements EventProvider {
    protected final EventEmitter eventEmitter = new EventEmitter();
    
    @Override
    public EventEmitter getEventEmitter() {
        return this.eventEmitter;
    }
}
