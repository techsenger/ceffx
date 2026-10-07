/*
 * Copyright 2026 Pavel Castornii.
 *
 * Licensed under the BSD 3-Clause License. See LICENSE file for details.
 */

package com.techsenger.ceffx.demo;

import com.techsenger.shellfx.core.ShellView;
import com.techsenger.shellfx.core.registry.AbstractControlRegistrar;
import com.techsenger.shellfx.core.registry.SimpleControlProvider;
import com.techsenger.shellfx.core.registry.SimpleGroupProvider;
import com.techsenger.shellfx.material.slot.GroupSlot;
import java.util.List;
import java.util.stream.IntStream;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;

/**
 * Registers the control of every menu and item the demo application contributes; where the menus and groups sit
 * is set by {@link ModuleSlotRegistrar}.
 *
 * @author Pavel Castornii
 */
public class ModuleControlRegistrar extends AbstractControlRegistrar {

    private static record Bookmark(String title, String url) { }

    private static final List<Bookmark> cefBookmarks = List.of(
            new Bookmark("CEFFX", "https://github.com/techsenger/ceffx"),
            new Bookmark("Java CEF", "https://github.com/chromiumembedded/java-cef"),
            new Bookmark("CEF", "https://github.com/chromiumembedded/cef"));

    private static final List<Bookmark> popularBookmarks = List.of(
            new Bookmark("Google", "http://google.com/"),
            new Bookmark("YouTube", "https://www.youtube.com/"),
            new Bookmark("GitHub", "https://github.com/"));

    private final ShellView<?> shell;

    private final TabOpener tabOpener;

    public ModuleControlRegistrar(ShellView<?> shell, TabOpener tabOpener) {
        super(shell.getContext().getControlRegistry());
        this.shell = shell;
        this.tabOpener = tabOpener;
    }

    @Override
    public void register() {
        registerMainMenu();
        registerFileMenu();
        registerExitItem();
        registerBookmarkMenu();
        IntStream
                .range(0, cefBookmarks.size())
                .forEach(i -> registerBookmarkItem(cefBookmarks.get(i), Slots.BookmarkMenu.CEF_GROUP, i));
        IntStream
                .range(0, popularBookmarks.size())
                .forEach(i -> registerBookmarkItem(popularBookmarks.get(i), Slots.BookmarkMenu.POPULAR_GROUP, i));
    }

    private void registerMainMenu() {
        register(Slots.MAIN_MENU, () -> new SimpleControlProvider<>(new MenuBar()));
    }

    private void registerFileMenu() {
        register(Slots.FileMenu.MENU, () -> new SimpleControlProvider<>(new Menu("_File")));
        register(Slots.FileMenu.GROUP, () -> new SimpleGroupProvider<>());
    }

    private void registerExitItem() {
        register(Slots.FileMenu.GROUP, 1000, () -> new SimpleControlProvider<>(new MenuItem("E_xit")) {
            @Override
            public void initialize(ShellView<?> view) {
                super.initialize(view);
                getControl().setOnAction(e -> shell.getViewModel().getOnCloseRequest().run());
            }
        });
    }

    private void registerBookmarkMenu() {
        register(Slots.BookmarkMenu.MENU, () -> new SimpleControlProvider<>(new Menu("_Bookmarks")));
        register(Slots.BookmarkMenu.CEF_GROUP, () -> new SimpleGroupProvider<>());
        register(Slots.BookmarkMenu.POPULAR_GROUP, () -> new SimpleGroupProvider<>());
    }

    private void registerBookmarkItem(Bookmark bookmark, GroupSlot<ShellView<?>, MenuItem> group, int pos) {
        register(group, pos, () -> new SimpleControlProvider<>(new MenuItem(bookmark.title)) {
            @Override
            public void initialize(ShellView<?> view) {
                super.initialize(view);
                getControl().setOnAction(e -> tabOpener.open(bookmark.url));
            }
        });
    }
}
