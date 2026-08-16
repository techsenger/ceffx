/*
 * Copyright 2026 Pavel Castornii.
 *
 * Licensed under the BSD 3-Clause License. See LICENSE file for details.
 */

package com.techsenger.ceffx.demo;

import atlantafx.base.theme.Styles;
import com.techsenger.ceffx.core.CefApp;
import com.techsenger.ceffx.core.CefBrowserSettings;
import com.techsenger.ceffx.core.CefClient;
import com.techsenger.ceffx.core.CefSettings;
import com.techsenger.ceffx.core.browser.CefBrowser;
import com.techsenger.ceffx.core.browser.CefBrowserFactory;
import com.techsenger.ceffx.core.browser.CefFrame;
import com.techsenger.ceffx.core.callback.CefCommandLine;
import com.techsenger.ceffx.core.handler.CefAppHandlerAdapter;
import com.techsenger.ceffx.core.handler.CefCursorUtils;
import com.techsenger.ceffx.core.handler.CefDisplayHandlerAdapter;
import com.techsenger.ceffx.core.handler.CefFocusHandlerAdapter;
import com.techsenger.ceffx.core.handler.CefLifeSpanHandlerAdapter;
import com.techsenger.ceffx.core.handler.CefLoadHandlerAdapter;
import com.techsenger.ceffx.core.handler.CefPrintHandlerAdapter;
import com.techsenger.ceffx.core.misc.CefPrintSettings;
import com.techsenger.ceffx.core.network.CefRequest;
import com.techsenger.ceffx.demo.menu.BookmarkMenuRegistrar;
import com.techsenger.ceffx.demo.menu.FileMenuRegistrar;
import com.techsenger.ceffx.demo.tab.BrowserTabFxView;
import com.techsenger.ceffx.demo.tab.BrowserTabParams;
import com.techsenger.ceffx.demo.tab.BrowserTabPort;
import com.techsenger.ceffx.demo.tab.BrowserTabPresenter;
import com.techsenger.ceffx.demo.tab.ChangeSource;
import com.techsenger.ceffx.natives.NativeDeployer;
import com.techsenger.ceffx.natives.NativeProps;
import com.techsenger.shellfx.core.DefaultShellContext;
import com.techsenger.shellfx.core.DefaultShellFxView;
import com.techsenger.shellfx.core.DefaultShellParams;
import com.techsenger.shellfx.core.DefaultShellPresenter;
import com.techsenger.shellfx.core.ShellFxView;
import com.techsenger.shellfx.core.area.AreaParams;
import com.techsenger.shellfx.core.dialog.DialogParams;
import com.techsenger.shellfx.core.history.InMemoryHistoryManager;
import com.techsenger.shellfx.core.registry.ControlRegistry;
import com.techsenger.shellfx.core.settings.ShellSettings;
import com.techsenger.shellfx.core.tab.TabContainerFxView;
import com.techsenger.shellfx.core.window.WindowType;
import com.techsenger.shellfx.dialogs.alert.AlertDialogButtons;
import com.techsenger.shellfx.dialogs.alert.AlertDialogFxView;
import com.techsenger.shellfx.dialogs.alert.AlertDialogParams;
import com.techsenger.shellfx.dialogs.alert.AlertDialogPresenter;
import com.techsenger.shellfx.dialogs.alert.AlertDialogType;
import com.techsenger.shellfx.dialogs.progress.ProgressDialogFxView;
import com.techsenger.shellfx.dialogs.progress.ProgressDialogPresenter;
import com.techsenger.shellfx.icons.Fonts;
import com.techsenger.shellfx.icons.IconStylesheetFactory;
import com.techsenger.shellfx.layout.tabhost.TabHostFxView;
import com.techsenger.shellfx.layout.tabhost.TabHostPresenter;
import com.techsenger.shellfx.material.icon.FontIconView;
import com.techsenger.shellfx.material.icon.PlainFontIcon;
import com.techsenger.shellfx.material.style.Density;
import com.techsenger.shellfx.material.style.IconStylesheets;
import com.techsenger.shellfx.material.style.StyleClasses;
import com.techsenger.shellfx.material.style.Stylesheet;
import com.techsenger.shellfx.material.theme.AtlantaFxTheme;
import com.techsenger.tabpanepro.core.TabPanePro;
import com.techsenger.tabpanepro.core.skin.TabHeaderAreaPolicy;
import com.techsenger.tabpanepro.core.skin.TabPaneProSkin;
import com.techsenger.tabpanepro.core.skin.TabPaneProSkin.TabHeaderArea;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.BoundingBox;
import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demo application.
 *
 * <p>The closing process follows these steps:
 *
 * <p><b>If there are open tabs:</b>
 * <ol>
 *     <li>A window close is triggered.</li>
 *     <li>{@code shellPresenter.getOnCloseRequest()} is invoked.</li>
 *     <li>{@code shellPresenter.closeSafely()} is called.</li>
 *     <li>All tabs are deinitialized.</li>
 *     <li>Each tab performs {@code postDeinitialize()}, which calls {@code browser.close(boolean)}.</li>
 *     <li>{@code CefLifeSpanHandler.onBeforeClose()} is invoked.</li>
 *     <li>{@code CefApp.dispose()} is invoked.</li>
 * </ol>
 *
 * <p><b>If there are no open tabs:</b>
 * <ol>
 *     <li>A window close is triggered.</li>
 *     <li>{@code shellPresenter.getOnCloseRequest()} is invoked.</li>
 *     <li>{@code shellPresenter.closeSafely()} is called.</li>
 *     <li>{@code shellPresenter.getOnClosed()} is invoked.</li>
 *     <li>{@code CefApp.dispose()} is invoked.</li>
 * </ol>
 *
 * @author Pavel Castornii
 */
