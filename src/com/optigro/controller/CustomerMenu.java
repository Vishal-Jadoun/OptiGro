package com.optigro.controller;

import com.optigro.constants.PaymentMode;
import com.optigro.exception.InvalidCouponException;
import com.optigro.exception.OutOfStockException;
import com.optigro.models.Customer;
import com.optigro.models.Order;
import com.optigro.models.OrderItem;
import com.optigro.models.Product;
import com.optigro.service.CartService;
import com.optigro.service.InventoryService;
import com.optigro.service.OrderService;
import com.optigro.service.PricingService;
import com.optigro.util.AsciiTableFormatter;
import com.optigro.util.ConsoleScannerUtil;

import java.util.*;


public class CustomerMenu {
    private final Customer customer;
    private final CartService cartService;
    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final PricingService pricingService;

    public CustomerMenu(Customer customer, CartService cartService, OrderService orderService,
                        InventoryService inventoryService, PricingService pricingService) {
        this.customer = customer;
        this.cartService = cartService;
        this.orderService = orderService;
        this.inventoryService = inventoryService;
        this.pricingService = pricingService;
    }

    public void show() {
        while (true) {
            System.out.printf("%n===== CUSTOMER PORTAL | Welcome, %s (Wallet: Rs.%.2f) =====%n",
                customer.getName(), customer.getWalletBalance());
            System.out.println("1. Browse / Search Products");
            System.out.println("2. Add Product to Cart");
            System.out.println("3. View Shopping Cart");
            System.out.println("4. Modify / Clear Cart");
            System.out.println("5. Checkout & Place Order (Generate GST Invoice)");
            System.out.println("6. Track Order Status");
            System.out.println("7. View My Order History");
            System.out.println("8. Logout to Main Menu");

            int choice = ConsoleScannerUtil.readIntInRange("Select option [1-8]: ", 1, 8);
            switch (choice) {
                case 1: browseProductsFlow(); break;
                case 2: addToCartFlow(); break;
                case 3: viewCartFlow(); break;
                case 4: modifyCartFlow(); break;
                case 5: checkoutFlow(); break;
                case 6: trackOrderFlow(); break;
                case 7: orderHistoryFlow(); break;
                case 8:
                default:
                    return;
            }
        }
    }

    private void browseProductsFlow() {
        System.out.println("\n1. Search by keyword   2. View all active categories (Set)");
        int sub = ConsoleScannerUtil.readIntInRange("Choose [1-2]: ", 1, 2);
        if (sub == 1) {
            String kw = ConsoleScannerUtil.readString("Enter search keyword: ");
            var prods = inventoryService.searchByName(kw);
            displayProductsTable(prods);
        } else {
            var active = inventoryService.getActiveCategories();
            System.out.println("\nIn-Stock Categories: " + active);
            var prods = inventoryService.searchByName("");
            displayProductsTable(prods);
        }
    }

    private void displayProductsTable(List<Product> prods) {
        if (prods.isEmpty()) {
            System.out.println("No products available.");
            return;
        }
        String[] headers = {"ID", "Product Name", "Category", "Price (Rs.)", "Stock Status"};
        List<String[]> rows = new ArrayList<>();
        for (var p : prods) {
            rows.add(new String[]{
                String.valueOf(p.getId()),
                p.getName(),
                p.getCategory().name(),
                String.format("%.2f", p.getPrice()),
                p.isSellable() ? (p.getStock() > 0 ? p.getStock() + " in stock" : "OUT OF STOCK") : "EXPIRED"
            });
        }
     
    }

    private void addToCartFlow() {
        int pid = ConsoleScannerUtil.readInt("Enter Product ID to add: ");
        var prod = inventoryService.getProduct(pid);
        if (prod.isEmpty() || !prod.get().isSellable() || prod.get().getStock() <= 0) {
            System.out.println("[!] Product is currently unavailable or out of stock.");
            return;
        }
        int qty = ConsoleScannerUtil.readIntInRange("Enter quantity [1-" + prod.get().getStock() + "]: ", 1, prod.get().getStock());
        try {
            cartService.addItem(customer.getCustomerId(), pid, qty);
            System.out.printf("[OK] Added %dx %s to your cart!%n", qty, prod.get().getName());
        } catch (IllegalArgumentException e) {
            System.out.println("[!] " + e.getMessage());
        }
    }

    private void viewCartFlow() {
        var cart = cartService.getCart(customer.getCustomerId());
        if (cart.isEmpty()) {
            System.out.println("Your shopping cart is empty.");
            return;
        }
        System.out.println("\n--- Your Shopping Cart ---");
        double subtotal = 0.0;
        String[] headers = {"ID", "Item Name", "Qty", "Unit Price", "Total (Rs.)"};
        List<String[]> rows = new ArrayList<>();
        for (var entry : cart.entrySet()) {
            var p = inventoryService.getProduct(entry.getKey()).orElseThrow();
            double lineTotal = p.getPrice() * entry.getValue();
            subtotal += lineTotal;
            rows.add(new String[]{
                String.valueOf(p.getId()), p.getName(), String.valueOf(entry.getValue()),
                String.format("%.2f", p.getPrice()), String.format("%.2f", lineTotal)
            });
        }
        AsciiTableFormatter.printTable(headers, rows);
        System.out.printf("Estimated Subtotal: Rs.%.2f%n", subtotal);
    }

