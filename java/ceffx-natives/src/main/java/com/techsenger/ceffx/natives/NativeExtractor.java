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
import java.util.Collections;
import java.util.stream.Stream;

/**
 * Extracts the CEFFX native payload (native libraries and, on macOS, the helper app bundles) bundled inside this
 * classifier's jar into a target directory on disk, so they can be loaded via {@code java.library.path} or
 * referenced directly by {@code CefSettings}.
 *
 * <p>On macOS, helper app bundles are placed inside a {@code Frameworks} subdirectory of the target directory,
 * matching the relative layout CEF expects. On Linux and Windows, all files are placed directly in the target
 * directory.
 *
 * @author Pavel Castornii
 */
public final class NativeExtractor {

    private static final String FRAMEWORKS_DIR = "Frameworks";

    public static void extract(Path targetDir) throws IOException {
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
                copyDirectory(source, targetDir);
            }
        } else {
            copyDirectory(Path.of(uri), targetDir);
        }
    }

    private static void copyDirectory(Path source, Path targetDir) throws IOException {
        try (Stream<Path> stream = Files.walk(source)) {
            for (Path entry : (Iterable<Path>) stream::iterator) {
                Path relative = source.relativize(entry);
                Path target = resolveTarget(targetDir, relative);
                if (Files.isDirectory(entry)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(entry, target, StandardCopyOption.REPLACE_EXISTING);
                    if (!target.toString().endsWith(".dll") && !target.toString().endsWith(".plist")) {
                        target.toFile().setExecutable(true);
                    }
                }
            }
        }
    }

    /**
     * Resolves the target path for a payload entry. On macOS ({@link NativeProps#CEFFX_CLASSIFIER} is
     * "mac" or "mac-aarch64"), CEF resolves helper app bundles relative to a "Frameworks" directory, so
     * any top-level ".app" bundle (and everything inside it) is redirected into
     * {@code targetDir/Frameworks/...} instead of being placed flat in {@code targetDir}. On other
     * platforms, or for any other entry, the path is unchanged from before.
     */
    private static Path resolveTarget(Path targetDir, Path relative) {
        boolean isMac = NativeProps.CEFFX_CLASSIFIER.startsWith("mac");
        boolean isAppBundle = relative.getNameCount() > 0 && relative.getName(0).toString().endsWith(".app");
        if (isMac && isAppBundle) {
            return targetDir.resolve(FRAMEWORKS_DIR).resolve(relative);
        }
        return targetDir.resolve(relative);
    }

    private NativeExtractor() {
        // empty
    }
}