public class Demo extends Application {

    private static final Logger logger = LoggerFactory.getLogger(Demo.class);

    private static final Object TAB_COMPONENT = new Object();

    private static final Map<NativeDeployer.Operation, Double> OPERATION_WEIGHTS = Map.of(
            NativeDeployer.Operation.DOWNLOAD_CEF, 0.55,
            NativeDeployer.Operation.EXTRACT_CEF, 0.40,
            NativeDeployer.Operation.EXTRACT_CEFFX, 0.05
    );

    public static void main(String[] args) throws IOException {
        Application.launch(Demo.class);
    }

    private static CefSettings createCefSettings() {
        CefSettings settings = new CefSettings();
        settings.windowless_rendering_enabled = true;
        if (NativeProps.CEF_PLATFORM.startsWith("mac")) {
            settings.multi_threaded_message_loop = false;
            settings.external_message_pump = true;
        } else {
            settings.multi_threaded_message_loop = true;
            settings.external_message_pump = false;
        }
        settings.command_line_args_disabled = false;
        return settings;
    }

    private static ShellSettings createShellSettings() {
        var settings = new DemoSettings();
        var appearance = settings.getAppearance();
        appearance.setRegularFont(Font.font("System", 14));
        appearance.setMonospaceFont(Font.font("Monospace", 14));
        appearance.setTheme(AtlantaFxTheme.CUPERTINO_DARK);
        appearance.setDensity(Density.S);
        return settings;
    }

    /**
     * Returns the {@link BrowserTabPort} associated with {@code browser}, or {@code null} if none has been
     * set yet.
     *
     * <p>CEF client handler callbacks (e.g. {@code onLoadStart}, {@code onAddressChange},
     * {@code onCursorChange}) can fire on the CEF thread as soon as the browser is created, but
     * {@code TAB_COMPONENT} is only set later, from the JavaFX Application Thread via
     * {@link Platform#runLater}, once the tab's UI presenter has been constructed. This creates a window
     * where a handler may run before the property has been set. Callers must check the result for
     * {@code null} rather than assuming it is always populated by the time a handler fires.
     *
     * @param browser the browser to look up the tab port for
     * @return the associated {@link BrowserTabPort}, or {@code null} if not yet set
     */
    private static BrowserTabPort getTabPort(CefBrowser browser) {
        BrowserTabPort tabPort = (BrowserTabPort) browser.getProperties().get(TAB_COMPONENT);
        return tabPort;
    }

