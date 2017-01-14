package com.quaap.launchtime.components.future;

import android.view.DragEvent;
import android.view.View;

import com.quaap.launchtime.components.AppShortcut;

/**
 * Created by tom on 1/10/17.
 * <p>
 * Copyright (C) 2017  tom
 * <p>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
public interface CategoryListener {

    void onAppShortcutClickListener(CategoryView categoryView, AppShortcut app, View view);

    void onAppShortcutLongClickListener(CategoryView categoryView, AppShortcut app, View view);

    void onAppShortcutDragListener(CategoryView categoryView, AppShortcut app, View view, DragEvent dragEvent);
}
