/*
 * Copyright 2026 Pavel Castornii.
 *
 * Licensed under the BSD 3-Clause License. See LICENSE file for details.
 */

package com.techsenger.ceffx.natives;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/**
 * Orchestrates deployment of the CEF runtime and the CEFFX native payload into a single target directory, so that a
 * JavaFX application can locate and load them at startup.
 *
 * <p>Deployment consists of three independently tracked {@link Operation}s: downloading the CEF distribution archive,
 * extracting that archive, and extracting the CEFFX native payload bundled in this jar (via {@link NativeExtractor}).
 * Each operation records a marker of what it produced in {@code targetDir}, so that {@link #getStatus} can report
 * which operations have already completed and {@link #deploy} can resume after a partial or interrupted run without
 * redoing completed work.
 *
 * <p>On macOS, the CEFFX helper app bundles and the CEF framework are placed inside a {@code Frameworks} subdirectory
 * of {@code targetDir}, matching the relative layout CEF requires there. On Linux and Windows, everything is placed
 * directly in {@code targetDir}. See the CEFFX README for the full directory layout on each platform.
 *
 * <p>This class is not safe for concurrent use from multiple threads against the same or different target directories
 * - all public methods are synchronized on this class, so calls from different threads are serialized.
 *
 * <p>This class requires Apache Commons Compress to extract the downloaded CEF distribution archive. The dependency
 * is declared as {@code requires static} in this module's {@code module-info.java}, so it is not added to the
 * runtime module graph automatically. Applications using the Java Platform Module System must add
 * {@code requires org.apache.commons.compress;} to their own {@code module-info.java} to use this class; otherwise a
 * {@link NoClassDefFoundError} is thrown when this class is first used. Applications running on the classpath
 * (unnamed module) are unaffected, since Commons Compress is a regular (non-optional) Maven dependency of this
 * module and is therefore already present on the classpath.
 *
 * @author Pavel Castornii
 */
public final class NativeDeployer {

    /**
     * A single step of native deployment that {@link #getStatus} and {@link #deploy} track independently.
     */
    public enum Operation {

        /** Downloading the CEF distribution archive matching {@link NativeProps#CEF_DISTRIBUTION}. */
        DOWNLOAD_CEF,
        /** Extracting the downloaded CEF distribution archive. */
        EXTRACT_CEF,
        /** Extracting the CEFFX native payload bundled in this jar, via {@link NativeExtractor}. */
        EXTRACT_CEFFX
    }

    /**
     * Receives progress updates during {@link #deploy}.
     */
    public interface ProgressListener {

        /**
         * Called as {@code operation} progresses.
         *
         * @param operation the operation currently in progress
         * @param progress  completion fraction between 0.0 and 1.0
         */
        void onProgress(Operation operation, double progress);
    }

    private static final String FRAMEWORKS_DIR = "Frameworks";

    private static final String CEF_FILES_MARKER = "cef.files";

    private static final String CEFFX_FILES_MARKER = "ceffx.files";

    private static final String PART_SUFFIX = ".part";

    private static final String MACOS_LIB_DIR = "Lib";

    /**
     * Returns the unmodifiable set of deploy operations that have already completed successfully in {@code targetDir}.
     *
     * @param targetDir the directory to inspect
     * @return the operations already completed; empty if none have run yet
     * @throws IOException if {@code targetDir} cannot be read
     */
    public static synchronized Set<Operation> getStatus(Path targetDir) throws IOException {
        var status = EnumSet.noneOf(Operation.class);
        if (!Files.isDirectory(targetDir)) {
             return Collections.unmodifiableSet(status);
        }
        if (Files.exists(targetDir.resolve(archiveFileName()))) {
            status.add(Operation.DOWNLOAD_CEF);
        }
        if (Files.exists(targetDir.resolve(CEF_FILES_MARKER))) {
            status.add(Operation.EXTRACT_CEF);
        }
        if (Files.exists(targetDir.resolve(CEFFX_FILES_MARKER))) {
            status.add(Operation.EXTRACT_CEFFX);
        }
        return Collections.unmodifiableSet(status);
    }