    private final AtomicInteger browserCount = new AtomicInteger();

    private ShellFxView<?> shell;

    private TabContainerFxView<?> workspace;

    private CefClient client;

    private volatile boolean exiting;

    @Override
    public void start(Stage stage) throws Exception {
        createShell(stage);
        createWorkspace();
        createMainMenu();
        var nativesPath = Paths.get(System.getProperty("ceffx.natives.path"));
        stage.setOnShown(e -> checkNatives(nativesPath));
        stage.show();
    }

    private void createShell(Stage stage) {
        FontIconView.setDefaultIconFont(Fonts.MATERIAL_DESIGN_ICONS.getFamily());
        IconStylesheets.addAll(IconStylesheetFactory.forAll());

        var stylesheets = List.of(new Stylesheet(Demo.class.getResource("demo.css")));
        var shellView = new DefaultShellFxView<>(this, stage, stylesheets, new ControlRegistry());
        this.shell = shellView;
        var settings = createShellSettings();
        var context = new DefaultShellContext(settings, new InMemoryHistoryManager(), getHostServices());
        var shellParams = new DefaultShellParams(context);
        var shellPresenter = new DefaultShellPresenter<>(shellView, shellParams);
        shellPresenter.initialize();
        shellView.getStage().getScene().getRoot().getStyleClass().add(StyleClasses.DENSITY_S);
        shellPresenter.setTitle("CEFFX Demo");
        shellPresenter.setOnCloseRequest(() -> {
            if (workspace.getComposer().getTabs().isEmpty()) {
                shellPresenter.setOnClosed(() -> {
                    CefApp.runLater(() -> CefApp.getInstance().dispose());
                });
            } else {
                exiting = true;
            }
            shellPresenter.closeSafely();
        });
    }

    private void createWorkspace() {
        var workspaceView = new TabHostFxView<>(true);
        this.workspace = workspaceView;
        TabPanePro tabPane = workspaceView.getNode();
        tabPane.setTabMaxWidth(220);
        TabPaneProSkin skin = (TabPaneProSkin) tabPane.getSkin();
        TabHeaderArea tabHeaderArea = skin.getTabHeaderArea();
        tabHeaderArea.setPolicy(TabHeaderAreaPolicy.ALWAYS_VISIBLE);
        StackPane stickyArea = tabHeaderArea.getStickyArea();
        stickyArea.setPadding(new Insets(2, 0, 0, 0));
        var newTabButton = new Button(null, new FontIconView(new PlainFontIcon(0xF0415)));
        newTabButton.getStyleClass().add(Styles.FLAT);
        newTabButton.setOnAction((e) -> onNewTab(null));
        stickyArea.getChildren().add(newTabButton);
        var workspacePresenter = new TabHostPresenter<>(workspaceView, new AreaParams());
        workspacePresenter.initialize();
        shell.getComposer().addWorkspace(workspaceView);
    }

    private void createMainMenu() {
        var controlRegistry = shell.getControlRegistry();
        var bookMarkRegistrar = new BookmarkMenuRegistrar(controlRegistry, shell, this::onNewTab);
        bookMarkRegistrar.register();
        var fileRegistrar = new FileMenuRegistrar(controlRegistry, shell);
        fileRegistrar.register();
        shell.upgradeMenuBar();
    }

    private void checkNatives(Path nativesPath) {
        try {
            var completedOperations = NativeDeployer.getStatus(nativesPath);
            if (completedOperations.size() < NativeDeployer.Operation.values().length) {
                var pendingOperations = EnumSet.allOf(NativeDeployer.Operation.class);
                pendingOperations.removeAll(completedOperations);
                var dialogView = new AlertDialogFxView<>();
                var dialogParams = new AlertDialogParams(WindowType.TOP_LEVEL,
                        shell.getPresenter().getContext().getSettings().getAppearance(),
                        AlertDialogType.CONFIRMATION);
                var dialogPresenter = new AlertDialogPresenter<>(dialogView, dialogParams);
                dialogPresenter.initialize();
                dialogPresenter.setMessage("Natives are not ready. Would you like to deploy them now?");
                dialogView.getStage().initOwner(shell.getStage());
                dialogView.getStage().show();
                dialogPresenter.setOnResult((button) -> {
                    dialogPresenter.closeSafely();
                    if (button == AlertDialogButtons.YES) {
                        deployNatives(nativesPath, pendingOperations);
                    }
                });
            } else {
                initCef(nativesPath);
            }
        } catch (Throwable t) {
            logger.error("Error checking natives", t);
        }
    }

