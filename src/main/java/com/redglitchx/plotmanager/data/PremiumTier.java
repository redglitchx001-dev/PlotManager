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
