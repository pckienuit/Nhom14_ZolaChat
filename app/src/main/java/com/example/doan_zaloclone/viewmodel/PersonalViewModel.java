package com.example.doan_zaloclone.viewmodel;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.doan_zaloclone.models.User;
import com.example.doan_zaloclone.repository.UserRepository;
import com.example.doan_zaloclone.utils.Resource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Map;

public class PersonalViewModel extends ViewModel {

    private final UserRepository userRepository;
    private final MutableLiveData<Resource<User>> currentUser = new MutableLiveData<>();
    private final MutableLiveData<Resource<Boolean>> updateProfileState = new MutableLiveData<>();
    private final MutableLiveData<Resource<String>> uploadImageState = new MutableLiveData<>();

    public PersonalViewModel() {
        this.userRepository = new UserRepository();
        // Don't auto-load - let caller decide which user to load
    }

    public LiveData<Resource<User>> getCurrentUser() {
        return currentUser;
    }

    public LiveData<Resource<Boolean>> getUpdateProfileState() {
        return updateProfileState;
    }

    public LiveData<Resource<String>> getUploadImageState() {
        return uploadImageState;
    }


    /**
     * Load current logged-in user profile
     */
    public void loadCurrentUser() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            currentUser.setValue(Resource.error("User not logged in"));
            return;
        }

        loadUser(firebaseUser.getUid());
    }

    /**
     * Load any user's profile by userId
     */
    public void loadUser(String userId) {
        if (userId == null || userId.isEmpty()) {
            currentUser.setValue(Resource.error("Invalid user ID"));
            return;
        }

        currentUser.setValue(Resource.loading());

        userRepository.getUserById(userId, new UserRepository.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User user) {
                currentUser.setValue(Resource.success(user));
            }

            @Override
            public void onError(String error) {
                currentUser.setValue(Resource.error(error));
            }
        });
    }

    /**
     * Update user profile name
     */
    public void updateName(String newName) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            updateProfileState.setValue(Resource.error("User not logged in"));
            return;
        }

        if (newName == null || newName.trim().isEmpty()) {
            updateProfileState.setValue(Resource.error("Tên không được để trống"));
            return;
        }

        updateProfileState.setValue(Resource.loading());

        userRepository.updateUserName(firebaseUser.getUid(), newName.trim(), new UserRepository.OnUpdateListener() {
            @Override
            public void onSuccess() {
                updateProfileState.setValue(Resource.success(true));
                loadCurrentUser(); // Reload user data
            }

            @Override
            public void onError(String error) {
                updateProfileState.setValue(Resource.error(error));
            }
        });
    }

    /**
     * Update user bio
     */
    public void updateBio(String newBio) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            updateProfileState.setValue(Resource.error("User not logged in"));
            return;
        }

        // Validate bio length (max 200 characters)
        if (newBio != null && newBio.length() > 200) {
            updateProfileState.setValue(Resource.error("Bio không được vượt quá 200 ký tự"));
            return;
        }

        updateProfileState.setValue(Resource.loading());

        userRepository.updateUserBio(firebaseUser.getUid(), newBio != null ? newBio.trim() : "",
                new UserRepository.OnUpdateListener() {
                    @Override
                    public void onSuccess() {
                        updateProfileState.setValue(Resource.success(true));
                        loadCurrentUser(); // Reload user data
                    }

                    @Override
                    public void onError(String error) {
                        updateProfileState.setValue(Resource.error(error));
                    }
                });
    }

    /**
     * Upload avatar image to Cloudinary and update user profile
     */
    public void uploadAvatar(Uri imageUri) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            uploadImageState.setValue(Resource.error("User not logged in"));
            return;
        }

        uploadImageState.setValue(Resource.loading());

        String userId = firebaseUser.getUid();

        try {
            MediaManager.get().upload(imageUri)
                    .option("folder", "zalo_profile/avatars/" + userId)
                    .option("public_id", "avatar_" + System.currentTimeMillis())
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            android.util.Log.d("PersonalViewModel", "Avatar upload started");
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                            // Optional: track progress
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String avatarUrl = (String) resultData.get("secure_url");

                            // Update user avatar URL in Firestore
                            userRepository.updateUserAvatar(userId, avatarUrl, new UserRepository.OnUpdateListener() {
                                @Override
                                public void onSuccess() {
                                    uploadImageState.setValue(Resource.success(avatarUrl));
                                    loadCurrentUser(); // Reload user data
                                }

                                @Override
                                public void onError(String error) {
                                    uploadImageState.setValue(Resource.error("Failed to update avatar: " + error));
                                }
                            });
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            uploadImageState.setValue(Resource.error("Upload failed: " + error.getDescription()));
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            android.util.Log.d("PersonalViewModel", "Avatar upload rescheduled");
                        }
                    })
                    .dispatch();
        } catch (Exception e) {
            uploadImageState.setValue(Resource.error("Failed to start upload: " + e.getMessage()));
        }
    }

    /**
     * Upload cover image to Cloudinary and update user profile
     */
    public void uploadCover(Uri imageUri) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            uploadImageState.setValue(Resource.error("User not logged in"));
            return;
        }

        uploadImageState.setValue(Resource.loading());

        String userId = firebaseUser.getUid();

        try {
            MediaManager.get().upload(imageUri)
                    .option("folder", "zalo_profile/covers/" + userId)
                    .option("public_id", "cover_" + System.currentTimeMillis())
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            android.util.Log.d("PersonalViewModel", "Cover upload started");
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                            // Optional: track progress
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String coverUrl = (String) resultData.get("secure_url");

                            // Update user cover URL in Firestore
                            userRepository.updateUserCover(userId, coverUrl, new UserRepository.OnUpdateListener() {
                                @Override
                                public void onSuccess() {
                                    uploadImageState.setValue(Resource.success(coverUrl));
                                    loadCurrentUser(); // Reload user data
                                }

                                @Override
                                public void onError(String error) {
                                    uploadImageState.setValue(Resource.error("Failed to update cover: " + error));
                                }
                            });
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            uploadImageState.setValue(Resource.error("Upload failed: " + error.getDescription()));
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            android.util.Log.d("PersonalViewModel", "Cover upload rescheduled");
                        }
                    })
                    .dispatch();
        } catch (Exception e) {
            uploadImageState.setValue(Resource.error("Failed to start upload: " + e.getMessage()));
        }
    }
}
