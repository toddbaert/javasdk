package dev.openfeature.sdk;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class ProviderRepository {

    private final Map<String, FeatureProvider> providers = new ConcurrentHashMap<>();
    private final Map<String, EventEmitter> clientEmitters = new ConcurrentHashMap<>();
    private final AtomicReference<FeatureProvider> defaultProvider = new AtomicReference<>(new NoOpProvider());
    private final EventEmitter defaultEmitter = new EventEmitter();
    private final ExecutorService taskExecutor = Executors.newCachedThreadPool();
    private final Map<String, FeatureProvider> initializingNamedProviders = new ConcurrentHashMap<>();
    private FeatureProvider initializingDefaultProvider;

    /**
     * Return the default provider.
     */
    public FeatureProvider getProvider() {
        return defaultProvider.get();
    }

    /**
     * Fetch a provider for a named client. If not found, return the default.
     *
     * @param name The client name to look for.
     * @return A named {@link FeatureProvider}
     */
    public FeatureProvider getProvider(String name) {
        return Optional.ofNullable(name).map(this.providers::get).orElse(this.defaultProvider.get());
    }

    /**
     * Set the default provider.
     */
    public void setProvider(FeatureProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Provider cannot be null");
        }
        initializeProvider(provider);
    }

    /**
     * Add a provider for a named client.
     *
     * @param clientName The name of the client.
     * @param provider   The provider to set.
     */
    public void setProvider(String clientName, FeatureProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Provider cannot be null");
        }
        if (clientName == null) {
            throw new IllegalArgumentException("clientName cannot be null");
        }
        initializeProvider(clientName, provider);
    }

    /**
     * Get the emitter for the referenced clientName, or the default.
     * The emitter is created if it doesn't exist.
     * 
     * @param clientName name for the client, or null for default.
     * @return existing or new emitter for this clientName (or the default).
     */
    EventEmitter getAndCacheEmitter(@Nullable String clientName) {
        if (clientName == null) {
            return this.defaultEmitter;
        }
        if (this.clientEmitters.get(clientName) == null) {
            this.clientEmitters.put(clientName, new EventEmitter());
        }
        return this.clientEmitters.get(clientName);
    }

    private void initializeProvider(FeatureProvider provider) {
        initializingDefaultProvider = provider;
        initializeProvider(null, provider, this::updateDefaultProviderAfterInitialization);
    }

    private void initializeProvider(String clientName, FeatureProvider provider) {
        initializingNamedProviders.put(clientName, provider);

        // if this is a provider that supports events, subscribe to it's event emitter.
        if (EventProvider.isEventProvider(provider)) {
            this.getAndCacheEmitter(clientName).forwardEvents(((EventProvider) provider).getEventEmitter(), clientName);
        }
        initializeProvider(clientName, provider, newProvider -> updateProviderAfterInit(clientName, newProvider));
    }

    private void initializeProvider(@Nullable String clientName, FeatureProvider provider,
            Consumer<FeatureProvider> afterInitialization) {
        taskExecutor.submit(() -> {
            try {
                if (!isProviderRegistered(provider)) {
                    provider.initialize();
                }
                afterInitialization.accept(provider);
            } catch (Exception e) {
                log.error("Exception when initializing feature provider {}", provider.getClass().getName(), e);
                EventEmitter eventEmitter = this.getAndCacheEmitter(clientName);
                EventDetails errorEvent = EventDetails.builder().clientName(clientName).message(e.getMessage()).build();
                eventEmitter.emit(ProviderEvent.PROVIDER_ERROR, errorEvent);
                OpenFeatureAPI.getInstance().emitter.emit(ProviderEvent.PROVIDER_ERROR, errorEvent);
            }
        });
    }

    private void updateProviderAfterInit(String clientName, FeatureProvider newProvider) {
        Optional
                .ofNullable(initializingNamedProviders.get(clientName))
                .filter(initializingProvider -> initializingProvider.equals(newProvider))
                .ifPresent(provider -> updateNamedProviderAfterInitialization(clientName, provider));
    }

    private void updateDefaultProviderAfterInitialization(FeatureProvider initializedProvider) {
        Optional
                .ofNullable(this.initializingDefaultProvider)
                .filter(initializingProvider -> initializingProvider.equals(initializedProvider))
                .ifPresent(provider -> {
                    EventDetails readyEvent = EventDetails.builder().build();
                    this.defaultEmitter.emit(ProviderEvent.PROVIDER_READY, readyEvent);
                    OpenFeatureAPI.getInstance().emitter.emit(ProviderEvent.PROVIDER_READY, readyEvent);
                    replaceDefaultProvider(provider);
                });
    }

    private void replaceDefaultProvider(FeatureProvider provider) {
        FeatureProvider oldProvider = this.defaultProvider.getAndSet(provider);
        if (isOldProviderNotBoundByName(oldProvider)) {
            shutdownProvider(oldProvider);
        }
    }

    private boolean isOldProviderNotBoundByName(FeatureProvider oldProvider) {
        return !this.providers.containsValue(oldProvider);
    }

    private void updateNamedProviderAfterInitialization(String clientName, FeatureProvider initializedProvider) {
        Optional
                .ofNullable(this.initializingNamedProviders.get(clientName))
                .filter(initializingProvider -> initializingProvider.equals(initializedProvider))
                .ifPresent(provider -> {
                    EventDetails readyEvent = EventDetails.builder().clientName(clientName).build();
                    this.clientEmitters.get(clientName).emit(ProviderEvent.PROVIDER_READY, readyEvent);
                    OpenFeatureAPI.getInstance().emitter.emit(ProviderEvent.PROVIDER_READY, readyEvent);
                    replaceNamedProviderAndShutdownOldOne(clientName, provider);
                });
    }

    private void replaceNamedProviderAndShutdownOldOne(String clientName, FeatureProvider provider) {
        FeatureProvider oldProvider = this.providers.put(clientName, provider);
        this.initializingNamedProviders.remove(clientName, provider);
        if (!isProviderRegistered(oldProvider)) {
            shutdownProvider(oldProvider);
        }
    }

    private boolean isProviderRegistered(FeatureProvider oldProvider) {
        return this.providers.containsValue(oldProvider) || this.defaultProvider.get().equals(oldProvider);
    }

    private void shutdownProvider(FeatureProvider provider) {
        taskExecutor.submit(() -> {
            try {
                provider.shutdown();
            } catch (Exception e) {
                log.error("Exception when shutting down feature provider {}", provider.getClass().getName(), e);
            }
        });
    }

    /**
     * Shutdowns this repository which includes shutting down all FeatureProviders
     * that are registered,
     * including the default feature provider.
     */
    public void shutdown() {
        Stream
                .concat(Stream.of(this.defaultProvider.get()), this.providers.values().stream())
                .distinct()
                .forEach(this::shutdownProvider);
        setProvider(new NoOpProvider());
        this.providers.clear();
        taskExecutor.shutdown();
    }
}
