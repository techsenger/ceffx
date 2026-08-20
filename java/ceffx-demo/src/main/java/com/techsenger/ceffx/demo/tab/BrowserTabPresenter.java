/*
 * Copyright 2026 Pavel Castornii.
 *
 * Licensed under the BSD 3-Clause License. See LICENSE file for details.
 */

package com.techsenger.ceffx.demo.tab;

import com.techsenger.ceffx.core.CefApp;
import com.techsenger.ceffx.core.browser.CefBrowserBase;
import com.techsenger.ceffx.demo.DemoComponents;
import com.techsenger.patternfx.mvp.ComponentDescriptor;
import com.techsenger.shellfx.core.CloseCheckResult;
import com.techsenger.shellfx.core.ClosePreparationResult;
import com.techsenger.shellfx.core.ShellContext;
import com.techsenger.shellfx.core.UiExecutor;
import com.techsenger.shellfx.core.tab.AbstractTabPresenter;
import com.techsenger.shellfx.material.icon.PlainFontIcon;
import com.techsenger.shellfx.material.icon.PlainImageIcon;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javafx.scene.Cursor;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Pavel Castornii
 */
public class BrowserTabPresenter extends AbstractTabPresenter<BrowserTabView> implements BrowserTabPort {

    private enum FavIconType {
        ICO, PNG
    }

    private static final Logger logger = LoggerFactory.getLogger(BrowserTabPresenter.class);

    private static String stripQuery(String url) {
        int q = url.indexOf('?');
        return q == -1 ? url : url.substring(0, q);
    }

    private static String getScheme(String url) {
        int idx = url.indexOf(':');

        if (idx <= 0) return null;

        String scheme = url.substring(0, idx);
        for (int i = 0; i < scheme.length(); i++) {
            char c = scheme.charAt(i);
            if (!Character.isLetter(c)) {
                return null;
            }
        }
        return scheme.toLowerCase();
    }

    private static String normalizeAddress(String address) {
        String url = address == null ? "" : address.trim();
        if (!url.isEmpty()) {
            String scheme = getScheme(url);
            if (scheme == null) {
                url = "https://" + url;
            }
        }
        return url;
    }

