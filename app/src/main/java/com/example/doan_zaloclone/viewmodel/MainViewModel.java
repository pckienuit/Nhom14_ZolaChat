package com.example.doan_zaloclone.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doan_zaloclone.repository.AuthRepository;
import com.example.doan_zaloclone.utils.Resource;

/**
 * ViewModel for MainActivity
 * Manages logout operations and main app state
 */
public class MainViewModel extends BaseViewModel {

    private final AuthRepository authRepository;
    private final MutableLiveData<Resource<Boolean>> logoutState = new MutableLiveData<>();

    public MainViewModel() {
        this.authRepository = new AuthRepository();
    }

    /**
     * Get logout state LiveData
     *
     * @return LiveData containing logout operation status
     */
    public LiveData<Resource<Boolean>> getLogoutState() {
        return logoutState;
    }

    /**
     * Perform logout operation
     */
    public void logout() {
        logoutState.setValue(Resource.loading());

        authRepository.logout(new AuthRepository.LogoutCallback() {
            @Override
            public void onLogoutComplete() {
                logoutState.setValue(Resource.success(true));
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up resources if needed
    }
}
