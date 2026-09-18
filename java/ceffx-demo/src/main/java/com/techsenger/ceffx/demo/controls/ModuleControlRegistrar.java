/*
 * Copyright 2026 Pavel Castornii.
 *
 * Licensed under the BSD 3-Clause License. See LICENSE file for details.
 */

package com.techsenger.ceffx.demo.controls;

import com.techsenger.ceffx.demo.ShellControls;
import com.techsenger.ceffx.demo.TabOpener;
import com.techsenger.shellfx.core.ShellView;
import com.techsenger.shellfx.core.registry.ControlFactory;
import com.techsenger.shellfx.core.registry.ControlRegistry;
import com.techsenger.shellfx.material.menu.AbstractMenuItemHandler;
import com.techsenger.shellfx.material.menu.ManagedMenu;
import com.techsenger.shellfx.material.menu.ManagedMenuGroup;
import com.techsenger.shellfx.material.menu.ManagedMenuItem;
import com.techsenger.shellfx.material.menu.MenuGroupName;
import com.techsenger.shellfx.material.menu.MenuItemHandler;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Registers every menu, group, and item the demo application contributes.
 *
 * @author Pavel Castornii
 */
public class ModuleControlRegistrar {

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
        this.shell = shell;
        this.tabOpener = tabOpener;
    }

    public void register() {
        registerFileMenu();
        registerFileGroup();
        registerExitItem();
        registerBookmarkMenu();
        registerBookmarkGroups();
        IntStream
                .range(0, cefBookmarks.size())
                .forEach(i -> registerBookmarkItem(cefBookmarks.get(i), ShellControls.BookmarkMenu.CEF_GROUP, i));
        IntStream
                .range(0, popularBookmarks.size())
                .forEach(i ->
                        registerBookmarkItem(popularBookmarks.get(i), ShellControls.BookmarkMenu.POPULAR_GROUP, i));
    }

    private void registerFileMenu() {
        ControlFactory<ShellView<?>, ManagedMenu> f = (v) -> {
            var menu = new ManagedMenu(ShellControls.FileMenu.NAME, "_File", 0);
            return menu;
        };
        getRegistry().registerMenu(ShellControls.MAIN_MENU_GROUP, f);
    }

    private void registerFileGroup() {
        ControlFactory<ShellView<?>, ManagedMenuGroup> f = (v) -> {
            return new ManagedMenuGroup(ShellControls.FileMenu.GROUP, 0);
        };
        getRegistry().registerMenuGroup(ShellControls.FileMenu.NAME, f);
    }

    private void registerExitItem() {
        ControlFactory<ShellView<?>, ManagedMenuItem> f = (v) -> {
            var item = new ManagedMenuItem("E_xit", 1000);
            var handler = new AbstractMenuItemHandler<ShellView<?>, ManagedMenuItem>(shell, item) {
                @Override
                public void onAction() {
                    shell.getViewModel().getOnCloseRequest().run();
                }
            };
            MenuItemHandler.setHandler(item, handler);
            return item;
        };
        getRegistry().registerMenuItem(ShellControls.FileMenu.GROUP, f);
    }

    private void registerBookmarkMenu() {
        ControlFactory<ShellView<?>, ManagedMenu> f = (v) -> {
            var menu = new ManagedMenu(ShellControls.BookmarkMenu.NAME, "_Bookmarks", 100);
            return menu;
        };
        getRegistry().registerMenu(ShellControls.MAIN_MENU_GROUP, f);
    }

    private void registerBookmarkGroups() {
        ControlFactory<ShellView<?>, ManagedMenuGroup> f = (v) -> {
            return new ManagedMenuGroup(ShellControls.BookmarkMenu.CEF_GROUP, 0);
        };
        getRegistry().registerMenuGroup(ShellControls.BookmarkMenu.NAME, f);
        f = (v) -> {
            return new ManagedMenuGroup(ShellControls.BookmarkMenu.POPULAR_GROUP, 1);
        };
        getRegistry().registerMenuGroup(ShellControls.BookmarkMenu.NAME, f);
    }

    private void registerBookmarkItem(Bookmark bookmark, MenuGroupName<ShellView<?>> group, int pos) {
        ControlFactory<ShellView<?>, ManagedMenuItem> f = (v) -> {
            var item = new ManagedMenuItem(bookmark.title, pos);
            var handler = new AbstractMenuItemHandler<ShellView<?>, ManagedMenuItem>(shell, item) {
                @Override
                public void onAction() {
                    tabOpener.open(bookmark.url);
                }
            };
            MenuItemHandler.setHandler(item, handler);
            return item;
        };
        getRegistry().registerMenuItem(group, f);
    }

    private ControlRegistry getRegistry() {
        return this.shell.getControlRegistry();
    }
}
