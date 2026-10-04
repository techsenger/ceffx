/*
 * Copyright 2026 Pavel Castornii.
 *
 * Licensed under the BSD 3-Clause License. See LICENSE file for details.
 */

package com.techsenger.ceffx.demo;

import com.techsenger.shellfx.core.ShellView;
import com.techsenger.shellfx.material.slot.GroupSlot;
import com.techsenger.shellfx.material.slot.MenuBarSlot;
import com.techsenger.shellfx.material.slot.MenuSlot;
import javafx.scene.control.MenuItem;

/**
 * The slots - the menu bar, its menus, and their groups - the demo application's shell offers.
 *
 * @author Pavel Castornii
 */
public final class Slots {

    public static final class FileMenu {

        public static final MenuSlot<ShellView<?>> MENU = new MenuSlot<>(ShellView.class, "File");

        public static final GroupSlot<ShellView<?>, MenuItem> GROUP = new GroupSlot<>(ShellView.class, "Group");

        private FileMenu() {
            // empty
        }
    }

    public static final class BookmarkMenu {

        public static final MenuSlot<ShellView<?>> MENU = new MenuSlot<>(ShellView.class, "Bookmarks");

        public static final GroupSlot<ShellView<?>, MenuItem> CEF_GROUP =
                new GroupSlot<>(ShellView.class, "Cef Group");

        public static final GroupSlot<ShellView<?>, MenuItem> POPULAR_GROUP =
                new GroupSlot<>(ShellView.class, "Popular Group");

        private BookmarkMenu() {
            // empty
        }
    }

    /**
     * The menu bar of the shell; the File and Bookmarks menus are put into it.
     */
    public static final MenuBarSlot<ShellView<?>> MAIN_MENU = new MenuBarSlot<>(ShellView.class, "MainMenu");

    private Slots() {
        // empty
    }
}
