/*
 * PlotManager — The Ultimate Plot Management System
 * Copyright (c) 2026 RedGlitchX. All Rights Reserved.
 *
 * This file is proprietary and confidential. Unauthorised copying,
 * redistribution, modification or use of this file, via any medium,
 * is strictly prohibited. See the LICENSE file for the full terms.
 */
package com.redglitchx.plotmanager.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CustomHologram {
    public UUID id = UUID.randomUUID();
    public String world;
    public double x, y, z;
    public List<String> lines = new ArrayList<>();
}
