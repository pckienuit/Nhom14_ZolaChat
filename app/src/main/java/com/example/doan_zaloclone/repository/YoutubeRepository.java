package com.example.doan_zaloclone.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.doan_zaloclone.BuildConfig;
import com.example.doan_zaloclone.models.VideoItem;
import com.example.doan_zaloclone.utils.Resource;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class YoutubeRepository {
    private static YoutubeRepository instance;
    private final YouTube youtubeService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static synchronized YoutubeRepository getInstance() {
        if (instance == null) {
            instance = new YoutubeRepository();
        }
        return instance;
    }

    private YoutubeRepository() {
        youtubeService = new YouTube.Builder(
            AndroidHttp.newCompatibleTransport(),
            new GsonFactory(),
            null // No Http-Request-Initializer needed for public API access
        ).setApplicationName("DoAn_ZaloClone").build();
    }

    public LiveData<Resource<List<VideoItem>>> searchShorts(String query) {
        MutableLiveData<Resource<List<VideoItem>>> data = new MutableLiveData<>();
        data.setValue(Resource.loading(null));

        executor.execute(() -> {
            try {
                YouTube.Search.List search = youtubeService.search().list(Collections.singletonList("snippet"));
                
                search.setKey("AIzaSyAswbPiGp-UJkz1LHsT_GeiHkgLdWzq6KI");
                search.setQ(query + " #shorts");
                // SỬA LỖI: Truyền vào một List<String> cho setType theo yêu cầu của trình biên dịch
                search.setType(Collections.singletonList("video"));
                search.setVideoDuration("short"); 
                search.setMaxResults(20L);

                SearchListResponse searchResponse = search.execute();
                List<SearchResult> searchResultList = searchResponse.getItems();
                List<VideoItem> videoItems = new ArrayList<>();

                if (searchResultList != null) {
                    for (SearchResult result : searchResultList) {
                        String videoId = result.getId().getVideoId();
                        String title = result.getSnippet().getTitle();
                        String thumbnailUrl = result.getSnippet().getThumbnails().getHigh().getUrl();
                        videoItems.add(new VideoItem(videoId, title, thumbnailUrl));
                    }
                }
                data.postValue(Resource.success(videoItems));
            } catch (IOException e) {
                e.printStackTrace();
                data.postValue(Resource.error("Lỗi mạng hoặc API Key không hợp lệ: " + e.getMessage(), null));
            }
        });

        return data;
    }
}
