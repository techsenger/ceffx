/*
 * Copyright 2026 Pavel Castornii.
 *
 * Licensed under the BSD 3-Clause License. See LICENSE file for details.
 */

package com.techsenger.ceffx.demo.menu;

import com.techsenger.shellfx.core.ShellFxView;
import com.techsenger.shellfx.core.menu.AbstractMenuItemHandler;
import com.techsenger.shellfx.core.menu.MenuItemHandler;
import com.techsenger.shellfx.core.registry.AbstractControlRegistrar;
import com.techsenger.shellfx.core.registry.ControlFactory;
import com.techsenger.shellfx.core.registry.ControlRegistry;
import com.techsenger.shellfx.material.menu.ManagedMenu;
import com.techsenger.shellfx.material.menu.ManagedMenuGroup;
import com.techsenger.shellfx.material.menu.ManagedMenuItem;

/**
 *
 * @author Pavel Castornii
 */
public class FileMenuRegistrar extends AbstractControlRegistrar {

    private final ShellFxView<?> shell;

    public FileMenuRegistrar(ControlRegistry registry, ShellFxView<?> shell) {
        super(registry);
        this.shell = shell;
    }

    @Override
    public void register() {
        registerMenu();
        registerGroup();
        registerExitItem();
    }

    protected void registerMenu() {
        ControlFactory<ShellFxView<?>, ManagedMenu> f = (v) -> {
            var menu = new ManagedMenu(FileMenu.NAME, "_File", 0);
            return menu;
        };
        addRegistration(getRegistry().mainMenu().registerMenu(f));
    }

    protected void registerGroup() {
        ControlFactory<ShellFxView<?>, ManagedMenuGroup> f = (v) -> {
            return new ManagedMenuGroup(FileMenu.GROUP, 0);
        };
        addRegistration(getRegistry().mainMenu().registerMenuGroup(FileMenu.NAME, f));
    }

    protected void registerExitItem() {
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
        addRegistration(getRegistry().mainMenu().registerMenuItem(FileMenu.GROUP, f));
    }
}
