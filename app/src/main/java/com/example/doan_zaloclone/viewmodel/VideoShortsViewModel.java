package com.example.doan_zaloclone.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.doan_zaloclone.data.repository.YoutubeRepository;
import com.example.doan_zaloclone.models.VideoItem;
import com.example.doan_zaloclone.utils.Resource;
import java.util.List;

public class VideoShortsViewModel extends ViewModel {
    private final YoutubeRepository youtubeRepository;

    public VideoShortsViewModel() {
        this.youtubeRepository = YoutubeRepository.getInstance();
    }

    public LiveData<Resource<List<VideoItem>>> searchShorts(String query) {
        return youtubeRepository.searchShorts(query);
    }
}
