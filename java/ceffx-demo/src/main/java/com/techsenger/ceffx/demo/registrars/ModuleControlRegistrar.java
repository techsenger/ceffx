/*
 * Copyright 2026 Pavel Castornii.
 *
 * Licensed under the BSD 3-Clause License. See LICENSE file for details.
 */

package com.techsenger.ceffx.demo.registrars;

import com.techsenger.ceffx.demo.TabOpener;
import com.techsenger.ceffx.demo.slots.ShellSlots;
import com.techsenger.shellfx.core.ShellView;
import com.techsenger.shellfx.core.registrars.AbstractControlRegistrar;
import com.techsenger.shellfx.core.registries.SimpleControlProvider;
import com.techsenger.shellfx.core.registries.SimpleGroupProvider;
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
                .forEach(i -> registerBookmarkItem(cefBookmarks.get(i), ShellSlots.BookmarkMenu.CEF_GROUP, i));
        IntStream
                .range(0, popularBookmarks.size())
                .forEach(i -> registerBookmarkItem(popularBookmarks.get(i), ShellSlots.BookmarkMenu.POPULAR_GROUP, i));
    }

    private void registerMainMenu() {
        register(ShellSlots.MAIN_MENU, () -> new SimpleControlProvider<>(new MenuBar()));
    }

    private void registerFileMenu() {
        register(ShellSlots.FileMenu.MENU, () -> new SimpleControlProvider<>(new Menu("_File")));
        register(ShellSlots.FileMenu.GROUP, () -> new SimpleGroupProvider<>());
    }

    private void registerExitItem() {
        register(ShellSlots.FileMenu.GROUP, 1000, () -> new SimpleControlProvider<>(new MenuItem("E_xit")) {
            @Override
            public void initialize(ShellView<?> view) {
                super.initialize(view);
                getControl().setOnAction(e -> shell.getViewModel().getOnCloseRequest().run());
            }
        });
    }

    private void registerBookmarkMenu() {
        register(ShellSlots.BookmarkMenu.MENU, () -> new SimpleControlProvider<>(new Menu("_Bookmarks")));
        register(ShellSlots.BookmarkMenu.CEF_GROUP, () -> new SimpleGroupProvider<>());
        register(ShellSlots.BookmarkMenu.POPULAR_GROUP, () -> new SimpleGroupProvider<>());
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
