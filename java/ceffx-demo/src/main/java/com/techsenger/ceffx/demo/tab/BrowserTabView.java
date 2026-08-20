/*
 * Copyright 2026 Pavel Castornii.
 *
 * Licensed under the BSD 3-Clause License. See LICENSE file for details.
 */

package com.techsenger.ceffx.demo.tab;

import com.techsenger.shellfx.core.history.HistoryManager;
import com.techsenger.shellfx.core.settings.ShellSettings;
import com.techsenger.shellfx.core.tab.TabView;
import javafx.scene.Cursor;

/**
 *
 * @author Pavel Castornii
 */
public interface BrowserTabView extends TabView {

    interface Composer extends TabView.Composer {

        void addDevTools(ShellSettings settings, HistoryManager historyManager);

        void removeDevTools();
    }

    @Override
    Composer getComposer();

    void updateAddress(String url);

    void transferFocusFromBrowser();

    void updateCursor(Cursor cursor);
}
