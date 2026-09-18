/*
 * Copyright 2026 Pavel Castornii.
 *
 * Licensed under the BSD 3-Clause License. See LICENSE file for details.
 */

package com.techsenger.ceffx.demo.tab;

import com.techsenger.ceffx.core.browser.CefBrowserBase;
import com.techsenger.shellfx.core.tab.TabParams;

/**
 *
 * @author Pavel Castornii
 */
public class BrowserTabParams extends TabParams {

    private final CefBrowserBase browser;

    public BrowserTabParams(CefBrowserBase browser) {
        this.browser = browser;
    }

    public CefBrowserBase getBrowser() {
        return browser;
    }
}
