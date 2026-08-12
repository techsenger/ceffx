/*
 * Copyright 2026 Pavel Castornii.
 *
 * Licensed under the BSD 3-Clause License. See LICENSE file for details.
 */

package com.techsenger.ceffx.natives;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Provides the CEF version and platform information bundled into this classifier's jar, along with the resulting
 * download links for the official CEF distribution archives.
 *
 * <p>Values are read once, at class initialization time, from the {@code native.properties} resource shipped alongside
 * this class in the {@code com.techsenger.ceffx.natives.props} package. This makes the required CEF version
 * discoverable by a build tool, installer or launcher without loading the native runtime - see {@link #CEF_VERSION}.
 *
 * @author Pavel Castornii
 */
public final class NativeProps {

    /**
     * The CEF version required by the native library in this jar, for example
     * {@code 146.0.10+g8219561+chromium-146.0.7680.179}.
     */
    public static final String CEF_VERSION;

    /**
     * The CEF distribution platform token, for example {@code linux64}, {@code macosx64}, {@code macosarm64} or
     * {@code windows64}. This is CEF's own distribution naming, which does not match the Maven classifier of this jar.
     */
    public static final String CEF_PLATFORM;

    /**
     * The exact distribution name used by CEF for this version and platform, for example
     * {@code cef_binary_146.0.10+g8219561+chromium-146.0.7680.179_linux64}.
     */
    public static final String CEF_DISTRIBUTION;

    /**
     * The base URL from which CEF distribution archives are downloaded, for example
     * {@code https://cef-builds.spotifycdn.com/}. Includes a trailing slash, so a distribution archive's file name
     * can be appended directly.
     */
    public static final String CEF_BASE_URL;

    /**
     * Direct download link for the standard CEF distribution archive matching {@link #CEF_DISTRIBUTION}.
     */
    public static final String CEF_URL;

    /**
     * Direct download link for the minimal CEF distribution archive matching {@link #CEF_DISTRIBUTION}.
     */
    public static final String CEF_URL_MINIMAL;

    /**
     * The Maven classifier of this jar, for example {@code linux}, {@code mac}, {@code mac-aarch64} or {@code win}.
     */
    public static final String CEFFX_CLASSIFIER;

    static {
        var properties = new Properties();
        try (InputStream in = NativeProps.class.getResourceAsStream("native.properties")) {
            if (in == null) {
                throw new IOException("native.properties not found next to " + NativeProps.class.getName());
            }
            properties.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read native.properties", e);
        }
        CEF_VERSION = requireProperty(properties, "cef.version");
        CEF_BASE_URL = requireProperty(properties, "cef.baseUrl");
        CEF_PLATFORM = requireProperty(properties, "cef.platform");
        CEF_DISTRIBUTION = requireProperty(properties, "cef.distribution");
        CEFFX_CLASSIFIER = requireProperty(properties, "ceffx.classifier");
        var encodedDistribution = urlEncodeDistribution(CEF_DISTRIBUTION);
        CEF_URL = CEF_BASE_URL + encodedDistribution + ".tar.bz2";
        CEF_URL_MINIMAL = CEF_BASE_URL + encodedDistribution + "_minimal.tar.bz2";
    }

    private static String requireProperty(Properties properties, String key) {
        var value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing '" + key + "' in native.properties");
        }
        return value;
    }

    /**
     * Percent-encodes the '+' characters in a CEF distribution name, matching the encoding CEF uses in
     * its own download links (e.g. {@code cef_binary_146.0.10%2Bg8219561%2Bchromium-...}). Every other
     * character in a distribution name (letters, digits, '.', '-', '_') is already URL-safe as-is, so
     * only '+' needs replacing.
     */
    private static String urlEncodeDistribution(String distribution) {
        return distribution.replace("+", "%2B");
    }

    private NativeProps() {
        // empty
    }
}
