package dev.openfeature.sdk;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EventsTest {

    private static final String CLIENT_NAME = "client name";
    private static final String ANOTHER_CLIENT_NAME = "another client name";
    private static final String FEATURE_KEY = "some key";
    private static final int TIMEOUT = 5000;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private ProviderRepository providerRepository;

    @BeforeEach
    void setupTest() {
        providerRepository = new ProviderRepository();
    }

    class TestEventsProvider implements FeatureProvider, EventProvider {

        private boolean initError = false;
        private String initErrorMessage;

        TestEventsProvider() {
        }

        TestEventsProvider(boolean initError, String initErrorMessage) {
            this.initError = initError;
            this.initErrorMessage = initErrorMessage;
        }

        private EventEmitter eventEmitter = new EventEmitter();

        @Override
        public void initialize() throws Exception {
            if (this.initError) {
                throw new Exception(initErrorMessage);
            }
        }

        public void fireEvent(ProviderEvent event, EventDetails details) {
            this.eventEmitter.emit(event, details);
        }

        @Override
        public Metadata getMetadata() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getMetadata'");
        }

        @Override
        public ProviderEvaluation<Boolean> getBooleanEvaluation(String key, Boolean defaultValue,
                EvaluationContext ctx) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getBooleanEvaluation'");
        }

        @Override
        public ProviderEvaluation<String> getStringEvaluation(String key, String defaultValue,
                EvaluationContext ctx) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getStringEvaluation'");
        }

        @Override
        public ProviderEvaluation<Integer> getIntegerEvaluation(String key, Integer defaultValue,
                EvaluationContext ctx) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getIntegerEvaluation'");
        }

        @Override
        public ProviderEvaluation<Double> getDoubleEvaluation(String key, Double defaultValue,
                EvaluationContext ctx) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getDoubleEvaluation'");
        }

        @Override
        public ProviderEvaluation<Value> getObjectEvaluation(String key, Value defaultValue,
                EvaluationContext ctx) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getObjectEvaluation'");
        }

        @Override
        public EventEmitter getEventEmitter() {
            return eventEmitter;
        }

    };

    @Nested
    class ClientEvents {

        @Nested
        class NamedProvider {

            @Nested
            class Initialization {
                @Test
                @DisplayName("should fire initial READY event when provider init succeeds after client retrieved")
                void initReadyProviderBefore() {
                    final Consumer<EventDetails> handler = mock(Consumer.class);
                    final String name = "initReady";

                    TestEventsProvider provider = new TestEventsProvider();
                    Client client = OpenFeatureAPI.getInstance().getClient(name);
                    client.onProviderReady(handler);
                    // set provider after getting a client
                    OpenFeatureAPI.getInstance().setProvider(name, provider);

                    await()
                            .until(() -> {
                                verify(handler, timeout(TIMEOUT))
                                        .accept(argThat(details -> details.getClientName().equals(name)));
                                return true;
                            });
                }

                @Test
                @DisplayName("should fire initial READY event when provider init succeeds before client retrieved")
                void initReadyProviderAfter() {
                    final Consumer<EventDetails> handler = mock(Consumer.class);
                    final String name = "initReady";

                    TestEventsProvider provider = new TestEventsProvider();
                    // set provider before getting a client
                    OpenFeatureAPI.getInstance().setProvider(name, provider);
                    Client client = OpenFeatureAPI.getInstance().getClient(name);
                    client.onProviderReady(handler);

                    await()
                            .until(() -> {
                                verify(handler, timeout(TIMEOUT))
                                        .accept(argThat(details -> details.getClientName().equals(name)));
                                return true;
                            });
                }

                @Test
                @DisplayName("should fire initial ERROR event when provider init errors after client retrieved")
                void initErrorProviderAfter() {
                    final Consumer<EventDetails> handler = mock(Consumer.class);
                    final String name = "initErrorProviderAfter";
                    final String errMessage = "oh no!";

                    TestEventsProvider provider = new TestEventsProvider(true, errMessage);
                    Client client = OpenFeatureAPI.getInstance().getClient(name);
                    client.onProviderError(handler);
                    // set provider after getting a client
                    OpenFeatureAPI.getInstance().setProvider(name, provider);

                    await()
                            .until(() -> {
                                verify(handler, timeout(TIMEOUT)).accept(argThat(details -> {
                                    return details.getClientName().equals(name)
                                            && details.getMessage().equals(errMessage);
                                }));
                                return true;
                            });
                }

                @Test
                @DisplayName("should fire initial ERROR event when provider init errors before client retrieved")
                void initErrorProviderBefore() {
                    final Consumer<EventDetails> handler = mock(Consumer.class);
                    final String name = "initErrorProviderBefore";
                    final String errMessage = "oh no!";

                    TestEventsProvider provider = new TestEventsProvider(true, errMessage);
                    OpenFeatureAPI.getInstance().onProviderError(handler);
                    OpenFeatureAPI.getInstance().setProvider(name, provider);

                    await()
                            .until(() -> {
                                verify(handler, timeout(TIMEOUT)).accept(argThat(details -> {
                                    return details.getClientName().equals(name)
                                            && details.getMessage().equals(errMessage);
                                }));
                                return true;
                            });
                }
            }

            @Nested
            class ProviderEvents {

                @Test
                @DisplayName("should propagate events when provider set before client retrieved")
                void shouldPropagateBefore() {
                    final Consumer<EventDetails> handler = mock(Consumer.class);
                    final String name = "providerBefore";

                    TestEventsProvider provider = new TestEventsProvider();
                    // set provider before getting a client
                    OpenFeatureAPI.getInstance().setProvider(name, provider);
                    Client client = OpenFeatureAPI.getInstance().getClient(name);
                    client.onProviderConfigurationChanged(handler);
                    provider.fireEvent(ProviderEvent.PROVIDER_CONFIGURATION_CHANGED, EventDetails.builder().build());
                    await()
                            .until(() -> {
                                verify(handler, timeout(TIMEOUT))
                                        .accept(argThat(details -> details.getClientName().equals(name)));
                                return true;
                            });
                }

                @Test
                @DisplayName("should propagate events when provider set after client retrieved")
                void shouldPropagateAfter() {

                    final Consumer<EventDetails> handler = mock(Consumer.class);
                    final String name = "shouldPropagateAfter";

                    TestEventsProvider provider = new TestEventsProvider();
                    Client client = OpenFeatureAPI.getInstance().getClient(name);
                    client.onProviderConfigurationChanged(handler);
                    // set provider after getting a client
                    OpenFeatureAPI.getInstance().setProvider(name, provider);
                    provider.fireEvent(ProviderEvent.PROVIDER_CONFIGURATION_CHANGED, EventDetails.builder().build());
                    await()
                            .until(() -> {
                                verify(handler, timeout(TIMEOUT))
                                        .accept(argThat(details -> details.getClientName().equals(name)));
                                return true;
                            });
                }
            }
        }
    }

    @Nested
    class ApiEvents {

        @Nested
        class NamedProvider {

            @Nested
            class Initialization {

                @Test
                @DisplayName("should fire initial READY event when provider init succeeds")
                void apiInitReady() {
                    final Consumer<EventDetails> handler = mock(Consumer.class);
                    final String name = "apiInitReady";

                    TestEventsProvider provider = new TestEventsProvider();
                    OpenFeatureAPI.getInstance().onProviderReady(handler);
                    OpenFeatureAPI.getInstance().setProvider(name, provider);

                    await()
                            .until(() -> {
                                verify(handler, timeout(TIMEOUT))
                                        .accept(argThat(details -> details.getClientName().equals(name)));
                                return true;
                            });
                }

                @Test
                @DisplayName("should fire initial ERROR event when provider init errors")
                void apiInitError() {
                    final Consumer<EventDetails> handler = mock(Consumer.class);
                    final String name = "apiInitError";
                    final String errMessage = "oh no!";

                    TestEventsProvider provider = new TestEventsProvider(true, errMessage);
                    OpenFeatureAPI.getInstance().onProviderError(handler);
                    OpenFeatureAPI.getInstance().setProvider(name, provider);

                    await()
                            .until(() -> {
                                verify(handler, timeout(TIMEOUT)).accept(argThat(details -> {
                                    return details.getClientName().equals(name)
                                            && details.getMessage().equals(errMessage);
                                }));
                                return true;
                            });
                }
            }

            @Nested
            class ProviderEvents {

                @Test
                @DisplayName("should propagate events")
                void apiShouldPropagateEvents() {
                    final Consumer<EventDetails> handler = mock(Consumer.class);
                    final String name = "apiProviderBefore";

                    TestEventsProvider provider = new TestEventsProvider();
                    OpenFeatureAPI.getInstance().setProvider(name, provider);
                    OpenFeatureAPI.getInstance().onProviderConfigurationChanged(handler);
                    provider.fireEvent(ProviderEvent.PROVIDER_CONFIGURATION_CHANGED, EventDetails.builder().build());

                    await()
                            .until(() -> {
                                verify(handler, timeout(TIMEOUT))
                                        .accept(argThat(details -> details.getClientName().equals(name)));
                                return true;
                            });
                }
            }
        }
    }
}
