/*
 * Copyright 2026 Pavel Castornii.
 *
 * Licensed under the BSD 3-Clause License. See LICENSE file for details.
 */

package com.techsenger.ceffx.natives;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Extracts the CEFFX native payload (native libraries and, on macOS, the helper app bundles) bundled inside this
 * classifier's jar, flat into a target directory on disk, so they can be loaded via {@code java.library.path} or
 * referenced directly by {@code CefSettings}.
 *
 * <p>This class only copies files as they are laid out in the jar - it has no knowledge of any platform-specific
 * directory requirements (such as the {@code Frameworks} subdirectory CEF expects on macOS). Arranging the extracted
 * files into whatever layout the runtime environment requires is the responsibility of the caller.
 *
 * @author Pavel Castornii
 */
public final class NativeExtractor {

    /**
     * Receives progress updates during {@link #extract(Path, ProgressListener)}.
     */
    public interface ProgressListener {

        /**
         * Called after each file is copied.
         *
         * @param progress completion fraction so far, between 0.0 and 1.0
         */
        void onProgress(double progress);
    }

    /**
     * Extracts the native payload into {@code targetDir}, creating the directory if it does not already
     * exist. Existing files at the destination are overwritten. Equivalent to
     * {@link #extract(Path, ProgressListener)} with no listener.
     *
     * @param targetDir the directory to extract the native payload into
     * @return an unmodifiable list of the paths extracted, relative to {@code targetDir}, in the order
     *     they were copied
     * @throws IOException if the native payload cannot be located in this jar, or if extraction fails
     */
    public static List<String> extract(Path targetDir) throws IOException {
        return extract(targetDir, null);
    }

    /**
     * Same as {@link #extract(Path)}, but reports progress as extraction proceeds.
     *
     * @param targetDir the directory to extract the native payload into
     * @param listener  receives progress updates, may be {@code null}
     * @return an unmodifiable list of the paths extracted, relative to {@code targetDir}, in the order
     *     they were copied
     * @throws IOException if the native payload cannot be located in this jar, or if extraction fails
     */
    public static List<String> extract(Path targetDir, ProgressListener listener) throws IOException {
        Files.createDirectories(targetDir);
        URL resourceUrl = NativeExtractor.class.getResource("/native/libs");
        if (resourceUrl == null) {
            throw new IOException("Native libs not found in jar");
        }
        URI uri;
        try {
            uri = resourceUrl.toURI();
        } catch (URISyntaxException e) {
            throw new IOException("Failed to get URI for native libs", e);
        }
        if ("jar".equals(uri.getScheme())) {
            try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                Path source = fs.getPath("/native/libs");
                return copyDirectory(source, targetDir, listener);
            }
        } else {
            return copyDirectory(Path.of(uri), targetDir, listener);
        }
    }

    private static List<String> copyDirectory(Path source, Path targetDir, ProgressListener listener)
            throws IOException {
        List<Path> files;
        try (Stream<Path> stream = Files.walk(source)) {
            files = stream.filter(Files::isRegularFile).collect(Collectors.toList());
        }
        var extracted = new ArrayList<String>();
        var total = files.size();
        var done = 0;
        for (Path entry : files) {
            var relative = source.relativize(entry);
            var target = targetDir.resolve(relative.toString());
            Files.createDirectories(target.getParent());
            Files.copy(entry, target, StandardCopyOption.REPLACE_EXISTING);
            var fileName = target.getFileName().toString();
            if (!fileName.endsWith(".dll") && !fileName.endsWith(".plist")) {
                target.toFile().setExecutable(true);
            }
            extracted.add(relative.toString().replace('\\', '/'));
            done++;
            if (listener != null) {
                listener.onProgress(total == 0 ? 1.0 : (double) done / total);
            }
        }
        return Collections.unmodifiableList(extracted);
    }

    private NativeExtractor() {
        // empty
    }
}