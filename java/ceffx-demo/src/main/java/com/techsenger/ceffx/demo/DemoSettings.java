/*
 * Copyright 2026 Pavel Castornii.
 *
 * Licensed under the BSD 3-Clause License. See LICENSE file for details.
 */

package com.techsenger.ceffx.demo;

import com.techsenger.shellfx.core.settings.AbstractShellSettings;
import com.techsenger.shellfx.core.settings.AppearanceSettings;
import com.techsenger.shellfx.core.settings.DefaultAppearanceSettings;

/**
 *
 * @author Pavel Castornii
 */
public class DemoSettings extends AbstractShellSettings {

    private final AppearanceSettings appearance = new DefaultAppearanceSettings();

    @Override
    public AppearanceSettings getAppearance() {
        return appearance;
    }

}
