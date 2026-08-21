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

public class PremiumTier {
    public String id;
    public String displayName;
    public List<String> luckpermsGroups = new ArrayList<>();
    public double claimCost;
    public int maxPlots;
    public int maxMembers;
    public int hopperLimit;
    public int spawnerLimit;
}