    /**
     * Performs whichever operations in {@link #getStatus} have not yet completed in {@code targetDir}.
     * Operations already marked complete are skipped.
     *
     * @param targetDir the directory to deploy into; created if it does not already exist
     * @param listener  receives progress updates, may be {@code null}
     * @throws IOException if any operation fails
     */
    public static synchronized void deploy(Path targetDir, ProgressListener listener) throws IOException {
        deploy(targetDir, listener, EnumSet.noneOf(Operation.class));
    }

    /**
     * Same as {@link #deploy(Path, ProgressListener)}, but re-runs every operation in {@code forceUpdate}
     * regardless of its current status. Forcing one operation does not force any operation it would
     * normally depend on - for example, forcing {@link Operation#DOWNLOAD_CEF} alone does not force
     * {@link Operation#EXTRACT_CEF} to re-extract the newly downloaded archive. Callers are responsible
     * for combining operations sensibly (typically all three, for a full clean redeploy).
     *
     * @param targetDir   the directory to deploy into; created if it does not already exist
     * @param listener    receives progress updates, may be {@code null}
     * @param forceUpdate operations to re-run even if already marked complete
     * @throws IOException if any operation fails
     */
    public static synchronized void deploy(Path targetDir, ProgressListener listener, Set<Operation> forceUpdate)
            throws IOException {
        Files.createDirectories(targetDir);
        var status = EnumSet.noneOf(Operation.class);
        status.addAll(getStatus(targetDir));

        if (forceUpdate.contains(Operation.DOWNLOAD_CEF)) {
            Files.deleteIfExists(targetDir.resolve(archiveFileName()));
            status.remove(Operation.DOWNLOAD_CEF);
        }
        if (!status.contains(Operation.DOWNLOAD_CEF)) {
            downloadCef(targetDir, listener);
        }

        if (forceUpdate.contains(Operation.EXTRACT_CEF)) {
            undoExtraction(targetDir, CEF_FILES_MARKER);
            status.remove(Operation.EXTRACT_CEF);
        }
        if (!status.contains(Operation.EXTRACT_CEF)) {
            extractCef(targetDir, listener);
        }

        if (forceUpdate.contains(Operation.EXTRACT_CEFFX)) {
            undoExtraction(targetDir, CEFFX_FILES_MARKER);
            status.remove(Operation.EXTRACT_CEFFX);
        }
        if (!status.contains(Operation.EXTRACT_CEFFX)) {
            extractCeffx(targetDir, listener);
        }
    }

    // -------------------------------------------------------------------------------------------
    // DOWNLOAD_CEF
    // -------------------------------------------------------------------------------------------

    private static String archiveFileName() {
        return NativeProps.CEF_DISTRIBUTION + "_minimal.tar.bz2";
    }

    private static void downloadCef(Path targetDir, ProgressListener listener) throws IOException {
        var expectedSha1 = fetchSha1(NativeProps.CEF_DOWNLOAD_URL_MIN + ".sha1");
        var finalFile = targetDir.resolve(archiveFileName());
        var partFile = targetDir.resolve(archiveFileName() + PART_SUFFIX);

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-1 algorithm not available", e);
        }

        var url = URI.create(NativeProps.CEF_DOWNLOAD_URL_MIN).toURL();
        URLConnection connection = url.openConnection();
        var contentLength = connection.getContentLengthLong();

