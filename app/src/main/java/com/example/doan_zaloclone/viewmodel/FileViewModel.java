package com.example.doan_zaloclone.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doan_zaloclone.models.FileCategory;
import com.example.doan_zaloclone.models.FileItem;
import com.example.doan_zaloclone.repository.FileRepository;
import com.example.doan_zaloclone.utils.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ViewModel for file management
 * Manages loading and categorizing files for display
 */
public class FileViewModel extends BaseViewModel {
    
    private static final int PAGE_SIZE = 50;
    
    private final FileRepository fileRepository;
    
    // LiveData for each category
    private final MutableLiveData<Resource<List<FileItem>>> mediaFilesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<FileItem>>> filesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<FileItem>>> linksLiveData = new MutableLiveData<>();
    
    // Combined LiveData for all files
    private final MutableLiveData<Resource<List<FileItem>>> allFilesLiveData = new MutableLiveData<>();
    
    // Pagination tracking
    private long lastMediaTimestamp = Long.MAX_VALUE;
    private long lastFilesTimestamp = Long.MAX_VALUE;
    private long lastLinksTimestamp = Long.MAX_VALUE;
    
    private boolean hasMoreMedia = true;
    private boolean hasMoreFiles = true;
    private boolean hasMoreLinks = true;
    
    // Current lists
    private List<FileItem> currentMediaFiles = new ArrayList<>();
    private List<FileItem> currentFiles = new ArrayList<>();
    private List<FileItem> currentLinks = new ArrayList<>();
    
    public FileViewModel() {
        this.fileRepository = new FileRepository();
    }
    
    /**
     * Load all files for a conversation
     * @param conversationId ID of the conversation
     */
    public void loadFiles(@NonNull String conversationId) {
        // Set loading state
        mediaFilesLiveData.setValue(Resource.loading(null));
        filesLiveData.setValue(Resource.loading(null));
        linksLiveData.setValue(Resource.loading(null));
        
        // Reset pagination
        lastMediaTimestamp = Long.MAX_VALUE;
        lastFilesTimestamp = Long.MAX_VALUE;
        lastLinksTimestamp = Long.MAX_VALUE;
        hasMoreMedia = true;
        hasMoreFiles = true;
        hasMoreLinks = true;
        currentMediaFiles.clear();
        currentFiles.clear();
        currentLinks.clear();
        
        // Load files from repository
        LiveData<Resource<List<FileItem>>> filesResource = 
                fileRepository.getFilesForConversation(conversationId, PAGE_SIZE);
        
        filesResource.observeForever(resource -> {
            if (resource == null) return;
            
            if (resource.isSuccess() && resource.getData() != null) {
                List<FileItem> allFiles = resource.getData();
                
                // Categorize files
                Map<FileCategory, List<FileItem>> categorized = 
                        fileRepository.categorizeFiles(allFiles);
                
                // Update category-specific LiveData
                List<FileItem> mediaFiles = categorized.get(FileCategory.MEDIA);
                List<FileItem> files = categorized.get(FileCategory.FILES);
                List<FileItem> links = categorized.get(FileCategory.LINKS);
                
                if (mediaFiles != null) {
                    currentMediaFiles = new ArrayList<>(mediaFiles);
                    mediaFilesLiveData.setValue(Resource.success(currentMediaFiles));
                    updateLastTimestamp(mediaFiles, ts -> lastMediaTimestamp = ts);
                    hasMoreMedia = mediaFiles.size() >= PAGE_SIZE;
                } else {
                    mediaFilesLiveData.setValue(Resource.success(new ArrayList<>()));
                }
                
                if (files != null) {
                    currentFiles = new ArrayList<>(files);
                    filesLiveData.setValue(Resource.success(currentFiles));
                    updateLastTimestamp(files, ts -> lastFilesTimestamp = ts);
                    hasMoreFiles = files.size() >= PAGE_SIZE;
                } else {
                    filesLiveData.setValue(Resource.success(new ArrayList<>()));
                }
                
                if (links != null) {
                    currentLinks = new ArrayList<>(links);
                    linksLiveData.setValue(Resource.success(currentLinks));
                    updateLastTimestamp(links, ts -> lastLinksTimestamp = ts);
                    hasMoreLinks = links.size() >= PAGE_SIZE;
                } else {
                    linksLiveData.setValue(Resource.success(new ArrayList<>()));
                }
                
                // Update all files LiveData
                allFilesLiveData.setValue(Resource.success(allFiles));
                
            } else if (resource.isError()) {
                String error = resource.getMessage();
                mediaFilesLiveData.setValue(Resource.error(error, null));
                filesLiveData.setValue(Resource.error(error, null));
                linksLiveData.setValue(Resource.error(error, null));
                allFilesLiveData.setValue(Resource.error(error, null));
            }
        });
    }
    
