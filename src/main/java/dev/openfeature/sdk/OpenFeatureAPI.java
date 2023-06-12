package dev.openfeature.sdk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import dev.openfeature.sdk.internal.AutoCloseableLock;
import dev.openfeature.sdk.internal.AutoCloseableReentrantReadWriteLock;
import lombok.extern.slf4j.Slf4j;

/**
 * A global singleton which holds base configuration for the OpenFeature library.
 * Configuration here will be shared across all {@link Client}s.
 */
@Slf4j
public class OpenFeatureAPI implements EventHandling<OpenFeatureAPI> {
    // package-private multi-read/single-write lock
    static AutoCloseableReentrantReadWriteLock hooksLock = new AutoCloseableReentrantReadWriteLock();
    static AutoCloseableReentrantReadWriteLock contextLock = new AutoCloseableReentrantReadWriteLock();
    private EvaluationContext evaluationContext;
    private final List<Hook> apiHooks;
    private ProviderRepository providerRepository = new ProviderRepository();
    final EventEmitter emitter = new EventEmitter();

    protected OpenFeatureAPI() {
        this.apiHooks = new ArrayList<>();
    }

    private static class SingletonHolder {
        private static final OpenFeatureAPI INSTANCE = new OpenFeatureAPI();
    }

    /**
     * Provisions the {@link OpenFeatureAPI} singleton (if needed) and returns it.
     *
     * @return The singleton instance.
     */
    public static OpenFeatureAPI getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public Metadata getProviderMetadata() {
        return getProvider().getMetadata();
    }

    public Metadata getProviderMetadata(String clientName) {
        return getProvider(clientName).getMetadata();
    }

    public Client getClient() {
        return getClient(null, null);
    }

    public Client getClient(@Nullable String name) {
        return getClient(name, null);
    }

    public Client getClient(@Nullable String name, @Nullable String version) {
        return new OpenFeatureClient(this, this.providerRepository.getAndCacheEmitter(name), name, version);
    }

    /**
     * {@inheritDoc}
     */
    public void setEvaluationContext(EvaluationContext evaluationContext) {
        try (AutoCloseableLock __ = contextLock.writeLockAutoCloseable()) {
            this.evaluationContext = evaluationContext;
        }
    }

    /**
     * {@inheritDoc}
     */
    public EvaluationContext getEvaluationContext() {
        try (AutoCloseableLock __ = contextLock.readLockAutoCloseable()) {
            return this.evaluationContext;
        }
    }

    /**
     * Set the default provider.
     */
    public void setProvider(FeatureProvider provider) {
        propagateEventsIfSupported(provider, null);
        providerRepository.setProvider(provider);
    }

    /**
     * Add a provider for a named client.
     *
     * @param clientName The name of the client.
     * @param provider   The provider to set.
     */
    public void setProvider(String clientName, FeatureProvider provider) {
        propagateEventsIfSupported(provider, clientName);
        providerRepository.setProvider(clientName, provider);
    }

    /**
     * Return the default provider.
     */
    public FeatureProvider getProvider() {
        return providerRepository.getProvider();
    }

    /**
     * Fetch a provider for a named client. If not found, return the default.
     *
     * @param name The client name to look for.
     * @return A named {@link FeatureProvider}
     */
    public FeatureProvider getProvider(String name) {
        return providerRepository.getProvider(name);
    }

    /**
     * {@inheritDoc}
     */
    public void addHooks(Hook... hooks) {
        try (AutoCloseableLock __ = hooksLock.writeLockAutoCloseable()) {
            this.apiHooks.addAll(Arrays.asList(hooks));
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<Hook> getHooks() {
        try (AutoCloseableLock __ = hooksLock.readLockAutoCloseable()) {
            return this.apiHooks;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void clearHooks() {
        try (AutoCloseableLock __ = hooksLock.writeLockAutoCloseable()) {
            this.apiHooks.clear();
        }
    }

    public void shutdown() {
        providerRepository.shutdown();
    }

    @Override
    public OpenFeatureAPI onProviderReady(Consumer<EventDetails> handler) {
        return this.on(ProviderEvent.PROVIDER_READY, handler);
    }

    @Override
    public OpenFeatureAPI onProviderConfigurationChanged(Consumer<EventDetails> handler) {
        return this.on(ProviderEvent.PROVIDER_CONFIGURATION_CHANGED, handler);
    }

    @Override
    public OpenFeatureAPI onProviderError(Consumer<EventDetails> handler) {
        return this.on(ProviderEvent.PROVIDER_ERROR, handler);
    }

    @Override
    public OpenFeatureAPI onProviderStale(Consumer<EventDetails> handler) {
        return this.on(ProviderEvent.PROVIDER_STALE, handler);
    }

    private OpenFeatureAPI on(ProviderEvent event, Consumer<EventDetails> consumer) {
        this.emitter.addHandler(event, consumer);
        return this;
    }

    private void propagateEventsIfSupported(FeatureProvider provider, @Nullable String clientName) {
        if (EventProvider.isEventProvider(provider)) {
            this.emitter.forwardEvents(((EventProvider)provider).getEventEmitter(), clientName);
        }
    }

    /**
     * This method is only here for testing as otherwise all tests after the API shutdown test would fail.
     */
    final void resetProviderRepository() {
        providerRepository = new ProviderRepository();
    }
}