    private static String loadHtmlAsDataUrl(String fileName) throws IOException {
        String html;
        try (InputStream is = BrowserTabPresenter.class.getResourceAsStream(fileName)) {
            html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        String base64 = Base64.getEncoder().encodeToString(html.getBytes(StandardCharsets.UTF_8));
        var url = "data:text/html;base64," + base64;
        return url;
    }

    private final ShellContext shellContext;

    private final CefBrowserBase browser;

    private String address;

    private volatile String[] faviconUrls;

    private volatile boolean darkTheme = true;

    private Cursor cursor;

    public BrowserTabPresenter(BrowserTabView view, BrowserTabParams params) {
        super(view, params);
        this.shellContext = params.getContext();
        this.browser = params.getBrowser();
    }

    @Override
    public CloseCheckResult isReadyToClose() {
        return CloseCheckResult.READY;
    }

    @Override
    public void prepareToClose(Consumer<ClosePreparationResult> resultCallback) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void onTitleChanged(String title) {
        setTitle(title);
    }

    @Override
    public void onFaviconUrlChanged(String[] iconUrls) {
        // Cef calls onFaviconUrlChanged on resizing. If it is the same urls, just drop it
        if (this.faviconUrls != null && iconUrls != null && Arrays.equals(faviconUrls, iconUrls)) {
            return;
        }
        this.faviconUrls = iconUrls;
        if (iconUrls == null || iconUrls.length == 0) {
            setIcon(null);
            return;
        }
        record Icon(FavIconType type, String url) {}

        List<Map.Entry<FavIconType, Predicate<String>>> priorities = List.of(
            Map.entry(FavIconType.PNG, url -> url.contains("32x32")),
            Map.entry(FavIconType.PNG, url -> url.contains("48x48")),
            Map.entry(FavIconType.PNG, url -> url.contains("64x64")),
            Map.entry(FavIconType.PNG, url -> stripQuery(url).endsWith("/favicon.png")),
            Map.entry(FavIconType.PNG, url -> stripQuery(url).endsWith(".png")),
            Map.entry(FavIconType.ICO, url -> stripQuery(url).endsWith(".ico"))
        );

        Icon icon = priorities.stream()
            .flatMap(entry -> Arrays.asList(iconUrls).stream()
                .filter(entry.getValue())
                .map(url -> new Icon(entry.getKey(), url))
            )
            .findFirst()
            .orElse(null);

        if (icon == null) {
            logger.warn("{} No suitable favicon found in urls: {}", getDescriptor().getLogPrefix(), iconUrls);
            return;
        }

        logger.debug("{} Final favicon; type: {}, url: {}", getDescriptor().getLogPrefix(), icon.type(), icon.url());
        loadFavicon(icon.type(), icon.url());
    }

    @Override
    public void onCursorChanged(Cursor cursor) {
        setCursor(cursor);
    }

    public Cursor getCursor() {
        return this.cursor;
    }

    @Override
    public void onAddressChanged(String address, ChangeSource source) {
        if (source == ChangeSource.VIEW) {
            this.address = address;
        } else {
            setAddress(address);
        }
    }

    @Override
    public void onSelected(boolean selected) {
        super.onSelected(selected);
        CefApp.runLater(() -> {
            browser.setRenderingEnabled(selected);
            browser.setFocus(selected);
        });
    }

    @Override
    public void onTakeFocusFromBrowser() {
        getView().transferFocusFromBrowser();
    }

    @Override
    protected ComponentDescriptor createDescriptor() {
        return new ComponentDescriptor(DemoComponents.BROWSER_TAB);
    }

    protected void onAddressSubmitted() {
        if (this.address != null) {
            setIcon(null);
            setTitle(null);
            var address = normalizeAddress(this.address);
            setAddress(address);
            CefApp.runLater(() -> this.browser.loadURL(address));
        }
    }

    protected void openDevTools() {
        getView().getComposer().addDevTools(this.shellContext.getSettings(), this.shellContext.getHistoryManager());
    }

    protected void closeDevTools() {
        getView().getComposer().removeDevTools();
    }

    protected void onBrowserDevTools() {
        browser.openDevTools();
    }

    protected void onPrint() {
        CefApp.runLater(() -> browser.print());
    }

    protected void onBack() {
        CefApp.runLater(() -> this.browser.goBack());
    }

    protected void onForward() {
        CefApp.runLater(() -> this.browser.goForward());
    }

    protected void onResetZoom() {
        CefApp.runLater(() -> this.browser.setZoomLevel(0.0));
    }

    protected void onDragAndDrop() {
        try {
            var url = loadHtmlAsDataUrl("draganddrop.html");
            setAddress(url);
            CefApp.runLater(() -> browser.loadURL(url));
        } catch (Exception ex) {
            logger.error("{} Error reading test HTML file", getDescriptor().getLogPrefix(), ex);
        }
    }

    protected void onToggleTheme() {
        CefApp.runLater(() -> {
            var devToolsClient = browser.getDevToolsClient();
            if (devToolsClient != null) {
                this.darkTheme = !this.darkTheme;
                String params = String.format("{ \"enabled\": %s }", darkTheme);
                devToolsClient.executeDevToolsMethod("Emulation.setAutoDarkModeOverride", params);
            }
        });
    }

    @Override
    protected void postInitialize() {
        super.postInitialize();
        setTitle("New Tab");
        setIcon(new PlainFontIcon(984479));
    }

    @Override
    protected void postDeinitialize() {
        super.postDeinitialize();
        CefApp.runLater(() -> browser.close(true));
    }

    protected void setAddress(String url) {
        if (Objects.equals(this.address, url)) {
            return;
        }
        this.address = url;
        getView().updateAddress(url);
    }

    protected void setCursor(Cursor cursor) {
        if (Objects.equals(this.cursor, cursor)) {
            return;
        }
        this.cursor = cursor;
        getView().updateCursor(cursor);
    }

    private void loadFavicon(FavIconType iconType, String iconUrl) {
        var location = browser.getMainFrame().getURL();
        Thread.startVirtualThread(() -> {
            try {
                Image image = null;
                if (iconUrl != null) {
                    if (iconType == FavIconType.PNG) {
                        image = FaviconLoader.loadPng(iconUrl);
                    } else {
                        image = FaviconLoader.loadIco(iconUrl);
                    }
                } else {
                    image = FaviconLoader.resolveAndLoad(new URI(location).toURL(), 32);
                }
                if (image != null) {
                    logger.debug("{} Loaded favicon from {} for {}", getDescriptor().getLogPrefix(),
                            iconUrl == null ? "resolved url" : iconUrl, location);
                    if (browser.getMainFrame().getURL().equals(location)) { // it hasn't changed)
                        var icon = new PlainImageIcon(image);
                        UiExecutor.execute(() -> setIcon(icon));
                    }
                }
            } catch (Exception ex) {
                logger.error("{} Error loading favicon", getDescriptor().getLogPrefix(),  ex);
            }
        });
    }
}
