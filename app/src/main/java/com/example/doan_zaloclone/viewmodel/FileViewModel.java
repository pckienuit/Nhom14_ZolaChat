package com.example.doan_zaloclone.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doan_zaloclone.models.FileCategory;
import com.example.doan_zaloclone.models.FileItem;
import com.example.doan_zaloclone.models.SenderInfo;
import com.example.doan_zaloclone.repository.FileRepository;
import com.example.doan_zaloclone.utils.Resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ViewModel for file management
 * Manages loading and categorizing files for display
 */
public class FileViewModel extends BaseViewModel {
    
    // Media type filter enum
    public enum MediaTypeFilter {
        ALL,
        IMAGES_ONLY,
        VIDEOS_ONLY
    }
    
    // File type filter enum
    public enum FileTypeFilter {
        ALL,
        PDF,
        WORD,
        EXCEL,
        POWERPOINT,
        ARCHIVE
    }
    
    private static final int PAGE_SIZE = 50;
    
    private final FileRepository fileRepository;
    
    // LiveData for each category
    private final MutableLiveData<Resource<List<FileItem>>> mediaFilesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<FileItem>>> filesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<FileItem>>> linksLiveData = new MutableLiveData<>();
    
    // Combined LiveData for all files
    private final MutableLiveData<Resource<List<FileItem>>> allFilesLiveData = new MutableLiveData<>();
    
    // Filter state
    private final MutableLiveData<String> searchQueryLiveData = new MutableLiveData<>("");
    private final MutableLiveData<Set<String>> selectedSendersLiveData = new MutableLiveData<>(new HashSet<>());
    private final MutableLiveData<Long> filterStartDateLiveData = new MutableLiveData<>(null);
    private final MutableLiveData<Long> filterEndDateLiveData = new MutableLiveData<>(null);
    private final MutableLiveData<MediaTypeFilter> mediaTypeFilterLiveData = new MutableLiveData<>(MediaTypeFilter.ALL);
    private final MutableLiveData<FileTypeFilter> fileTypeFilterLiveData = new MutableLiveData<>(FileTypeFilter.ALL);
    private final MutableLiveData<Set<String>> selectedDomainsLiveData = new MutableLiveData<>(new HashSet<>());
    
