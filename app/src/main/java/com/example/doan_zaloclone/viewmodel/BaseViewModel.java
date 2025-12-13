package com.example.doan_zaloclone.viewmodel;

import androidx.lifecycle.ViewModel;

/**
 * Base ViewModel class providing common functionality for all ViewModels
 * Extends Android Architecture Components ViewModel for lifecycle awareness
 */
public abstract class BaseViewModel extends ViewModel {
    
    /**
     * Override this method to clean up resources when ViewModel is destroyed
     * Remember to call super.onCleared() when overriding
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        // Base cleanup - subclasses should override and add their own cleanup
    }
    
    /**
     * Helper method to generate user-friendly error messages
     * @param exception The exception to convert to a message
     * @return User-friendly error message
     */
    protected String getErrorMessage(Exception exception) {
        if (exception == null) {
            return "An unknown error occurred";
        }
        
        String message = exception.getMessage();
        if (message != null && !message.isEmpty()) {
            return message;
        }
        
        return "An error occurred: " + exception.getClass().getSimpleName();
    }
}
