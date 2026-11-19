package com.optigro.models;

import com.optigro.constants.ProductCategory;
import java.util.Objects;

public abstract class Product {
    private final int id;
    private final String name;
    private final ProductCategory category;
    private final double price;
    private int stock;
    private final int reorderLevel;

    protected Product(int id,String name,ProductCategory category,double price,int stock,int reorderLevel){
        if(price <=0) throw new IllegalArgumentException("Price must be Positive");
        if(stock<0) throw new IllegalArgumentException("Stock cannot be Negative");
        if(reorderLevel<0) throw new IllegalArgumentException("Reorder level cannot be Negative");

        this.id=id;
        this.name=name;
        this.category=category;
        this.price=price;
        this.stock=stock;
        this.reorderLevel=reorderLevel;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public double getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        if(stock<0) throw new IllegalArgumentException("Stock cannot be Negative");
        this.stock = stock;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public abstract boolean isSellable();
    public abstract String getStorageInfo();

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof Product)) return false;
        Product product = (Product) o;
        return id == product.id;
    }

    @Override
    public int hashCode(){
        return Objects.hash(id);
    }

    @Override
    public String toString(){
        return String.format("%d | %-20s | Rs.%-6.2f | Stock: %-4d",id,name,price,stock);
    }
}
