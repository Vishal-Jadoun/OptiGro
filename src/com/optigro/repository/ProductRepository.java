package com.optigro.repository;

import com.optigro.constants.ProductCategory;
import com.optigro.models.NonPerishableProduct;
import com.optigro.models.PerishableProduct;
import com.optigro.models.Product;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ProductRepository implements BaseRepository<Product>{
    private final ConcurrentHashMap<Integer, Product> store = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger(1000);

    @Override
    public void save(Product p) {
        store.put(p.getId(), p);
    }

    @Override
    public Optional<Product> findById(int id) {
        return Optional.ofNullable(store.get(id));
    }
    @Override
    public List<Product> findAll() {
        return new ArrayList<>(store.values());
    }
    @Override
    public boolean deleteById(int id) {
        return store.remove(id) != null;
    }
    @Override
    public int nextId() {
        return counter.incrementAndGet();
    }

    public List<Product> findByCategory(ProductCategory cat) {
        return store.values().stream()
                .filter(p -> p.getCategory() == cat)
                .collect(Collectors.toList());
    }

    public void loadFromCsv(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split(",");
                if (parts.length < 8) continue;

                int id = Integer.parseInt(parts[0].trim());
                String name = parts[1].trim();
                ProductCategory category = ProductCategory.valueOf(parts[2].trim());
                double price = Double.parseDouble(parts[3].trim());
                int stock = Integer.parseInt(parts[4].trim());
                int reorder = Integer.parseInt(parts[5].trim());
                String type = parts[6].trim();

                Product product;
                if ("PERISHABLE".equalsIgnoreCase(type)) {
                    LocalDate expiry = LocalDate.parse(parts[7].trim());
                    product = new PerishableProduct(id, name, category, price, stock, reorder, expiry);
                } else {
                    int shelfLife = Integer.parseInt(parts[7].trim());
                    product = new NonPerishableProduct(id, name, category, price, stock, reorder, shelfLife);
                }

                save(product);
                if (id >= counter.get()) {
                    counter.set(id);
                }
            }
        }
    }

    public void saveToCsv(String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("# id,name,category,price,stock,reorderLevel,type,extra\n");
            for (Product p : findAll()) {
                if (p instanceof PerishableProduct) {
                    PerishableProduct pp = (PerishableProduct) p;
                    writer.write(String.format("%d,%s,%s,%.2f,%d,%d,PERISHABLE,%s%n",
                            pp.getId(), pp.getName(), pp.getCategory().name(),
                            pp.getPrice(), pp.getStock(), pp.getReorderLevel(), pp.getExpiryDate()));
                } else if (p instanceof NonPerishableProduct) {
                    NonPerishableProduct np = (NonPerishableProduct) p;
                    writer.write(String.format("%d,%s,%s,%.2f,%d,%d,NON_PERISHABLE,%d%n",
                            np.getId(), np.getName(), np.getCategory().name(),
                            np.getPrice(), np.getStock(), np.getReorderLevel(), np.getShelfLifeMonths()));
                }
            }
        }
    }


}
