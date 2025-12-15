package com.example.doan_zaloclone.ui.room.actions;

import java.util.ArrayList;
import java.util.List;

/**
 * Manager class for quick actions
 * Singleton pattern for global access
 * Registry pattern for extensibility
 */
public class QuickActionManager {
    
    private static QuickActionManager instance;
    private List<QuickAction> actions;
    
    private QuickActionManager() {
        actions = new ArrayList<>();
        registerDefaultActions();
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized QuickActionManager getInstance() {
        if (instance == null) {
            instance = new QuickActionManager();
        }
        return instance;
    }
    
    private void registerDefaultActions() {
        // Register send image action
        registerAction(new SendImageAction());
        
        // Register send file action
        registerAction(new SendFileAction());
        
        // Future actions can be added here
        // registerAction(new SendLocationAction());
        // registerAction(new SendContactAction());
    }
    
    /**
     * Register a new action
     * @param action Action to register
     */
    public void registerAction(QuickAction action) {
        if (action != null && !actions.contains(action)) {
            actions.add(action);
        }
    }
    
    /**
     * Unregister an action
     * @param action Action to remove
     */
    public void unregisterAction(QuickAction action) {
        actions.remove(action);
    }
    
    /**
     * Get all registered actions
     * @return List of actions
     */
    public List<QuickAction> getActions() {
        return new ArrayList<>(actions);
    }
    
    /**
     * Clear all actions
     */
    public void clearActions() {
        actions.clear();
    }
    
    /**
     * Reset to default actions
     */
    public void resetToDefaults() {
        clearActions();
        registerDefaultActions();
    }
}
