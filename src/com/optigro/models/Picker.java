package com.optigro.models;

import java.util.Objects;

public class Picker{
    private final int pickerId;
    private final int userId;
    private final String name ;
    private boolean available ;
    private int tasksCompleted;

    public Picker(int pickerId , int userId , String name){
        this.pickerId = pickerId;
        this.userId = userId;
        this.name = Objects.requireNonNull(name , "Name cannot be null");
        this.available = true;
        this.tasksCompleted = 0;
    }

    public int getPickerId() {return pickerId ;}
    public int getUserId() {return userId; }
    public String getName() {return name; }
    public boolean isAvailable() {return available; }
    public void setAvailable(boolean available) {this.available = available;} 
    public int getTaskCompleted() {return tasksCompleted;}
    public void incrementTasksCompleted() {this.tasksCompleted++;} 

    @Override 
    public boolean equals(Object o){
        if (this == o)return true;
        if(!(o instanceof Picker)) return false;
        Picker picker = (Picker) o;
        return pickerId == picker.pickerId;
    }
    
    @Override
    public int hashCode(){
        return Objects.hash(pickerId);
    }

    @Override 
    public String toString(){
        return String.format("Picker #%d: %-15s | Status: %-10s | Completed: %d" , pickerId , name , available ? "AVAILABLE" : "BUSY" , tasksCompleted);
    }
}