package com.optigro.service;

import com.optigro.models.Bin;
import com.optigro.models.OrderItem;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PickRoutePlanner {
    private final BinLayoutService binLayoutService;
    public PickRoutePlanner(BinLayoutService binLayoutService) {
        this.binLayoutService = binLayoutService;
    }
    public BinLayoutService getBinLayoutService() {
        return binLayoutService;
    }

    public List<OrderItem> planRoute(List<OrderItem> items) {
        return items.stream()
            .sorted((a, b) -> {
                Bin binA = binLayoutService.locate(a.getProductId());
                Bin binB = binLayoutService.locate(b.getProductId());

                int aisleCompare = Integer.compare(binA.getAisle(), binB.getAisle());
                if (aisleCompare != 0) return aisleCompare;

                if (binA.getAisle() % 2 == 0) {
                    return Integer.compare(binA.getShelf(), binB.getShelf());
                } else {
                    return Integer.compare(binB.getShelf(), binA.getShelf());
                }
            })
            .collect(Collectors.toList());
    }
}