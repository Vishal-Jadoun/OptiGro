package com.optigro.models;

import com.optigro.constants.ProductCategory;

public class NonPerishableProduct extends Product {
        private final int shelfLifeMonths;
    public NonPerishableProduct(int id, String name, ProductCategory category, double price, int stock, int reorderLevel, int shelfLifeMonths){
        super(id,name,category,price,stock,reorderLevel);
        if(shelfLifeMonths <= 0){
            throw new IllegalArgumentException("Shelf Life must be positive");
        }
        this.shelfLifeMonths=shelfLifeMonths;
    }
    public int getShelfLifeMonths(){
        return shelfLifeMonths;
    }

    @Override
    public boolean isSellable(){
        return true;
    }

    @Override
    public String getStorageInfo(){
        return "Ambient shelf stable - " + shelfLifeMonths + " months shelf life";
    }

    @Override
    public String toString(){
        return super.toString()+String.format(" | ShelfLife: %d mo",shelfLifeMonths);
    }
}
