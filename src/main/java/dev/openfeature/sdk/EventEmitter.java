package dev.openfeature.sdk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import lombok.extern.slf4j.Slf4j;

/**
 * Event emitter construct to be used by providers.
 */
@Slf4j
public class EventEmitter {

    private static final ExecutorService taskExecutor = Executors.newCachedThreadPool();
    private final Map<ProviderEvent, List<Consumer<EventDetails>>> handlerMap =
        new ConcurrentHashMap<ProviderEvent, List<Consumer<EventDetails>>>() {
            {
                put(ProviderEvent.PROVIDER_READY, new ArrayList<>());
                put(ProviderEvent.PROVIDER_CONFIGURATION_CHANGED, new ArrayList<>());
                put(ProviderEvent.PROVIDER_ERROR, new ArrayList<>());
                put(ProviderEvent.PROVIDER_STALE, new ArrayList<>());
            }
        };

    /**
     * Emit an event.
     *
     * @param event   Event type to emit.
     * @param details Event details.
     */
    public void emit(ProviderEvent event, ProviderEventDetails details) {
        this.handlerMap.get(event).stream().forEach(handler -> {
            taskExecutor.submit(() -> {
                try {
                    EventDetails eventDetails = EventDetails.builder()
                            .flagMetadata(details.getFlagMetadata())
                            .flagsChanged(details.getFlagsChanged())
                            .message(details.getMessage())
                            .build();

                    // we may be proxying this event, preserve the name if so.
                    if (EventDetails.class.isInstance(details)) {
                        eventDetails.setClientName(((EventDetails) details).getClientName());
                    }
                    handler.accept(eventDetails);
                } catch (Exception e) {
                    log.error("Exception in event handler {}", handler, e);
                }
            });
        });
    }

    void addHandler(ProviderEvent event, Consumer<EventDetails> handler) {
        this.handlerMap.get(event).add(handler);
    }

    /**
     * Propagates all events from the originatingEmitter to this one.
     *
     * @param originatingEmitter The emitter to forward events from.
     * @param clientName The client name that will be added to the events.
     */
    void forwardEvents(EventEmitter originatingEmitter, @Nullable String clientName) {
        Arrays.asList(ProviderEvent.values()).stream().forEach(eventType -> {
            originatingEmitter.addHandler(eventType, details -> {

                // set the client name when we proxy the events through.
                details.setClientName(clientName);
                this.emit(eventType, details);
            });
        });
    }
}
