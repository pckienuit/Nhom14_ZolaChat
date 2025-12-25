package com.example.doan_zaloclone.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.doan_zaloclone.models.Conversation;
import com.example.doan_zaloclone.repository.ChatRepository;
import com.example.doan_zaloclone.repository.ConversationRepository;
import com.example.doan_zaloclone.utils.Resource;

import java.util.List;

/**
 * ViewModel for group-related operations
 */
public class GroupViewModel extends ViewModel {

    private final ChatRepository chatRepository;
    private final ConversationRepository conversationRepository;
    private final MutableLiveData<Resource<Conversation>> createGroupResult;
    private final MutableLiveData<Resource<Boolean>> updateResult;
    private final MutableLiveData<Resource<Void>> leaveGroupResult;
    private final MutableLiveData<Resource<Boolean>> deleteGroupResult;

    public GroupViewModel() {
        this.chatRepository = new ChatRepository();
        this.conversationRepository = ConversationRepository.getInstance();
        this.createGroupResult = new MutableLiveData<>();
        this.updateResult = new MutableLiveData<>();
        this.leaveGroupResult = new MutableLiveData<>();
        this.deleteGroupResult = new MutableLiveData<>();
    }

    /**
     * Create a new group conversation
     *
     * @param adminId   ID of the group creator
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
     * Leave group (migrated to API)
     */
    public void leaveGroup(@NonNull String conversationId) {
        conversationRepository.leaveGroup(conversationId)
                .observeForever(resource -> leaveGroupResult.setValue(resource));
    }

    /**
     * Get LiveData for leave group result
     */
    public LiveData<Resource<Void>> getLeaveGroupResult() {
        return leaveGroupResult;
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
                .observeForever(resource -> deleteGroupResult.setValue(resource));
    }

    /**
     * Get LiveData for delete group result
     */
    public LiveData<Resource<Boolean>> getDeleteGroupResult() {
        return deleteGroupResult;
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
