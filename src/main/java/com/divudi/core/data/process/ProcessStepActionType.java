package com.divudi.core.data.process;

/**
 * Author : Dr M H B Ariyaratne buddhika.ari@gmail.com Defines possible actions
 * that can be taken by a ProcessStepActionDefinition.
 */
public enum ProcessStepActionType {
    COMPLETE_STEP("Complete Current Step"),
    COMPLETE_PROCESS("Complete Entire Process"),
    CANCEL_PROCESS("Cancel Process"), // Indicates stopping the process for reasons like irrelevance or user withdrawal
    REJECT_PROCESS("Reject Process"), // Implies the process does not meet certain criteria or standards
    MOVE_TO_NEXT_STEP("Move to Next Step"),
    RETURN_TO_PREVIOUS_STEP("Return to Previous Step"),
    MOVE_TO_SPECIFIED_STEP("Move to a Specified Step"), // Allows jumping to any specified step
    PAUSE_PROCESS("Pause Entire Process"),
    ESCALATE_ISSUE("Escalate Issue"),
    CUSTOM_ACTION("Custom Action");  // Allows for extension without modifying the enum

    private final String displayLabel;

    ProcessStepActionType(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }
}
