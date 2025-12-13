package com.example.doan_zaloclone.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.doan_zaloclone.models.Conversation;
import com.example.doan_zaloclone.repository.ChatRepository;
import com.example.doan_zaloclone.utils.Resource;

import java.util.List;

/**
 * ViewModel for group-related operations
 */
public class GroupViewModel extends ViewModel {

    private final ChatRepository chatRepository;
    private MutableLiveData<Resource<Conversation>> createGroupResult;

    public GroupViewModel() {
        this.chatRepository = new ChatRepository();
        this.createGroupResult = new MutableLiveData<>();
    }

    /**
     * Create a new group conversation
     * @param adminId ID of the group creator
     * @param groupName Name of the group
     * @param memberIds List of member IDs (including admin)
     */
    public void createGroup(@NonNull String adminId, 
                           @NonNull String groupName, 
                           @NonNull List<String> memberIds) {
        chatRepository.createGroup(adminId, groupName, memberIds)
                .observeForever(resource -> createGroupResult.setValue(resource));
    }

    /**
     * Get LiveData for create group result
     */
    public LiveData<Resource<Conversation>> getCreateGroupResult() {
        return createGroupResult;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        chatRepository.cleanup();
    }
}
