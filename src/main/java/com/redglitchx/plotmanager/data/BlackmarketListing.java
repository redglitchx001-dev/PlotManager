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
