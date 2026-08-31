/*
 * Copyright 2026 Pavel Castornii.
 *
 * Licensed under the BSD 3-Clause License. See LICENSE file for details.
 */

package com.techsenger.ceffx.demo.menu;

import com.techsenger.ceffx.demo.TabOpener;
import com.techsenger.shellfx.core.ShellFxView;
import com.techsenger.shellfx.core.registry.AbstractControlRegistrar;
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
 *
 * @author Pavel Castornii
 */
public class BookmarkMenuRegistrar extends AbstractControlRegistrar {

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

    public BookmarkMenuRegistrar(ControlRegistry registry, ShellFxView<?> shell, TabOpener tabOpener) {
        super(registry);
        this.shell = shell;
        this.tabOpener = tabOpener;
    }

    @Override
    public void register() {
        registerMenu();
        registerGroups();
        IntStream
                .range(0, cefBookmarks.size())
                .forEach(i -> registerBookmarkItem(cefBookmarks.get(i), BookmarkMenu.CEF_GROUP, i));
        IntStream
                .range(0, popularBookmarks.size())
                .forEach(i -> registerBookmarkItem(popularBookmarks.get(i), BookmarkMenu.POPULAR_GROUP, i));
    }

    protected void registerMenu() {
        ControlFactory<ShellFxView<?>, ManagedMenu> f = (v) -> {
            var menu = new ManagedMenu(BookmarkMenu.NAME, "_Bookmarks", 100);
            return menu;
        };
        addRegistration(getRegistry().mainMenu().registerMenu(f));
    }

    protected void registerGroups() {
        ControlFactory<ShellFxView<?>, ManagedMenuGroup> f = (v) -> {
            return new ManagedMenuGroup(BookmarkMenu.CEF_GROUP, 0);
        };
        addRegistration(getRegistry().mainMenu().registerMenuGroup(BookmarkMenu.NAME, f));
        f = (v) -> {
            return new ManagedMenuGroup(BookmarkMenu.POPULAR_GROUP, 1);
        };
        addRegistration(getRegistry().mainMenu().registerMenuGroup(BookmarkMenu.NAME, f));
    }

    protected void registerBookmarkItem(Bookmark bookmark, MenuGroupName group, int pos) {
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
        addRegistration(getRegistry().mainMenu().registerMenuItem(group, f));
    }
}
