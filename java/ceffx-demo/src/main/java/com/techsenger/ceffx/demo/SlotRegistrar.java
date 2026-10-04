/*
 * Copyright 2026 Pavel Castornii.
 *
 * Licensed under the BSD 3-Clause License. See LICENSE file for details.
 */

package com.techsenger.ceffx.demo;

import com.techsenger.shellfx.core.ShellView;
import com.techsenger.shellfx.core.registry.AbstractSlotRegistrar;

/**
 * Builds the tree of the slots of the demo shell: which menus the menu bar has, and which groups each menu has.
 *
 * @author Pavel Castornii
 */
public class SlotRegistrar extends AbstractSlotRegistrar {

    public SlotRegistrar(ShellView<?> shell) {
        super(shell.getContext().getSlotRegistry());
    }

    @Override
    public void register() {
        register(Slots.MAIN_MENU, 0, Slots.FileMenu.MENU);
        register(Slots.MAIN_MENU, 100, Slots.BookmarkMenu.MENU);

        register(Slots.FileMenu.MENU, 0, Slots.FileMenu.GROUP);

        register(Slots.BookmarkMenu.MENU, 0, Slots.BookmarkMenu.CEF_GROUP);
        register(Slots.BookmarkMenu.MENU, 100, Slots.BookmarkMenu.POPULAR_GROUP);
    }
}
