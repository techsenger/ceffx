/*
 * Copyright 2026 Pavel Castornii.
 *
 * Licensed under the BSD 3-Clause License. See LICENSE file for details.
 */

package com.techsenger.ceffx.demo.controls;

import com.techsenger.ceffx.demo.ShellControls;
import com.techsenger.ceffx.demo.TabOpener;
import com.techsenger.shellfx.core.ShellFxView;
import com.techsenger.shellfx.core.registry.AbstractControlRegistrar;
import com.techsenger.shellfx.core.registry.ControlFactory;
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

    private final ShellFxView<?> shell;

    private final TabOpener tabOpener;

    public ModuleControlRegistrar(ShellFxView<?> shell, TabOpener tabOpener) {
        super(shell.getControlRegistry());
        this.shell = shell;
        this.tabOpener = tabOpener;
    }

    @Override
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
        ControlFactory<ShellFxView<?>, ManagedMenu> f = (v) -> {
            var menu = new ManagedMenu(ShellControls.FileMenu.NAME, "_File", 0);
            return menu;
        };
        addRegistration(getRegistry().registerMenu(ShellControls.MAIN_MENU_GROUP, f));
    }

    private void registerFileGroup() {
        ControlFactory<ShellFxView<?>, ManagedMenuGroup> f = (v) -> {
            return new ManagedMenuGroup(ShellControls.FileMenu.GROUP, 0);
        };
        addRegistration(getRegistry().registerMenuGroup(ShellControls.FileMenu.NAME, f));
    }

    private void registerExitItem() {
        ControlFactory<ShellFxView<?>, ManagedMenuItem> f = (v) -> {
            var item = new ManagedMenuItem("E_xit", 1000);
            var handler = new AbstractMenuItemHandler<ShellFxView<?>, ManagedMenuItem>(shell, item) {
                @Override
                public void onAction() {
                    shell.getPresenter().getOnCloseRequest().run();
                }
            };
            MenuItemHandler.setHandler(item, handler);
            return item;
        };
        addRegistration(getRegistry().registerMenuItem(ShellControls.FileMenu.GROUP, f));
    }

    private void registerBookmarkMenu() {
        ControlFactory<ShellFxView<?>, ManagedMenu> f = (v) -> {
            var menu = new ManagedMenu(ShellControls.BookmarkMenu.NAME, "_Bookmarks", 100);
            return menu;
        };
        addRegistration(getRegistry().registerMenu(ShellControls.MAIN_MENU_GROUP, f));
    }

    private void registerBookmarkGroups() {
        ControlFactory<ShellFxView<?>, ManagedMenuGroup> f = (v) -> {
            return new ManagedMenuGroup(ShellControls.BookmarkMenu.CEF_GROUP, 0);
        };
        addRegistration(getRegistry().registerMenuGroup(ShellControls.BookmarkMenu.NAME, f));
        f = (v) -> {
            return new ManagedMenuGroup(ShellControls.BookmarkMenu.POPULAR_GROUP, 1);
        };
        addRegistration(getRegistry().registerMenuGroup(ShellControls.BookmarkMenu.NAME, f));
    }

    private void registerBookmarkItem(Bookmark bookmark, MenuGroupName<ShellFxView<?>> group, int pos) {
        ControlFactory<ShellFxView<?>, ManagedMenuItem> f = (v) -> {
            var item = new ManagedMenuItem(bookmark.title, pos);
            var handler = new AbstractMenuItemHandler<ShellFxView<?>, ManagedMenuItem>(shell, item) {
                @Override
                public void onAction() {
                    tabOpener.open(bookmark.url);
                }
            };
            MenuItemHandler.setHandler(item, handler);
            return item;
        };
        addRegistration(getRegistry().registerMenuItem(group, f));
    }
}
