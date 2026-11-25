package com.optigro.models;

import com.optigro.constants.ProductCategory;

import java.time.LocalDate;
import java.util.Objects;

public class PerishableProduct extends Product{
    private final LocalDate expiryDate;
    public PerishableProduct (int id, String name, ProductCategory category,double price,int stock,int reorderLevel,LocalDate expiryDate){
        super(id,name,category,price,stock,reorderLevel);
        this.expiryDate = Objects.requireNonNull(expiryDate,"Expire date cannot be null");
    }

    public LocalDate getExpiryDate(){
        return expiryDate;
    }

    @Override
    public boolean isSellable(){
        return !LocalDate.now().isAfter(expiryDate);
    }

    @Override
    public String getStorageInfo(){
        return "Chilled storage - expires " + expiryDate;
    }

    @Override
    public String toString(){
        return super.toString() + String.format(" | Exp: %s" , expiryDate);
    }
}
