/*
 * Copyright 2026 Pavel Castornii.
 *
 * Licensed under the BSD 3-Clause License. See LICENSE file for details.
 */

package com.techsenger.ceffx.demo.tab;

import com.techsenger.connectorfx.LocalConnector;
import com.techsenger.shellfx.core.ShellView;
import com.techsenger.shellfx.core.tab.AbstractTabView;
import com.techsenger.shellfx.devtools.DevToolsHostType;
import com.techsenger.shellfx.devtools.DevToolsTabDockParams;
import com.techsenger.shellfx.devtools.DevToolsTabDockView;
import com.techsenger.shellfx.devtools.DevToolsTabDockViewModel;
import com.techsenger.shellfx.material.icon.FontIconView;
import com.techsenger.shellfx.material.icon.PlainFontIcon;
import com.techsenger.shellfx.material.style.StyleClasses;
import com.techsenger.toolkit.fx.utils.NodeUtils;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 *
 * @author Pavel Castornii
 */
public class BrowserTabView<VM extends BrowserTabViewModel<?>> extends AbstractTabView<VM> {

    public class Composer extends AbstractTabView<VM>.Composer {

        private final BrowserTabView<VM> view = BrowserTabView.this;

        private DevToolsTabDockView<?> devTools;

        public void addDevTools() {
            if (devTools != null) {
                return;
            }
            var context = getShell().getViewModel().getContext();
            var connector = new LocalConnector(getShell().getStage(), null);
            var devToolsParams = new DevToolsTabDockParams(DevToolsHostType.OTHER, context.getSettings(),
                    context.getHistoryManager(), connector, getShell().getStage().hashCode());
            var devToolsViewModel = new DevToolsTabDockViewModel<>(devToolsParams);
            devTools = new DevToolsTabDockView<>(devToolsViewModel, getShell(), getShell()) {

                @Override
                protected void addHandlers() {
                    super.addHandlers();
                    getCloseButton().setOnAction(e -> view.getComposer().removeDevTools());
                }
            };
            devTools.initialize();
            getModifiableChildren().add(devTools);
            var splitPane = new SplitPane(content, devTools.getNode());
            splitPane.setOrientation(Orientation.VERTICAL);
            splitPane.setDividerPositions(0.75);
            getContentBox().getChildren().set(0, splitPane);
        }

        public void removeDevTools() {
            getModifiableChildren().remove(devTools);
            getContentBox().getChildren().set(0, content);
            devTools.getViewModel().requestDeinitializeTree();
            this.devTools = null;
        }
    }

    private final Button backButton = new Button(null, new FontIconView(new PlainFontIcon(0xF004D)));

    private final Button forwardButton = new Button(null, new FontIconView(new PlainFontIcon(0xF0054)));

    private final TextField addressTextField = new TextField();

    private final Button devToolsButton = new Button(null, new FontIconView(new PlainFontIcon(0xF1064)));

    private final ToolBar toolBar = new ToolBar(backButton, forwardButton, addressTextField, createMenuButton(),
            devToolsButton);

    private final VBox content = new VBox(toolBar);

    private final Pane browserPane;

    public BrowserTabView(VM viewModel, ShellView<?> shell, Pane browserPane) {
        super(viewModel, shell);
        this.browserPane = browserPane;
    }

    @Override
    public void requestFocus() {
        NodeUtils.requestFocus(addressTextField);
    }

    @Override
    public Composer getComposer() {
        return (Composer) super.getComposer();
    }

    @Override
    protected Composer createComposer() {
        return new BrowserTabView.Composer();
    }

    @Override
    protected void build() {
        super.build();
        toolBar.getStyleClass().addAll(StyleClasses.PROMINENT, StyleClasses.BLEND);
        forwardButton.getStyleClass().addAll(StyleClasses.SIZE_L);
        backButton.getStyleClass().addAll(StyleClasses.SIZE_L);
        devToolsButton.getStyleClass().addAll(StyleClasses.SIZE_L);
        VBox.setVgrow(browserPane, Priority.ALWAYS);
        HBox.setHgrow(addressTextField, Priority.ALWAYS);
        content.getChildren().add(browserPane);
        VBox.setVgrow(content, Priority.ALWAYS);
        getContentBox().getChildren().add(content);
    }

    @Override
    protected void bind() {
        super.bind();
        browserPane.cursorProperty().bind(getViewModel().cursorProperty());
    }

    @Override
    protected void addListeners() {
        super.addListeners();
        var viewModel = getViewModel();
        addressTextField.textProperty()
                .addListener((ov, oldV, newV) -> viewModel.onAddressChanged(newV, ChangeSource.VIEW));
        viewModel.addressSource().addListener((url) -> this.addressTextField.setText(url));
        viewModel.takeFocusSource().addListener((v) -> this.addressTextField.requestFocus());
    }

    @Override
    protected void addHandlers() {
        super.addHandlers();
        addressTextField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                getViewModel().onAddressSubmitted();
            }
        });
        forwardButton.setOnAction(e -> getViewModel().onForward());
        backButton.setOnAction(e -> getViewModel().onBack());
        devToolsButton.setOnAction(e -> getComposer().addDevTools());
    }

    private MenuButton createMenuButton() {
        var openDevToolsItem = new MenuItem("Open DevTools");
        openDevToolsItem.setOnAction(e -> getViewModel().onBrowserDevTools());
        var printItem = new MenuItem("Print");
        printItem.setOnAction(e -> getViewModel().onPrint());
        var resetZoomItem = new MenuItem("Reset Zoom");
        resetZoomItem.setOnAction(e -> getViewModel().onResetZoom());
        var dndZoomItem = new MenuItem("Drag And Drop");
        dndZoomItem.setOnAction(e -> getViewModel().onDragAndDrop());
        var themeItem = new MenuItem("Toggle Theme");
        themeItem.setOnAction(e -> getViewModel().onToggleTheme());
        MenuButton menuButton = new MenuButton("Tests", null, openDevToolsItem, printItem, resetZoomItem,
                dndZoomItem, themeItem);
        return menuButton;
    }
}