        try (var in = new DigestInputStream(connection.getInputStream(), digest);
                OutputStream out = Files.newOutputStream(partFile)) {
            var buffer = new byte[8192];
            long total = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                total += read;
                reportProgress(listener, Operation.DOWNLOAD_CEF,
                        contentLength > 0 ? (double) total / contentLength : 0.0);
            }
        } catch (IOException e) {
            Files.deleteIfExists(partFile);
            throw e;
        }

        var actualSha1 = toHex(digest.digest());
        if (!actualSha1.equalsIgnoreCase(expectedSha1)) {
            Files.deleteIfExists(partFile);
            throw new IOException("SHA-1 mismatch for downloaded CEF archive: expected " + expectedSha1
                    + " but got " + actualSha1);
        }

        try {
            Files.move(partFile, finalFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Files.move(partFile, finalFile, StandardCopyOption.REPLACE_EXISTING);
        }
        reportProgress(listener, Operation.DOWNLOAD_CEF, 1.0);
    }

    private static String fetchSha1(String url) throws IOException {
        try (InputStream in = URI.create(url).toURL().openStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
    }

    private static String toHex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }

    // -------------------------------------------------------------------------------------------
    // EXTRACT_CEF
    // -------------------------------------------------------------------------------------------

    private static void extractCef(Path targetDir, ProgressListener listener) throws IOException {
        var archiveFile = targetDir.resolve(archiveFileName());
        if (!Files.exists(archiveFile)) {
            throw new IOException("CEF archive not found at " + archiveFile + " - run DOWNLOAD_CEF first");
        }
        var destinationRoot = isMac() ? targetDir.resolve(FRAMEWORKS_DIR) : targetDir;
        Files.createDirectories(destinationRoot);
        var extractedFiles = new ArrayList<String>();
        var archiveSize = Files.size(archiveFile);
        var counter = new CountingInputStream(new BufferedInputStream(Files.newInputStream(archiveFile), 1 << 16));
        try (counter;
                var bzIn = new BZip2CompressorInputStream(counter);
                var tarIn = new TarArchiveInputStream(bzIn)) {
            TarArchiveEntry entry;
            var buffer = new byte[128 * 1024];
            while ((entry = tarIn.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                if (!tarIn.canReadEntryData(entry)) {
                    throw new IOException("Cannot read TAR entry: " + entry.getName());
                }
                var strippedName = stripToRuntimePayload(entry.getName());
                if (strippedName == null) {
                    continue;
                }
                var destination = destinationRoot.resolve(strippedName).normalize();
                if (!destination.startsWith(destinationRoot)) {
                    throw new IOException("Invalid TAR entry outside destination directory: " + entry.getName());
                }
                Files.createDirectories(destination.getParent());
                try (var out = Files.newOutputStream(destination, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING)) {
                    int read;
                    while ((read = tarIn.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                        reportProgress(listener, Operation.EXTRACT_CEF, archiveSize > 0
                                ? (double) counter.bytesRead() / archiveSize : 0.0);
                    }
                }
                var fileName = destination.getFileName().toString();
                if (!fileName.endsWith(".dll") && !fileName.endsWith(".plist")) {
                    destination.toFile().setExecutable(true);
                }
                extractedFiles.add(toRelativeString(targetDir, destination));
            }
        }

        writeMarker(targetDir.resolve(CEF_FILES_MARKER), extractedFiles);
        reportProgress(listener, Operation.EXTRACT_CEF, 1.0);
    }

    /**
     * Strips the outer distribution-name wrapper folder and the "Release"/"Resources" folder from a tar
     * entry name, so only actual runtime payload merges into the destination - everything else in the CEF
     * distribution archive (cmake/, include/, libcef_dll/, CMakeLists.txt, README.txt, bazel/, etc.) is
     * build-time material for compiling CEF-based projects from source and is not extracted.
     */
    private static String stripToRuntimePayload(String entryName) {
        var normalized = entryName.replace('\\', '/');
        var firstSlash = normalized.indexOf('/');
        if (firstSlash == -1) {
            return null;
        }
        var afterWrapper = normalized.substring(firstSlash + 1);
        if (afterWrapper.isEmpty()) {
            return null;
        }
        var secondSlash = afterWrapper.indexOf('/');
        var topFolder = secondSlash == -1 ? afterWrapper : afterWrapper.substring(0, secondSlash);
        if (!"Release".equals(topFolder) && !"Resources".equals(topFolder)) {
            return null;
        }
        if (secondSlash == -1 || secondSlash == afterWrapper.length() - 1) {
            return null;
        }
        return afterWrapper.substring(secondSlash + 1);
    }

    /**
     * Wraps an {@link InputStream}, tracking the number of bytes read so far - used to estimate
     * extraction progress from compressed-archive bytes consumed, since the tar format does not expose
     * a total entry count upfront.
     */
    private static final class CountingInputStream extends FilterInputStream {

        private long bytesRead;

        CountingInputStream(InputStream in) {
            super(in);
        }

        long bytesRead() {
            return bytesRead;
        }

        @Override
        public int read() throws IOException {
            var b = super.read();
            if (b != -1) {
                bytesRead++;
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            var n = super.read(b, off, len);
            if (n != -1) {
                bytesRead += n;
            }
            return n;
        }
    }

    // -------------------------------------------------------------------------------------------
    // EXTRACT_CEFFX
    // -------------------------------------------------------------------------------------------

    private static void extractCeffx(Path targetDir, ProgressListener listener) throws IOException {
        var extractedFiles = NativeExtractor.extract(targetDir,
                progress -> reportProgress(listener, Operation.EXTRACT_CEFFX, progress));
        var finalPaths = new ArrayList<String>();
        if (isMac()) {
            var frameworksDir = targetDir.resolve(FRAMEWORKS_DIR);
            var javaDir = targetDir.resolve(MACOS_LIB_DIR);
            Files.createDirectories(frameworksDir);
            Files.createDirectories(javaDir);
            var movedTopLevelDirs = new HashSet<String>();
            for (String relative : extractedFiles) {
                var firstSegment = firstSegment(relative);
                if (firstSegment.endsWith(".app") && movedTopLevelDirs.add(firstSegment)) {
                    var source = targetDir.resolve(firstSegment);
                    var destination = frameworksDir.resolve(firstSegment);
                    deleteRecursively(destination);
                    Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            for (String relative : extractedFiles) {
                var firstSegment = firstSegment(relative);
                if (movedTopLevelDirs.contains(firstSegment)) {
                    finalPaths.add(FRAMEWORKS_DIR + "/" + relative);
                } else {
                    var source = targetDir.resolve(relative);
                    var destination = javaDir.resolve(relative);
                    Files.createDirectories(destination.getParent());
                    Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
                    finalPaths.add(MACOS_LIB_DIR + "/" + relative);
                }
            }
        } else {
            finalPaths.addAll(extractedFiles);
        }
        writeMarker(targetDir.resolve(CEFFX_FILES_MARKER), finalPaths);
    }

    private static String firstSegment(String relativePath) {
        var slash = relativePath.indexOf('/');
        return slash == -1 ? relativePath : relativePath.substring(0, slash);
    }

    // -------------------------------------------------------------------------------------------
    // Shared helpers
    // -------------------------------------------------------------------------------------------

    private static boolean isMac() {
        return NativeProps.CEF_PLATFORM.startsWith("mac");
    }

    private static void reportProgress(ProgressListener listener, Operation operation, double progress) {
        if (listener != null) {
            listener.onProgress(operation, progress);
        }
    }

    private static String toRelativeString(Path base, Path path) {
        return base.relativize(path).toString().replace('\\', '/');
    }

    private static void writeMarker(Path markerFile, List<String> relativePaths) throws IOException {
        var content = String.join("\n", relativePaths);
        Files.writeString(markerFile, content, StandardCharsets.UTF_8);
    }

    private static List<String> readMarker(Path markerFile) throws IOException {
        if (!Files.exists(markerFile)) {
            return List.of();
        }
        var content = Files.readString(markerFile, StandardCharsets.UTF_8);
        if (content.isBlank()) {
            return List.of();
        }
        return List.of(content.split("\n"));
    }

    /**
     * Removes every file recorded by a previous extraction's marker, then removes the marker itself.
     */
    private static void undoExtraction(Path targetDir, String markerFileName) throws IOException {
        var markerFile = targetDir.resolve(markerFileName);
        for (String relative : readMarker(markerFile)) {
            deleteRecursively(targetDir.resolve(relative));
        }
        Files.deleteIfExists(markerFile);
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    // Best-effort cleanup - ignore individual failures (e.g. already removed).
                }
            });
        }
    }

    private NativeDeployer() {
        // empty
    }
}