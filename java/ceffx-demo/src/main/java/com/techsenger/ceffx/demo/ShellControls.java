/*
 * Copyright 2026 Pavel Castornii.
 *
 * Licensed under the BSD 3-Clause License. See LICENSE file for details.
 */

package com.techsenger.ceffx.demo;

import com.techsenger.shellfx.core.ShellView;
import com.techsenger.shellfx.material.menu.DefaultMenuGroupName;
import com.techsenger.shellfx.material.menu.DefaultMenuName;
import com.techsenger.shellfx.material.menu.MenuGroupName;
import com.techsenger.shellfx.material.menu.MenuName;

/**
 * The menus and menu groups the demo application's shell menu bar offers.
 *
 * @author Pavel Castornii
 */
public final class ShellControls {

    public static final class FileMenu {

        public static final MenuName<ShellView<?>> NAME = createName();

        public static final MenuGroupName<ShellView<?>> GROUP = createGroupName("Group");

        private FileMenu() {
            // empty
        }
    }

    public static final class BookmarkMenu {

        public static final MenuName<ShellView<?>> NAME = createName();

        public static final MenuGroupName<ShellView<?>> CEF_GROUP = createGroupName("Cef Group");

        public static final MenuGroupName<ShellView<?>> POPULAR_GROUP = createGroupName("Popular Group");

        private BookmarkMenu() {
            // empty
        }
    }

    /**
     * The group File/Bookmark menus register into, and that {@link com.techsenger.shellfx.core.DefaultShellView}
     * treats as the top-level group of its own menu bar.
     */
    public static final MenuGroupName<ShellView<?>> MAIN_MENU_GROUP = createGroupName("MainMenuGroup");

    private static MenuName<ShellView<?>> createName() {
        return new DefaultMenuName<>(ShellView.class);
    }

    private static MenuGroupName<ShellView<?>> createGroupName(String text) {
        return new DefaultMenuGroupName<>(ShellView.class, text);
    }

    private ShellControls() {
        // empty
    }
}
