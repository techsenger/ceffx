// Copyright (c) 2013 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package com.techsenger.ceffx.core;

import com.techsenger.ceffx.core.callback.CefSchemeHandlerFactory;
import com.techsenger.ceffx.core.handler.CefAppHandler;
import com.techsenger.ceffx.core.handler.CefAppHandlerAdapter;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exposes static methods for managing the global CEF context.
 */
public class CefApp extends CefAppHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(CefApp.class);

    /**
     * The CEF thread. Scheduling-capable so that it can also serve delayed message loop pumps
     * requested via CefAppHandler.onScheduleMessagePumpWork (see doMessageLoopWork), keeping CEFFX
     * to its documented two threads: the JavaFX Application Thread and this one.
     */
    private static class ExecutorHolder {
        static final ScheduledExecutorService INSTANCE = newExecutor();

        private static ScheduledExecutorService newExecutor() {
            ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, r -> {
                Thread t = new Thread(r, "cef-main-thread");
                t.setDaemon(false);
                t.setUncaughtExceptionHandler((th, ex) -> logger.error("Error on Cef Thread", ex));
                return t;
            });
            // A delayed pump left over at shutdown would call into CEF after N_Shutdown. Drop such
            // tasks instead of running them (the default policy is to run them).
            executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
            return executor;
        }
    }

    /**
     * Submits {@code r} to run on the CEF thread.
     *
     * <p>{@code r} is wrapped in its own try/catch before being submitted. Without this, any exception
     * {@code r} throws would be silently lost: {@link ExecutorService#submit} wraps the task in a
     * {@link java.util.concurrent.FutureTask}, which catches any {@link Throwable} internally and stores
     * it as the task's outcome rather than letting it propagate out of the thread - so the CEF thread's
     * uncaught exception handler never sees it, and the only other way to observe it would be to read the
     * returned {@link java.util.concurrent.Future} via {@code get()}, which this method does not do. The
     * inner try/catch logs the exception itself instead, so failures inside {@code r} are not lost.
     *
     * @param r the task to run on the CEF thread
     */
    public static void runLater(Runnable r) {
        try {
            ExecutorHolder.INSTANCE.submit(() -> {
                try {
                    r.run();
                } catch (Throwable t) {
                    logger.error("Error on CEF thread in runLater task", t);
                }
            });
        } catch (RejectedExecutionException e) {
            // The CEF thread has been shut down by dispose(). Callers keep arriving after that -
            // notably CefBrowserOsr's mouse handlers, because JavaFX still delivers enter/exit
            // events while the window tears down - and an unguarded submit() throws
            // RejectedExecutionException straight onto the JavaFX Application Thread. There is
            // nothing useful left to do with the task, so drop it.
            logger.debug("CEF thread already shut down; dropping task", e);
        }
    }

    public final class CefVersion {
        public final int CEFFX_COMMIT_NUMBER;

        public final int CEF_VERSION_MAJOR;
        public final int CEF_VERSION_MINOR;
        public final int CEF_VERSION_PATCH;
        public final int CEF_COMMIT_NUMBER;

        public final int CHROME_VERSION_MAJOR;
        public final int CHROME_VERSION_MINOR;
        public final int CHROME_VERSION_BUILD;
        public final int CHROME_VERSION_PATCH;

        private CefVersion(int cefFxCommitNo, int cefMajor, int cefMinor, int cefPatch,
                int cefCommitNo, int chrMajor, int chrMin, int chrBuild, int chrPatch) {
            CEFFX_COMMIT_NUMBER = cefFxCommitNo;

            CEF_VERSION_MAJOR = cefMajor;
            CEF_VERSION_MINOR = cefMinor;
            CEF_VERSION_PATCH = cefPatch;
            CEF_COMMIT_NUMBER = cefCommitNo;

            CHROME_VERSION_MAJOR = chrMajor;
            CHROME_VERSION_MINOR = chrMin;
            CHROME_VERSION_BUILD = chrBuild;
            CHROME_VERSION_PATCH = chrPatch;
        }

        public String getCefFXVersion() {
            return CEF_VERSION_MAJOR + "." + CEF_VERSION_MINOR + "." + CEF_VERSION_PATCH + "."
                    + CEFFX_COMMIT_NUMBER;
        }

        public String getCefVersion() {
            return CEF_VERSION_MAJOR + "." + CEF_VERSION_MINOR + "." + CEF_VERSION_PATCH;
        }

        public String getChromeVersion() {
            return CHROME_VERSION_MAJOR + "." + CHROME_VERSION_MINOR + "." + CHROME_VERSION_BUILD
                    + "." + CHROME_VERSION_PATCH;
        }

        @Override
        public String toString() {
            return "CEFFX Version = " + getCefFXVersion() + "\n"
                    + "CEF Version = " + getCefVersion() + "\n"
                    + "Chromium Version = " + getChromeVersion();
        }
    }

    /**
     * The CefAppState gives you a hint if the CefApp is already usable or not
     * usable any more. See values for details.
     */
    public enum CefAppState {
        /**
         * No CefApp instance was created yet. Call getInstance() to create a new
         * one.
         */
        NONE,

        /**
         * CefApp is new created but not initialized yet. No CefClient and no
         * CefBrowser was created until now.
         */
        NEW,

        /**
         * CefApp is in its initializing process. Please wait until initializing is
         * finished.
         */
        INITIALIZING,

        /**
         * CefApp is up and running. At least one CefClient was created and the
         * message loop is running. You can use all classes and methods of CEFFX now.
         */
        INITIALIZED,

        /**
         * CEF initialization has failed (for example due to a second process using
         * the same root_cache_path).
         */
        INITIALIZATION_FAILED,

        /**
         * CefApp is in its shutdown process. All CefClients and CefBrowser
         * instances will be disposed. No new CefClient or CefBrowser is allowed to
         * be created. The message loop will be performed until all CefClients and
         * all CefBrowsers are disposed completely.
         */
        SHUTTING_DOWN,

        /**
         * CefApp is terminated and can't be used any more. You can shutdown the
         * application safely now.
         */
        TERMINATED
    }

    /** 30fps floor, matching kMaxTimerDelay in CEF's reference external pump. */
    private static final long MAX_PUMP_DELAY_MS = 1000 / 30;

    /**
     * According the singleton pattern, this attribute keeps
     * one single object of this class.
     */
    private static CefApp self = null;
    private static CefAppHandler appHandler_ = null;
    private static CefAppState state_ = CefAppState.NONE;
    private final Object pumpLock = new Object();
    private ScheduledFuture<?> pendingPump = null;
    private HashSet<CefClient> clients_ = new HashSet<CefClient>();
    private CefSettings settings_ = null;

    /**
     * To get an instance of this class, use the method
     * getInstance() instead of this CTOR.
     *
     * The CTOR is called by getInstance() as needed and
     * loads all required CEFFX libraries.
     *
     * @throws UnsatisfiedLinkError
     */
    private CefApp(String[] args, CefSettings settings) throws UnsatisfiedLinkError {
        super(args);
        if (settings != null) settings_ = settings.clone();
        if (OS.isWindows()) {
            SystemBootstrap.loadLibrary("chrome_elf");
            SystemBootstrap.loadLibrary("libcef");

            // Other platforms load this library in CefApp.startup().
            SystemBootstrap.loadLibrary("ceffx");
        } else if (OS.isLinux()) {
            SystemBootstrap.loadLibrary("cef");
        }
        if (appHandler_ == null) {
            appHandler_ = this;
        }

        try {
            Runnable r = () -> {
                // Perform native pre-initialization.
                if (!N_PreInitialize()) {
                    throw new IllegalStateException("Failed to pre-initialize native code");
                }
            };
            r.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Assign an AppHandler to CefApp. The AppHandler can be used to evaluate
     * application arguments, to register your own schemes and to hook into the
     * shutdown sequence. See CefAppHandler for more details.
     *
     * This method must be called before CefApp is initialized. CefApp will be
     * initialized automatically if you call createClient() the first time.
     * @param appHandler An instance of CefAppHandler.
     *
     * @throws IllegalStateException in case of CefApp is already initialized
     */
    public static void addAppHandler(CefAppHandler appHandler) throws IllegalStateException {
        if (getState().compareTo(CefAppState.NEW) > 0)
            throw new IllegalStateException("Must be called before CefApp is initialized");
        appHandler_ = appHandler;
    }

    /**
     * Get an instance of this class.
     * @return an instance of this class
     * @throws UnsatisfiedLinkError
     */
    public static synchronized CefApp getInstance() throws UnsatisfiedLinkError {
        return getInstance(null, null);
    }

    public static synchronized CefApp getInstance(String[] args) throws UnsatisfiedLinkError {
        return getInstance(args, null);
    }

    public static synchronized CefApp getInstance(CefSettings settings)
            throws UnsatisfiedLinkError {
        return getInstance(null, settings);
    }

    public static synchronized CefApp getInstance(String[] args, CefSettings settings)
            throws UnsatisfiedLinkError {
        if (settings != null) {
            if (getState() != CefAppState.NONE && getState() != CefAppState.NEW)
                throw new IllegalStateException("Settings can only be passed to CEF"
                        + " before createClient is called the first time.");
        }
        if (self == null) {
            if (getState() == CefAppState.TERMINATED)
                throw new IllegalStateException("CefApp was terminated");
            self = new CefApp(args, settings);
            setState(CefAppState.NEW);
        }
        return self;
    }

    public final void setSettings(CefSettings settings) throws IllegalStateException {
        if (getState() != CefAppState.NONE && getState() != CefAppState.NEW)
            throw new IllegalStateException("Settings can only be passed to CEF"
                    + " before createClient is called the first time.");
        settings_ = settings.clone();
    }

    public final CefVersion getVersion() {
        try {
            return N_GetVersion();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the current state of CefApp.
     * @return current state.
     */
    public final static CefAppState getState() {
        synchronized (state_) {
            return state_;
        }
    }

    private static final void setState(final CefAppState state) {
        synchronized (state_) {
            state_ = state;
        }
        // Execute on the AWT event dispatching thread.
        CefApp.runLater(new Runnable() {
            @Override
            public void run() {
                if (appHandler_ != null) appHandler_.stateHasChanged(state);
            }
        });
    }

    /**
     * To shutdown the system, it's important to call the dispose
     * method. Calling this method closes all client instances with
     * and all browser instances each client owns. After that the
     * message loop is terminated and CEF is shutdown.
     */
    public synchronized final void dispose() {
        switch (getState()) {
            case NEW:
                // Nothing to do inspite of invalidating the state
                setState(CefAppState.TERMINATED);
                break;

            case INITIALIZING:
            case INITIALIZED:
                // (3) Shutdown sequence. Close all clients and continue.
                setState(CefAppState.SHUTTING_DOWN);
                if (clients_.isEmpty()) {
                    shutdown();
                } else {
                    // shutdown() will be called from clientWasDisposed() when the last
                    // client is gone.
                    // Use a copy of the HashSet to avoid iterating during modification.
                    HashSet<CefClient> clients = new HashSet<CefClient>(clients_);
                    for (CefClient c : clients) {
                        c.dispose();
                    }
                }
                break;

            case NONE:
            case SHUTTING_DOWN:
            case TERMINATED:
                // Ignore shutdown, CefApp is already terminated, in shutdown progress
                // or was never created (shouldn't be possible)
                break;
        }
    }

    /**
     * Creates a new client instance and returns it to the caller.
     * One client instance is responsible for one to many browser
     * instances
     * @return a new client instance
     */
    public synchronized CefClient createClient() {
        switch (getState()) {
            case NEW:
                setState(CefAppState.INITIALIZING);
                initialize();
                // FALL THRU

            case INITIALIZING:
            case INITIALIZED:
                CefClient client = new CefClient();
                clients_.add(client);
                return client;

            default:
                throw new IllegalStateException("Can't crate client in state " + state_);
        }
    }

    /**
     * Register a scheme handler factory for the specified |scheme_name| and
     * optional |domain_name|. An empty |domain_name| value for a standard scheme
     * will cause the factory to match all domain names. The |domain_name| value
     * will be ignored for non-standard schemes. If |scheme_name| is a built-in
     * scheme and no handler is returned by |factory| then the built-in scheme
     * handler factory will be called. If |scheme_name| is a custom scheme then
     * also implement the CefApp::OnRegisterCustomSchemes() method in all
     * processes. This function may be called multiple times to change or remove
     * the factory that matches the specified |scheme_name| and optional
     * |domain_name|. Returns false if an error occurs. This function may be
     * called on any thread in the browser process.
     */
    public boolean registerSchemeHandlerFactory(
            String schemeName, String domainName, CefSchemeHandlerFactory factory) {
        try {
            return N_RegisterSchemeHandlerFactory(schemeName, domainName, factory);
        } catch (Exception err) {
            err.printStackTrace();
        }
        return false;
    }

    /**
     * Clear all registered scheme handler factories. Returns false on error. This
     * function may be called on any thread in the browser process.
     */
    public boolean clearSchemeHandlerFactories() {
        try {
            return N_ClearSchemeHandlerFactories();
        } catch (Exception err) {
            err.printStackTrace();
        }
        return false;
    }

    /**
     * This method is called by a CefClient if it was disposed. This causes
     * CefApp to clean up its list of available client instances. If all clients
     * are disposed, CefApp will be shutdown.
     * @param client the disposed client.
     */
    protected final synchronized void clientWasDisposed(CefClient client) {
        clients_.remove(client);
        if (clients_.isEmpty() && getState().compareTo(CefAppState.SHUTTING_DOWN) >= 0) {
            // Shutdown native system.
            shutdown();
        }
    }

    /**
     * Initialize the context.
     * @return true on success.
     */
    private final void initialize() {
        try {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    String library_path = getCefFXLibPath();

                    CefSettings settings = settings_ != null ? settings_ : new CefSettings();

                    // Avoid to override user values by testing on NULL
                    if (OS.isMacintosh()) {
                        if (settings.browser_subprocess_path == null) {
                            Path path = Paths.get(library_path,
                                    "../Frameworks/ceffx Helper.app/Contents/MacOS/ceffx Helper");
                            settings.browser_subprocess_path =
                                    path.normalize().toAbsolutePath().toString();
                        }
                    } else if (OS.isWindows()) {
                        if (settings.browser_subprocess_path == null) {
                            Path path = Paths.get(library_path, "ceffx_helper.exe");
                            settings.browser_subprocess_path =
                                    path.normalize().toAbsolutePath().toString();
                        }
                    } else if (OS.isLinux()) {
                        if (settings.browser_subprocess_path == null) {
                            Path path = Paths.get(library_path, "ceffx_helper");
                            settings.browser_subprocess_path =
                                    path.normalize().toAbsolutePath().toString();
                        }
                        if (settings.resources_dir_path == null) {
                            Path path = Paths.get(library_path);
                            settings.resources_dir_path =
                                    path.normalize().toAbsolutePath().toString();
                        }
                        if (settings.locales_dir_path == null) {
                            Path path = Paths.get(library_path, "locales");
                            settings.locales_dir_path =
                                    path.normalize().toAbsolutePath().toString();
                        }
                    }

                    if (N_Initialize(appHandler_, settings)) {
                        setState(CefAppState.INITIALIZED);
                    } else {
                        setState(CefAppState.INITIALIZATION_FAILED);
                    }
                }
            };
            r.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method is invoked by the native code (currently on Mac only) in case
     * of a termination event (e.g. someone pressed CMD+Q).
     */
    protected final void handleBeforeTerminate() {
        CefApp.runLater(new Runnable() {
            @Override
            public void run() {
                CefAppHandler handler =
                        (CefAppHandler) ((appHandler_ == null) ? this : appHandler_);
                if (!handler.onBeforeTerminate()) dispose();
            }
        });
    }

    /**
     * Shut down the context.
     */
    private final void shutdown() {
        CefApp.runLater(new Runnable() {
            @Override
            public void run() {
                // Shutdown native CEF.
                N_Shutdown();

                synchronized (pumpLock) {
                    if (pendingPump != null) {
                        pendingPump.cancel(false);
                        pendingPump = null;
                    }
                }
                ExecutorHolder.INSTANCE.shutdown();
                setState(CefAppState.TERMINATED);
                CefApp.self = null;
            }
        });
    }

    /**
     * Schedule a single message loop iteration. Used on all platforms except Windows with windowed
     * rendering, and required on macOS, where CEF does not support multi_threaded_message_loop and
     * so cannot run a loop of its own.
     *
     * <p>This is the Java end of CefBrowserProcessHandler::OnScheduleMessagePumpWork: CEF asks for
     * CefDoMessageLoopWork() to be called in {@code delay_ms}. A request from CEF always takes
     * precedence, replacing any pump already scheduled, so that work CEF wants done sooner is never
     * held back by one scheduled for later.
     *
     * <p>The pump is scheduled on the CEF thread, which is where all CEF calls must be made.
     *
     * @param delay_ms delay requested by CEF, in milliseconds; may be zero or negative for "now"
     */
    public final void doMessageLoopWork(final long delay_ms) {
        if (getState() == CefAppState.TERMINATED) {
            return;
        }
        // Cap the delay, exactly as CEF's own reference pump does
        // (tests/shared/browser/main_message_loop_external_pump.cc: kMaxTimerDelay = 1000/30).
        final long delay = delay_ms <= 0 ? 0 : Math.min(delay_ms, MAX_PUMP_DELAY_MS);
        synchronized (pumpLock) {
            if (pendingPump != null) {
                pendingPump.cancel(false);
            }
            schedulePump(delay);
        }
    }

    /** Schedules a pump on the CEF thread. Caller must hold pumpLock. */
    private void schedulePump(long delay) {
        try {
            pendingPump = ExecutorHolder.INSTANCE.schedule(this::pump, delay, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            // Executor already shut down (teardown raced with a pump request) — nothing to do.
            pendingPump = null;
        }
    }

    /**
     * Runs on the CEF thread.
     */
    private void pump() {
        synchronized (pumpLock) {
            pendingPump = null;
        }
        if (getState() == CefAppState.TERMINATED) {
            return;
        }
        try {
            N_DoMessageLoopWork();
        } catch (Throwable t) {
            logger.error("Error on CEF thread in message loop", t);
        }
        // Re-arm, so the loop is self-sustaining rather than purely reactive: a pump that runs only
        // when CEF asks stalls the moment CEF stops asking, which on macOS left the page blank and
        // the close handshake unable to complete.
        //
        // Do not disturb a pump that CEF requested during the work above, though. CEF's reference
        // pump draws the same distinction, marking its own re-arm with kTimerDelayPlaceholder and
        // returning early from OnScheduleWork when a timer is already pending. Without this, a
        // request for immediate work made inside N_DoMessageLoopWork would be pushed back out to the
        // 33ms floor by the re-arm that follows it.
        synchronized (pumpLock) {
            if (pendingPump == null && getState() != CefAppState.TERMINATED) {
                schedulePump(MAX_PUMP_DELAY_MS);
            }
        }
    }

    /**
     * This method must be called at the beginning of the main() method to perform platform-
     * specific startup initialization. On Linux this initializes Xlib multithreading and on
     * macOS this dynamically loads the CEF framework.
     * @param args Command-line arguments massed to main().
     * @return True on successful startup.
     */
    public static final boolean startup(String[] args) {
        if (OS.isLinux() || OS.isMacintosh()) {
            SystemBootstrap.loadLibrary("ceffx");
            return N_Startup(OS.isMacintosh() ? getCefFrameworkPath(args) : null);
        }
        return true;
    }

    /**
     * Get the path which contains the CEFFX library
     * @return The path to the CEFFX library
     */
    private static final String getCefFXLibPath() {
        String library_path = System.getProperty("java.library.path");
        String[] paths = library_path.split(System.getProperty("path.separator"));
        for (String path : paths) {
            File dir = new File(path);
            String[] found = dir.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return (name.equalsIgnoreCase("libceffx.dylib")
                            || name.equalsIgnoreCase("libceffx.so")
                            || name.equalsIgnoreCase("ceffx.dll"));
                }
            });
            if (found != null && found.length != 0) return path;
        }
        return library_path;
    }

    /**
     * Get the path that contains the CEF Framework on macOS.
     * @return The path to the CEF Framework.
     */
    private static final String getCefFrameworkPath(String[] args) {
        // Check for the path on the command-line.
        String switchPrefix = "--framework-dir-path=";
        for (String arg : args) {
            if (arg.startsWith(switchPrefix)) {
                return new File(arg.substring(switchPrefix.length())).getAbsolutePath();
            }
        }

        // Determine the path relative to the CEFFX lib location in the app bundle.
        return new File(getCefFXLibPath() + "/../Frameworks/Chromium Embedded Framework.framework")
                .getAbsolutePath();
    }

    private final static native boolean N_Startup(String pathToCefFramework);
    private final native boolean N_PreInitialize();
    private final native boolean N_Initialize(CefAppHandler appHandler, CefSettings settings);
    private final native void N_Shutdown();
    private final native void N_DoMessageLoopWork();
    private final native CefVersion N_GetVersion();
    private final native boolean N_RegisterSchemeHandlerFactory(
            String schemeName, String domainName, CefSchemeHandlerFactory factory);
    private final native boolean N_ClearSchemeHandlerFactories();
}
