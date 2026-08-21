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
