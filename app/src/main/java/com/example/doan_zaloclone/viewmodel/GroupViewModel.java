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
    private MutableLiveData<Resource<Boolean>> updateResult;

    public GroupViewModel() {
        this.chatRepository = new ChatRepository();
        this.createGroupResult = new MutableLiveData<>();
        this.updateResult = new MutableLiveData<>();
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

    /**
     * Update group name
     */
    public void updateGroupName(@NonNull String conversationId, @NonNull String newName) {
        chatRepository.updateGroupName(conversationId, newName)
                .observeForever(resource -> updateResult.setValue(resource));
    }

    /**
     * Update group avatar
     */
    public void updateGroupAvatar(@NonNull String conversationId, @NonNull String avatarUrl) {
        chatRepository.updateGroupAvatar(conversationId, avatarUrl)
                .observeForever(resource -> updateResult.setValue(resource));
    }

    /**
     * Add members to group
     */
    public void addGroupMembers(@NonNull String conversationId, @NonNull List<String> memberIds) {
        chatRepository.addGroupMembers(conversationId, memberIds)
                .observeForever(resource -> updateResult.setValue(resource));
    }

    /**
     * Remove member from group
     */
    public void removeGroupMember(@NonNull String conversationId, @NonNull String memberId) {
        chatRepository.removeGroupMember(conversationId, memberId)
                .observeForever(resource -> updateResult.setValue(resource));
    }

    /**
     * Leave group
     */
    public void leaveGroup(@NonNull String conversationId, @NonNull String userId) {
        chatRepository.leaveGroup(conversationId, userId)
                .observeForever(resource -> updateResult.setValue(resource));
    }

    /**
     * Transfer admin rights to member
     */
    public void transferAdmin(@NonNull String conversationId, @NonNull String newAdminId) {
        chatRepository.transferAdmin(conversationId, newAdminId)
                .observeForever(resource -> updateResult.setValue(resource));
    }

    /**
     * Delete group
     */
    public void deleteGroup(@NonNull String conversationId) {
        chatRepository.deleteGroup(conversationId)
                .observeForever(resource -> updateResult.setValue(resource));
    }

    /**
     * Get LiveData for update operations result
     */
    public LiveData<Resource<Boolean>> getUpdateResult() {
        return updateResult;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        chatRepository.cleanup();
    }
}