    /**
     * Load more files for a specific category
     * @param conversationId ID of the conversation
     * @param category Category to load more files for
     */
    public void loadMoreFiles(@NonNull String conversationId, @NonNull FileCategory category) {
        long lastTimestamp;
        boolean hasMore;
        
        switch (category) {
            case MEDIA:
                lastTimestamp = lastMediaTimestamp;
                hasMore = hasMoreMedia;
                break;
            case FILES:
                lastTimestamp = lastFilesTimestamp;
                hasMore = hasMoreFiles;
                break;
            case LINKS:
                lastTimestamp = lastLinksTimestamp;
                hasMore = hasMoreLinks;
                break;
            default:
                return;
        }
        
        if (!hasMore || lastTimestamp == Long.MAX_VALUE) {
            return; // No more items to load
        }
        
        // Load more from repository
        LiveData<Resource<List<FileItem>>> moreFilesResource = 
                fileRepository.loadMoreFiles(conversationId, lastTimestamp, PAGE_SIZE);
        
        moreFilesResource.observeForever(resource -> {
            if (resource == null) return;
            
            if (resource.isSuccess() && resource.getData() != null) {
                List<FileItem> moreFiles = resource.getData();
                
                // Categorize new files
                Map<FileCategory, List<FileItem>> categorized = 
                        fileRepository.categorizeFiles(moreFiles);
                
                List<FileItem> categoryFiles = categorized.get(category);
                if (categoryFiles != null && !categoryFiles.isEmpty()) {
                    // Append to current list
                    switch (category) {
                        case MEDIA:
                            currentMediaFiles.addAll(categoryFiles);
                            mediaFilesLiveData.setValue(Resource.success(new ArrayList<>(currentMediaFiles)));
                            updateLastTimestamp(categoryFiles, ts -> lastMediaTimestamp = ts);
                            hasMoreMedia = categoryFiles.size() >= PAGE_SIZE;
                            break;
                        case FILES:
                            currentFiles.addAll(categoryFiles);
                            filesLiveData.setValue(Resource.success(new ArrayList<>(currentFiles)));
                            updateLastTimestamp(categoryFiles, ts -> lastFilesTimestamp = ts);
                            hasMoreFiles = categoryFiles.size() >= PAGE_SIZE;
                            break;
                        case LINKS:
                            currentLinks.addAll(categoryFiles);
                            linksLiveData.setValue(Resource.success(new ArrayList<>(currentLinks)));
                            updateLastTimestamp(categoryFiles, ts -> lastLinksTimestamp = ts);
                            hasMoreLinks = categoryFiles.size() >= PAGE_SIZE;
                            break;
                    }
                } else {
                    // No more items in this category
                    switch (category) {
                        case MEDIA:
                            hasMoreMedia = false;
                            break;
                        case FILES:
                            hasMoreFiles = false;
                            break;
                        case LINKS:
                            hasMoreLinks = false;
                            break;
                    }
                }
            }
        });
    }
    
    /**
     * Refresh files (for pull-to-refresh)
     * @param conversationId ID of the conversation
     */
    public void refreshFiles(@NonNull String conversationId) {
        loadFiles(conversationId);
    }
    
    // Getters for LiveData
    public LiveData<Resource<List<FileItem>>> getMediaFilesLiveData() {
        return mediaFilesLiveData;
    }
    
    public LiveData<Resource<List<FileItem>>> getFilesLiveData() {
        return filesLiveData;
    }
    
    public LiveData<Resource<List<FileItem>>> getLinksLiveData() {
        return linksLiveData;
    }
    
    public LiveData<Resource<List<FileItem>>> getAllFilesLiveData() {
        return allFilesLiveData;
    }
    
    /**
     * Check if more items can be loaded for a category
     */
    public boolean hasMore(FileCategory category) {
        switch (category) {
            case MEDIA:
                return hasMoreMedia;
            case FILES:
                return hasMoreFiles;
            case LINKS:
                return hasMoreLinks;
            default:
                return false;
        }
    }
    
    // Helper to update last timestamp
    private void updateLastTimestamp(List<FileItem> items, TimestampUpdater updater) {
        if (items != null && !items.isEmpty()) {
            FileItem lastItem = items.get(items.size() - 1);
            if (lastItem.getMessage() != null) {
                updater.update(lastItem.getMessage().getTimestamp());
            }
        }
    }
    
    @FunctionalInterface
    private interface TimestampUpdater {
        void update(long timestamp);
    }
}
