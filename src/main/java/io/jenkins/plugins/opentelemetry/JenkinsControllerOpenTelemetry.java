/*
 * Copyright The Original Author or Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.jenkins.plugins.opentelemetry;

import static io.jenkins.plugins.opentelemetry.semconv.ConfigurationKey.OTEL_LOGS_EXPORTER;
import static io.jenkins.plugins.opentelemetry.semconv.ConfigurationKey.OTEL_LOGS_MIRROR_TO_DISK;

import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import io.jenkins.plugins.opentelemetry.api.ReconfigurableOpenTelemetry;
import io.jenkins.plugins.opentelemetry.semconv.ExtendedJenkinsAttributes;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.resources.ProcessResourceProvider;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * {@link OpenTelemetry} instance intended to live on the Jenkins Controller.
 */
@Extension(ordinal = Integer.MAX_VALUE)
public class JenkinsControllerOpenTelemetry implements ExtensionPoint {

    /**
     * See {@code OTEL_JAVA_DISABLED_RESOURCE_PROVIDERS}
     */
    public static final String DEFAULT_OTEL_JAVA_DISABLED_RESOURCE_PROVIDERS = ProcessResourceProvider.class.getName();

    @VisibleForTesting
    @Inject
    protected ReconfigurableOpenTelemetry openTelemetry;

    private Tracer defaultTracer;
    private Meter defaultMeter;

    public JenkinsControllerOpenTelemetry() {
        super();
    }

    @PostConstruct
    public void postConstruct() {
        String opentelemetryPluginVersion = OtelUtils.getOpentelemetryPluginVersion();

        this.defaultTracer = this.openTelemetry
                .tracerBuilder(ExtendedJenkinsAttributes.INSTRUMENTATION_NAME)
                .setInstrumentationVersion(opentelemetryPluginVersion)
                .build();

        this.defaultMeter = openTelemetry
                .meterBuilder(ExtendedJenkinsAttributes.INSTRUMENTATION_NAME)
                .setInstrumentationVersion(opentelemetryPluginVersion)
                .build();
    }

    @NonNull
    public Tracer getDefaultTracer() {
        return defaultTracer;
    }

    @NonNull
    public Meter getDefaultMeter() {
        return defaultMeter;
    }

    public boolean isLogsEnabled() {
        String otelLogsExporter = openTelemetry.getConfig().getString(OTEL_LOGS_EXPORTER.asProperty(), "none");
        return !Objects.equals(otelLogsExporter, "none");
    }

    public boolean isOtelLogsMirrorToDisk() {
        String mirrorLogsToDisk = openTelemetry.getConfig().getString(OTEL_LOGS_MIRROR_TO_DISK.asProperty(), "false");
        return Objects.equals(mirrorLogsToDisk, "true");
    }

    @VisibleForTesting
    @NonNull
    @Deprecated
    protected OpenTelemetrySdk getOpenTelemetrySdk() {
        return (OpenTelemetrySdk) Optional.ofNullable(openTelemetry)
                .map(ReconfigurableOpenTelemetry::getImplementation)
                .orElseThrow(() -> new IllegalStateException("OpenTelemetry not initialized"));
    }

    public void initialize(@NonNull OpenTelemetryConfiguration configuration) {
        openTelemetry.configure(
                configuration.toOpenTelemetryProperties(), configuration.toOpenTelemetryResource(), true);
    }

    public static JenkinsControllerOpenTelemetry get() {
        return ExtensionList.lookupSingleton(JenkinsControllerOpenTelemetry.class);
    }
}
