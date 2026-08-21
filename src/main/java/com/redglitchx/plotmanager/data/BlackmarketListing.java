/*
 * PlotManager — The Ultimate Plot Management System
 * Copyright (c) 2026 RedGlitchX. All Rights Reserved.
 *
 * This file is proprietary and confidential. Unauthorised copying,
 * redistribution, modification or use of this file, via any medium,
 * is strictly prohibited. See the LICENSE file for the full terms.
 */
package com.redglitchx.plotmanager.data;

import java.util.UUID;

public class BlackmarketListing {
    public UUID id = UUID.randomUUID();
    public UUID seller;
    public String sellerName;
    public UUID plotId;
    public String itemBase64;
    public double price;
    public long created = System.currentTimeMillis();
}