    private void deployNatives(Path nativesPath, Set<NativeDeployer.Operation> pendingOperations) {
        try {
            var dialogView = new ProgressDialogFxView();
            var dialogParams = new DialogParams(WindowType.TOP_LEVEL,
                    shell.getPresenter().getContext().getSettings().getAppearance());
            var dialogPresenter = new ProgressDialogPresenter(dialogView, dialogParams);
            dialogPresenter.initialize();
            dialogPresenter.setTitle("Deploying Natives");
            dialogPresenter.setProgress(0);
            dialogView.getStage().initOwner(shell.getStage());
            dialogView.getStage().show();
            Thread.startVirtualThread(() -> {
                var lastOp = new AtomicReference<NativeDeployer.Operation>();
                var completedOps = EnumSet.noneOf(NativeDeployer.Operation.class);
                try {
                    NativeDeployer.deploy(nativesPath, (op, progress) -> {
                        Platform.runLater(() -> {
                            dialogPresenter.setMessage("Operation: " + op);
                            var previousOp = lastOp.getAndSet(op);
                            if (previousOp != op) {
                                if (previousOp != null) {
                                    completedOps.add(previousOp);
                                }
                            }
                            var totProgress = computeOverallProgress(pendingOperations, completedOps, op, progress);
                            dialogPresenter.setProgress(totProgress);
                        });
                    });
                    Thread.sleep(700);
                    Platform.runLater(() -> {
                        dialogPresenter.closeSafely();
                        initCef(nativesPath);
                    });
                } catch (Exception ex) {
                    logger.error("Error deploying natives", ex);
                    Platform.runLater(() -> dialogPresenter.closeSafely());
                }
            });
        } catch (Throwable t) {
            logger.error("Error deploying natives", t);
        }
    }

    private double computeOverallProgress(Set<NativeDeployer.Operation> pendingOperations,
            Set<NativeDeployer.Operation> completedOps, NativeDeployer.Operation currentOp, double currentOpProgress) {
        double completedWeight = 0;
        double totalWeight = 0;
        for (var op : pendingOperations) {
            var weight = OPERATION_WEIGHTS.get(op);
            totalWeight += weight;
            if (op == currentOp) {
                completedWeight += weight * currentOpProgress;
            } else if (completedOps.contains(op)) {
                completedWeight += weight;
            }
        }
        return totalWeight > 0 ? completedWeight / totalWeight : 1.0;
    }

