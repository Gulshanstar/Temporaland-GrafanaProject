package helloworldapp;

import com.sun.net.httpserver.HttpServer;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

import io.temporal.client.WorkflowClientOptions;
import io.temporal.common.reporter.MicrometerClientStatsReporter;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import com.uber.m3.tally.RootScopeBuilder;
import com.uber.m3.tally.Scope;
import com.uber.m3.util.Duration;
import com.uber.m3.util.ImmutableMap;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import io.temporal.serviceclient.SimpleSslContextBuilder;

import java.io.FileInputStream;
import java.io.InputStream;

public class HelloWorldWorker {

    public static void main(String[] args) {

        // Set up metrics and Prometheus registry
        PrometheusMeterRegistry yourRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        Scope yourScope = new RootScopeBuilder()
                .tags(ImmutableMap.of(
                        "customtag1", "customvalue1",
                        "customtag2", "customvalue2"))
                .reporter(new MicrometerClientStatsReporter(yourRegistry))
                .reportEvery(Duration.ofSeconds(10));

        // Start Prometheus scrape endpoint
        HttpServer scrapeEndpoint = startPrometheusScrapeEndpoint(yourRegistry, 8077);

        // Get a Workflow service stub.
        // WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

        WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder()
                        .setMetricsScope(yourScope)
                        .build());

        /*
         * Get a Workflow service client which can be used to start, Signal, and Query
         * Workflow Executions.
         */
        // Create a Workflow service client with metrics
        WorkflowClient client = WorkflowClient.newInstance(service,
                WorkflowClientOptions.newBuilder().build());

        /*
         * Define the workflow factory. It is used to create workflow workers that poll
         * specific Task Queues.
         */
        WorkerFactory factory = WorkerFactory.newInstance(client);

        /*
         * Define the workflow worker. Workflow workers listen to a defined task queue
         * and process
         * workflows and activities.
         */
        Worker worker = factory.newWorker(Shared.HELLO_WORLD_TASK_QUEUE);

        /*
         * Register our workflow implementation with the worker.
         * Workflow implementations must be known to the worker at runtime in
         * order to dispatch workflow tasks.
         */
        worker.registerWorkflowImplementationTypes(HelloWorldWorkflowImpl.class);

        /*
         * Register our Activity Types with the Worker. Since Activities are stateless
         * and thread-safe,
         * the Activity Type is a shared instance.
         */
        worker.registerActivitiesImplementations(new HelloWorldActivitiesImpl());

        /*
         * Start all the workers registered for a specific task queue.
         * The started workers then start polling for workflows and activities.
         */
        factory.start();

    }

    // Add the scrape endpoint method
    private static HttpServer startPrometheusScrapeEndpoint(
            PrometheusMeterRegistry yourRegistry, int port) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext(
                    "/metrics",
                    httpExchange -> {
                        String response = yourRegistry.scrape();
                        httpExchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                        try (OutputStream os = httpExchange.getResponseBody()) {
                            os.write(response.getBytes(StandardCharsets.UTF_8));
                        }
                    });
            server.start();
            return server;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}