    private void modifyCartFlow() {
        if (cartService.isEmpty(customer.getCustomerId())) {
            System.out.println("Cart is empty.");
            return;
        }
        viewCartFlow();
        System.out.println("1. Update Quantity  2. Remove Item  3. Clear Cart");
        int ch = ConsoleScannerUtil.readIntInRange("Choose [1-3]: ", 1, 3);
        if (ch == 1) {
            int pid = ConsoleScannerUtil.readInt("Enter Product ID: ");
            int qty = ConsoleScannerUtil.readInt("Enter new quantity (0 to remove): ");
            cartService.updateQuantity(customer.getCustomerId(), pid, qty);
            System.out.println("[OK] Cart updated.");
        } else if (ch == 2) {
            int pid = ConsoleScannerUtil.readInt("Enter Product ID to remove: ");
            cartService.removeItem(customer.getCustomerId(), pid);
            System.out.println("[OK] Item removed.");
        } else {
            cartService.clearCart(customer.getCustomerId());
            System.out.println("[OK] Cart cleared.");
        }
    }

    private void checkoutFlow() {
        var cart = cartService.getCart(customer.getCustomerId());
        if (cart.isEmpty()) {
            System.out.println("[!] Your cart is empty. Add items before checking out.");
            return;
        }

        viewCartFlow();
        System.out.println("\nSelect Payment Mode: 1.COD  2.WALLET (Bal: Rs." + customer.getWalletBalance() + ")  3.UPI");
        int payChoice = ConsoleScannerUtil.readIntInRange("Payment Mode [1-3]: ", 1, 3);
        PaymentMode mode = PaymentMode.values()[payChoice - 1];

        String coupon = ConsoleScannerUtil.readString("Enter coupon code (e.g. WELCOME50, SAVE10, FREEDEL) or press Enter to skip: ");
        if (coupon.trim().isEmpty()) coupon = null;

        try {
            Order order = orderService.placeOrder(customer.getCustomerId(), cart, mode, coupon);
            cartService.clearCart(customer.getCustomerId());
            printInvoice(order);
            System.out.printf("[OK] Order #%d placed successfully! Current status: %s%n", order.getOrderId(), order.getStatus());
        } catch (OutOfStockException | InvalidCouponException | IllegalStateException e) {
            System.out.println("[!] Checkout Failed: " + e.getMessage());
        }
    }

    private void printInvoice(Order order) {
        System.out.println("\n+==========================================================+");
        System.out.println("|                       OPTIGRO                            |");
        System.out.println("|                     GST INVOICE                          |");
        System.out.println("+==========================================================+");
        System.out.printf("| Order ID: %-15d Date: %-22s |%n", order.getOrderId(), order.getPlacedAt().toLocalDate());
        System.out.printf("| Customer: %-15s Phone: %-21s |%n", customer.getName(), customer.getPhone());
        System.out.printf("| Address:  %-44s |%n", customer.getAddress().length() > 44 ? customer.getAddress().substring(0, 41) + "..." : customer.getAddress());
        System.out.println("+==========================================================+");
        System.out.printf("| %-25s %-4s %-10s %-11s |%n", "Item", "Qty", "Price", "Total");
        System.out.println("+----------------------------------------------------------+");
        for (OrderItem item : order.getItems()) {
            String name = item.getProductName().length() > 25 ? item.getProductName().substring(0, 22) + "..." : item.getProductName();
            System.out.printf("| %-25s %-4d Rs.%-7.2f Rs.%-8.2f |%n",
                name, item.getQuantity(), item.getUnitPrice(), item.getLineTotal());
        }
        System.out.println("+==========================================================+");
        System.out.printf("| Subtotal:                                    Rs.%-8.2f |%n", order.getSubtotal());
        System.out.printf("| GST (Taxes):                                 Rs.%-8.2f |%n", order.getGst());
        System.out.printf("| Delivery Fee:                                Rs.%-8.2f |%n", order.getDeliveryFee());
        System.out.printf("| Discount Applied:                           -Rs.%-8.2f |%n", order.getDiscount());
        System.out.println("+==========================================================+");
        System.out.printf("| FINAL PAYABLE:                               Rs.%-8.2f |%n", order.getTotal());
        System.out.printf("| Payment Method: %-40s |%n", order.getPaymentMode());
        System.out.println("+==========================================================+");
    }

    private void trackOrderFlow() {
        int oid = ConsoleScannerUtil.readInt("Enter Order ID to track: ");
        var order = orderService.getOrder(oid);
        if (order.isEmpty() || order.get().getCustomerId() != customer.getCustomerId()) {
            System.out.println("[!] Order not found on your profile.");
            return;
        }
        System.out.printf("Order #%d | Status: [%s] | Placed: %s%n",
            order.get().getOrderId(), order.get().getStatus(), order.get().getPlacedAt());
    }

    private void orderHistoryFlow() {
        var orders = orderService.getOrdersByCustomer(customer.getCustomerId());
        if (orders.isEmpty()) {
            System.out.println("No past orders found.");
            return;
        }
        System.out.printf("%-8s %-12s %-18s %-10s%n", "Order ID", "Items", "Status", "Total (Rs.)");
        System.out.println("-".repeat(55));
        for (var o : orders) {
            System.out.printf("%-8d %-12d %-18s Rs.%-8.2f%n",
                o.getOrderId(), o.getItems().size(), o.getStatus(), o.getTotal());
        }
    }
}
