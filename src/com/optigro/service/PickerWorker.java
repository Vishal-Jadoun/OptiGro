package com.optigro.service;

import com.optigro.models.PickTask;
import com.optigro.models.Picker;

public class PickerWorker implements Runnable {
    private final Picker picker;
    private final PickTaskService taskService;
    public PickerWorker(Picker picker, PickTaskService taskService) {
        this.picker = picker;
        this.taskService = taskService;
    }

    @Override
    public void run() {
        System.out.printf("[WorkerThread] %s is active and waiting on queue...%n", picker.getName());
        while (!Thread.currentThread().isInterrupted()) {
            try {
                PickTask task = taskService.takeNextTask();
                taskService.processTask(picker, task);
            } 
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.printf("[WorkerThread] %s finished shift.%n", picker.getName());
    }
}