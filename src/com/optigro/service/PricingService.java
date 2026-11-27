package com.optigro.service;

import com.optigro.constants.ProductCategory;
import com.optigro.exception.InvalidCouponException;
import com.optigro.models.Customer;
import com.optigro.models.OrderItem;
import com.optigro.models.Product;

import java.util.List;


public class PricingService {

    public double getGSTRate(ProductCategory category) {
        switch (category) {
            case STAPLES:
            case DAIRY:
                return 0.05; // 5% GST
            case FRUITS:
            case VEGETABLES:
                return 0.00; // 0% GST (Fresh essentials exempt)
            case SNACKS:
            case BEVERAGES:
                return 0.12; // 12% GST
            default:
                return 0.05;
        }
    }

   
    public double calculateSubtotal(List<OrderItem> items) {
        return items.stream()
            .mapToDouble(OrderItem::getLineTotal)
            .sum();
    }

    
    public double calculateGST(List<OrderItem> items, InventoryService inventory) {
        return items.stream()
            .mapToDouble(item -> {
                Product p = inventory.getProduct(item.getProductId())
                    .orElseThrow(() -> new IllegalStateException("Product not found: " + item.getProductId()));
                double rate = getGSTRate(p.getCategory());
                return item.getLineTotal() * rate;
            })
            .sum();
    }

    
    public double calculateDeliveryFee(double subtotal, String couponCode) {
        if ("FREEDEL".equalsIgnoreCase(couponCode)) {
            return 0.0;
        }
        return (subtotal >= 500.0) ? 0.0 : 30.0;
    }

    
    public double applyCoupon(String couponCode, double subtotal, Customer customer)
            throws InvalidCouponException {
        if (couponCode == null || couponCode.trim().isEmpty()) {
            return 0.0;
        }
        String code = couponCode.trim().toUpperCase();

        
        if (customer.hasUsedCoupon(code)) {
            throw new InvalidCouponException("Coupon '" + code + "' has already been redeemed on this account.");
        }

        switch (code) {
            case "WELCOME50":
                if (subtotal < 300.0) {
                    throw new InvalidCouponException("WELCOME50 requires a minimum cart subtotal of Rs.300.00");
                }
                return 50.0;

            case "SAVE10":
                
                return Math.min(100.0, subtotal * 0.10);

            case "FREEDEL":
                
                return 0.0;

            default:
                throw new InvalidCouponException("Invalid or unrecognized coupon code: " + code);
        }
    }
}