    // Filtered LiveData (combines original data + filters)
    private final MediatorLiveData<Resource<List<FileItem>>> filteredMediaFilesLiveData = new MediatorLiveData<>();
    private final MediatorLiveData<Resource<List<FileItem>>> filteredFilesLiveData = new MediatorLiveData<>();
    private final MediatorLiveData<Resource<List<FileItem>>> filteredLinksLiveData = new MediatorLiveData<>();
    
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
        setupFilteredLiveData();
    }
    
    /**
     * Setup MediatorLiveData to combine original data with filters
     */
    private void setupFilteredLiveData() {
        // Media files filtering
        filteredMediaFilesLiveData.addSource(mediaFilesLiveData, resource -> 
            filteredMediaFilesLiveData.setValue(applyFilters(resource)));
        filteredMediaFilesLiveData.addSource(searchQueryLiveData, query -> 
            filteredMediaFilesLiveData.setValue(applyFilters(mediaFilesLiveData.getValue())));
        filteredMediaFilesLiveData.addSource(selectedSendersLiveData, senders -> 
            filteredMediaFilesLiveData.setValue(applyFilters(mediaFilesLiveData.getValue())));
        filteredMediaFilesLiveData.addSource(filterStartDateLiveData, date -> 
            filteredMediaFilesLiveData.setValue(applyFilters(mediaFilesLiveData.getValue())));
        filteredMediaFilesLiveData.addSource(filterEndDateLiveData, date -> 
            filteredMediaFilesLiveData.setValue(applyFilters(mediaFilesLiveData.getValue())));
        filteredMediaFilesLiveData.addSource(mediaTypeFilterLiveData, mediaType -> 
            filteredMediaFilesLiveData.setValue(applyFilters(mediaFilesLiveData.getValue())));
        
        // Files filtering
        filteredFilesLiveData.addSource(filesLiveData, resource -> 
            filteredFilesLiveData.setValue(applyFilters(resource)));
        filteredFilesLiveData.addSource(searchQueryLiveData, query -> 
            filteredFilesLiveData.setValue(applyFilters(filesLiveData.getValue())));
        filteredFilesLiveData.addSource(selectedSendersLiveData, senders -> 
            filteredFilesLiveData.setValue(applyFilters(filesLiveData.getValue())));
        filteredFilesLiveData.addSource(filterStartDateLiveData, date -> 
            filteredFilesLiveData.setValue(applyFilters(filesLiveData.getValue())));
        filteredFilesLiveData.addSource(filterEndDateLiveData, date -> 
            filteredFilesLiveData.setValue(applyFilters(filesLiveData.getValue())));
        filteredFilesLiveData.addSource(fileTypeFilterLiveData, fileType -> 
            filteredFilesLiveData.setValue(applyFilters(filesLiveData.getValue())));
        
        // Links filtering
        filteredLinksLiveData.addSource(linksLiveData, resource -> 
            filteredLinksLiveData.setValue(applyFilters(resource)));
        filteredLinksLiveData.addSource(searchQueryLiveData, query -> 
            filteredLinksLiveData.setValue(applyFilters(linksLiveData.getValue())));
        filteredLinksLiveData.addSource(selectedSendersLiveData, senders -> 
            filteredLinksLiveData.setValue(applyFilters(linksLiveData.getValue())));
        filteredLinksLiveData.addSource(filterStartDateLiveData, date -> 
            filteredLinksLiveData.setValue(applyFilters(linksLiveData.getValue())));
        filteredLinksLiveData.addSource(filterEndDateLiveData, date -> 
            filteredLinksLiveData.setValue(applyFilters(linksLiveData.getValue())));
        filteredLinksLiveData.addSource(selectedDomainsLiveData, domains -> 
            filteredLinksLiveData.setValue(applyFilters(linksLiveData.getValue())));
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
    
    // ========== FILTER METHODS ==========
    
    /**
     * Get filtered LiveData for media files
     */
    public LiveData<Resource<List<FileItem>>> getFilteredMediaFilesLiveData() {
        return filteredMediaFilesLiveData;
    }
    
    /**
     * Get filtered LiveData for document files
     */
    public LiveData<Resource<List<FileItem>>> getFilteredFilesLiveData() {
        return filteredFilesLiveData;
    }
    
    /**
     * Get filtered LiveData for links
     */
    public LiveData<Resource<List<FileItem>>> getFilteredLinksLiveData() {
        return filteredLinksLiveData;
    }
    
    /**
     * Set search query for filtering
     */
    public void setSearchQuery(String query) {
        searchQueryLiveData.setValue(query != null ? query : "");
    }
    
    /**
     * Set selected senders for filtering
     */
    public void setSelectedSenders(Set<String> senderIds) {
        selectedSendersLiveData.setValue(senderIds != null ? senderIds : new HashSet<>());
    }
    
    /**
     * Set date range for filtering
     */
    public void setDateRange(Long startDate, Long endDate) {
        filterStartDateLiveData.setValue(startDate);
        filterEndDateLiveData.setValue(endDate);
    }
    
    /**
     * Clear all filters
     */
    public void clearFilters() {
        searchQueryLiveData.setValue("");
        selectedSendersLiveData.setValue(new HashSet<>());
        filterStartDateLiveData.setValue(null);
        filterEndDateLiveData.setValue(null);
        mediaTypeFilterLiveData.setValue(MediaTypeFilter.ALL);
        fileTypeFilterLiveData.setValue(FileTypeFilter.ALL);
        selectedDomainsLiveData.setValue(new HashSet<>());
    }
    
    /**
     * Set media type filter (for MEDIA category only)
     */
    public void setMediaTypeFilter(MediaTypeFilter filter) {
        mediaTypeFilterLiveData.setValue(filter != null ? filter : MediaTypeFilter.ALL);
    }
    
    /**
     * Set file type filter (for FILES category only)
     */
    public void setFileTypeFilter(FileTypeFilter filter) {
        fileTypeFilterLiveData.setValue(filter != null ? filter : FileTypeFilter.ALL);
    }
    
    /**
     * Set selected domains for filtering (for LINKS category only)
     */
    public void setSelectedDomains(Set<String> domains) {
        selectedDomainsLiveData.setValue(domains != null ? domains : new HashSet<>());
    }
    
    /**
     * Apply filters to a Resource<List<FileItem>>
     */
    private Resource<List<FileItem>> applyFilters(Resource<List<FileItem>> resource) {
        if (resource == null || !resource.isSuccess() || resource.getData() == null) {
            return resource;
        }
        
        List<FileItem> items = resource.getData();
        List<FileItem> filtered = new ArrayList<>();
        
        String query = searchQueryLiveData.getValue();
        Set<String> senders = selectedSendersLiveData.getValue();
        Long startDate = filterStartDateLiveData.getValue();
        Long endDate = filterEndDateLiveData.getValue();
        
        for (FileItem item : items) {
            // Apply search filter (case-insensitive filename match)
            if (query != null && !query.isEmpty()) {
                String displayName = item.getDisplayName();
                if (displayName == null || !displayName.toLowerCase().contains(query.toLowerCase())) {
                    continue;
                }
            }
            
            // Apply sender filter
            if (senders != null && !senders.isEmpty()) {
                String senderId = item.getMessage() != null ? item.getMessage().getSenderId() : null;
                if (senderId == null || !senders.contains(senderId)) {
                    continue;
                }
            }
            
            // Apply date range filter
            if (startDate != null || endDate != null) {
                long timestamp = item.getMessage() != null ? item.getMessage().getTimestamp() : 0;
                if (startDate != null &&  timestamp < startDate) {
                    continue;
                }
                if (endDate != null && timestamp > endDate) {
                    continue;
                }
            }
            
            // Apply media type filter (for MEDIA category only)
            MediaTypeFilter mediaFilter = mediaTypeFilterLiveData.getValue();
            if (mediaFilter != null && mediaFilter != MediaTypeFilter.ALL) {
                if (mediaFilter == MediaTypeFilter.IMAGES_ONLY && !item.isImage()) {
                    continue;
                }
                if (mediaFilter == MediaTypeFilter.VIDEOS_ONLY && !item.isVideo()) {
                    continue;
                }
            }
            
            // Apply file type filter (for FILES category only)
            FileTypeFilter fileFilter = fileTypeFilterLiveData.getValue();
            if (fileFilter != null && fileFilter != FileTypeFilter.ALL) {
                boolean matches = false;
                switch (fileFilter) {
                    case PDF:
                        matches = item.isPdf();
                        break;
                    case WORD:
                        matches = item.isWord();
                        break;
                    case EXCEL:
                        matches = item.isExcel();
                        break;
                    case POWERPOINT:
                        matches = item.isPowerPoint();
                        break;
                    case ARCHIVE:
                        matches = item.isArchive();
                        break;
                }
                if (!matches) {
                    continue;
                }
            }
            
            // Apply domain filter (for LINKS category only)
            Set<String> domains = selectedDomainsLiveData.getValue();
            if (domains != null && !domains.isEmpty()) {
                String itemDomain = item.getDomain();
                if (itemDomain == null || !domains.contains(itemDomain)) {
                    continue;
                }
            }
            
            // Item passed all filters
            filtered.add(item);
        }
        
        return Resource.success(filtered);
    }
    
    /**
     * Get unique senders from all files for filter dialog
     */
    public LiveData<List<SenderInfo>> getUniqueSenders() {
        MutableLiveData<List<SenderInfo>> result = new MutableLiveData<>();
        
        // Combine all categories to get unique senders
        List<FileItem> allItems = new ArrayList<>();
        if (currentMediaFiles != null) allItems.addAll(currentMediaFiles);
        if (currentFiles != null) allItems.addAll(currentFiles);
        if (currentLinks != null) allItems.addAll(currentLinks);
        
        // Count files per sender
        Map<String, SenderInfo> senderMap = new HashMap<>();
        for (FileItem item : allItems) {
            if (item.getMessage() == null) continue;
            
            String senderId = item.getMessage().getSenderId();
            String senderName = item.getSenderName();
            String senderAvatar = item.getSenderAvatarUrl();
            
            if (senderId != null) {
                if (senderMap.containsKey(senderId)) {
                    SenderInfo info = senderMap.get(senderId);
                    info.setFileCount(info.getFileCount() + 1);
                } else {
                    senderMap.put(senderId, new SenderInfo(
                        senderId,
                        senderName != null ? senderName : "User",
                        senderAvatar,
                        1
                    ));
                }
            }
        }
        
        result.setValue(new ArrayList<>(senderMap.values()));
        return result;
    }
    
    /**
     * Get unique domains from links for filter dialog
     */
    public LiveData<List<String>> getUniqueDomains() {
        MutableLiveData<List<String>> result = new MutableLiveData<>();
        
        // Get domains from link items only
        List<String> domains = new ArrayList<>();
        if (currentLinks != null) {
            Set<String> uniqueDomains = new HashSet<>();
            for (FileItem item : currentLinks) {
                String domain = item.getDomain();
                if (domain != null && !domain.isEmpty()) {
                    uniqueDomains.add(domain);
                }
            }
            domains = new ArrayList<>(uniqueDomains);
            // Sort alphabetically
            java.util.Collections.sort(domains);
        }
        
        result.setValue(domains);
        return result;
    }
}
