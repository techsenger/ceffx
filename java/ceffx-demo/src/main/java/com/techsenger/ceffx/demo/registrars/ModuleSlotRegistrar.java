/*
 * Copyright 2026 Pavel Castornii.
 *
 * Licensed under the BSD 3-Clause License. See LICENSE file for details.
 */

package com.techsenger.ceffx.demo.registrars;

import com.techsenger.ceffx.demo.slots.ShellSlots;
import com.techsenger.shellfx.core.ShellView;
import com.techsenger.shellfx.core.registrars.AbstractSlotRegistrar;

/**
 * Builds the tree of the slots of the demo shell: which menus the menu bar has, and which groups each menu has.
 *
 * @author Pavel Castornii
 */
public class ModuleSlotRegistrar extends AbstractSlotRegistrar {

    public ModuleSlotRegistrar(ShellView<?> shell) {
        super(shell.getContext().getSlotRegistry());
    }

    @Override
    public void register() {
        register(ShellSlots.MAIN_MENU, 0, ShellSlots.FileMenu.MENU);
        register(ShellSlots.MAIN_MENU, 100, ShellSlots.BookmarkMenu.MENU);

        register(ShellSlots.FileMenu.MENU, 0, ShellSlots.FileMenu.GROUP);

        register(ShellSlots.BookmarkMenu.MENU, 0, ShellSlots.BookmarkMenu.CEF_GROUP);
        register(ShellSlots.BookmarkMenu.MENU, 100, ShellSlots.BookmarkMenu.POPULAR_GROUP);
    }
}
