package com.example.doan_zaloclone.utils;

import android.content.Context;
import android.content.Intent;

import com.example.doan_zaloclone.ui.personal.ProfileCardActivity;
import com.google.firebase.auth.FirebaseAuth;

/**
 * ProfileNavigator - Utility class for navigating to user profiles
 */
public class ProfileNavigator {

    /**
     * Open own profile (editable)
     */
    public static void openOwnProfile(Context context) {
        Intent intent = new Intent(context, ProfileCardActivity.class);
        intent.putExtra(ProfileCardActivity.EXTRA_IS_EDITABLE, true);
        context.startActivity(intent);
    }

    /**
     * Open another user's profile (read-only)
     */
    public static void openUserProfile(Context context, String userId) {
        // Don't open if it's current user's ID
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null &&
                auth.getCurrentUser().getUid().equals(userId)) {
            openOwnProfile(context);
            return;
        }

        Intent intent = new Intent(context, ProfileCardActivity.class);
        intent.putExtra(ProfileCardActivity.EXTRA_USER_ID, userId);
        intent.putExtra(ProfileCardActivity.EXTRA_IS_EDITABLE, false);
        context.startActivity(intent);
    }
}