    private void initCef(Path path) {
        try {
            var macFrameworkDir = path.resolve("Frameworks").resolve("Chromium Embedded Framework.framework");
            CefApp.startup(new String[]{ "--framework-dir-path=" + macFrameworkDir.toAbsolutePath()});
            CefApp.addAppHandler(new CefAppHandlerAdapter(null) {
                @Override
                public void onBeforeCommandLineProcessing(String processType, CefCommandLine commandLine) {
                    // commandLine.appendSwitchWithValue("force-dark-mode", "1");
                    commandLine.appendSwitchWithValue("enable-features", "WebUIDarkMode,ForceDarkMode");
                    if (NativeProps.CEF_PLATFORM.startsWith("mac")) {
                        commandLine.appendSwitchWithValue("framework-dir-path",
                                macFrameworkDir.toAbsolutePath().toString());
                        var mainBundlePath = path.resolve("Frameworks").resolve("ceffx Helper.app");
                        commandLine.appendSwitchWithValue("main-bundle-path",
                                mainBundlePath.toAbsolutePath().toString());
                    }
                }
            });

            var cefApp = CefApp.getInstance(createCefSettings());

            CefApp.runLater(() -> {
                // one client for all tabs
                client = CefApp.getInstance().createClient();

                client.addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
                    @Override
                    public void onBeforeClose(CefBrowser browser) {
                        if (browserCount.decrementAndGet() == 0 && exiting) {
                            CefApp.runLater(() -> CefApp.getInstance().dispose());
                        }
                    }
                });
                client.addDisplayHandler(new CefDisplayHandlerAdapter() {
                    @Override
                    public void onTitleChange(CefBrowser browser, String title) {
                        super.onTitleChange(browser, title);
                        var tabPort = getTabPort(browser);
                        if (tabPort != null) {
                            Platform.runLater(() -> tabPort.onTitleChanged(title));
                        }
                    }

                    @Override
                    public void onFaviconURLChange(CefBrowser browser, String[] iconUrls) {
                        super.onFaviconURLChange(browser, iconUrls);
                        var tabPort = getTabPort(browser);
                        if (tabPort != null) {
                            Platform.runLater(() -> tabPort.onFaviconUrlChanged(iconUrls));
                        }
                    }

                    @Override
                    public void onAddressChange(CefBrowser browser, CefFrame frame, String url) {
                        var tabPort = getTabPort(browser);
                        if (tabPort != null) {
                            Platform.runLater(() -> tabPort.onAddressChanged(url, ChangeSource.BROWSER));
                        }
                    }

                    @Override
                    public boolean onCursorChange(CefBrowser browser, int cursorType) {
                        var tabPort = getTabPort(browser);
                        if (tabPort != null) {
                            Platform.runLater(() -> tabPort.onCursorChanged(CefCursorUtils.getCursor(cursorType)));
                        }
                        return true;
                    }
                });
                client.addLoadHandler(new CefLoadHandlerAdapter() {
                    @Override
                    public void onLoadStart(CefBrowser browser, CefFrame frame,
                                            CefRequest.TransitionType transitionType) {
                        if (frame.isMain()) {
                            CefApp.runLater(() -> {
                                var devToolsClient = browser.getDevToolsClient();
                                if (devToolsClient != null) {
                                    devToolsClient.executeDevToolsMethod("Emulation.setAutoDarkModeOverride",
                                        "{ \"enabled\": true }"
                                    );
                                }
                            });
                        }
                    }
                });
                client.addFocusHandler(new CefFocusHandlerAdapter() {
                    @Override
                    public void onTakeFocus(CefBrowser browser, boolean next) {
                        super.onTakeFocus(browser, next);
                        var tabPort = getTabPort(browser);
                        if (tabPort != null) {
                            Platform.runLater(() -> tabPort.onTakeFocusFromBrowser());
                        }
                    }
                });
                client.addPrintHandler(new CefPrintHandlerAdapter() {
                    @Override
                    public void onPrintSettings(CefBrowser browser, CefPrintSettings settings, boolean getDefaults) {
                        // settings.setDeviceName(...);
                        settings.setPrinterPrintableArea(
                            new Dimension2D(300, 300),
                            new BoundingBox(0, 0, 300, 300),
                            false
                        );
                    }
                });
            });
        } catch (Throwable t) {
            logger.error("Error initializing CEF", t);
        }
    }

    private void onNewTab(String url) {
        CefApp.runLater(()-> {
            try {
                var browserSettings = new CefBrowserSettings();
                var browser = CefBrowserFactory.create(client, url, true, false, null, browserSettings);
                browser.setWindowlessFrameRate(60);
                browserCount.incrementAndGet();
                Platform.runLater(() -> {
                    var tabView = new BrowserTabFxView(shell, browser.getPane());
                    var tabParams = new BrowserTabParams(shell.getPresenter().getContext(), browser);
                    var tabPresenter = new BrowserTabPresenter(tabView, tabParams);
                    tabPresenter.initialize();
                    browser.getProperties().put(TAB_COMPONENT, tabPresenter);
                    workspace.getComposer().addTab(tabView);
                });
            } catch (Throwable ex) {
                logger.error("Error creating browser", ex);
            }
        });
    }
}
