package com.optigro.controller;

import com.optigro.models.Picker;
import com.optigro.repository.PickTaskRepository;
import com.optigro.service.BinLayoutService;
import com.optigro.service.PickTaskService;
import com.optigro.util.ConsoleScannerUtil;

public class PickingMenu {
    private final Picker picker;
    private final PickTaskService taskService;
    private final BinLayoutService binLayoutService;
    private final PickTaskRepository taskRepo;

    public PickingMenu(Picker picker, PickTaskService taskService,
                       BinLayoutService binLayoutService, PickTaskRepository taskRepo) {
        this.picker = picker;
        this.taskService = taskService;
        this.binLayoutService = binLayoutService;
        this.taskRepo = taskRepo;
    }

    public void show() {
        while (true) {
            System.out.printf("%n===== WAREHOUSE PICKER PORTAL | %s =====%n", picker.getName());
            System.out.println("1. View Tasks Completed By Me");
            System.out.println("2. Manual Pick Next Queued Task");
            System.out.println("3. Inspect Warehouse Store Grid");
            System.out.println("4. Toggle My Shift (Online/Offline)");
            System.out.println("5. Logout to Main Menu");

            int ch = ConsoleScannerUtil.readIntInRange("Choose [1-5]: ", 1, 5);
            switch (ch) {
                case 1:
                    var myTasks = taskRepo.findByPickerId(picker.getPickerId());
                    System.out.println("\n--- My Task History ---");
                    if (myTasks.isEmpty()) System.out.println("No completed tasks yet.");
                    for (var t : myTasks) System.out.println(t);
                    break;

                case 2:
                    System.out.println("Attempting to take next task from queue...");
                    try {
                        var task = taskService.takeNextTask();
                        taskService.processTask(picker, task);
                    } catch (InterruptedException e) {
                        System.out.println("Interrupted.");
                    }
                    break;

                case 3:
                    binLayoutService.printGrid();
                    break;

                case 4:
                    picker.setAvailable(!picker.isAvailable());
                    System.out.println("✓ Shift status toggled: " + (picker.isAvailable() ? "AVAILABLE" : "OFFLINE"));
                    break;

                case 5:
                    return;
            }
        }
    }
}