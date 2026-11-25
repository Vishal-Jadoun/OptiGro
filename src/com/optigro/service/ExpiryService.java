package com.optigro.service;

import com.optigro.models.PerishableProduct;
import com.optigro.repository.ProductRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ExpiryService {
    private final ProductRepository productRepo;
    public ExpiryService(ProductRepository productRepo) {
        this.productRepo = productRepo;
    }
    private TreeMap<LocalDate, List<PerishableProduct>> buildExpiryMap() {
        TreeMap<LocalDate, List<PerishableProduct>> map = new TreeMap<>();
        for (var p : productRepo.findAll()) {
            if (p instanceof PerishableProduct) {
                PerishableProduct pp = (PerishableProduct) p;
                map.computeIfAbsent(pp.getExpiryDate(), k -> new ArrayList<>()).add(pp);
            }
        }
        return map;
    }
    public List<PerishableProduct> expiringWithinDays(int days) {
        LocalDate threshold = LocalDate.now().plusDays(days + 1);
        TreeMap<LocalDate, List<PerishableProduct>> map = buildExpiryMap();
        List<PerishableProduct> result = new ArrayList<>();
        for (List<PerishableProduct> list : map.headMap(threshold).values()) {
            for (PerishableProduct pp : list) {
                if (pp.isSellable()) {
                    result.add(pp);
                }
            }
        }
        return result;
    }
    public List<PerishableProduct> getExpired() {
        LocalDate today = LocalDate.now();
        return productRepo.findAll().stream()
                .filter(p -> p instanceof PerishableProduct)
                .map(p -> (PerishableProduct) p)
                .filter(pp -> today.isAfter(pp.getExpiryDate()))
                .collect(Collectors.toList());
    }
}
