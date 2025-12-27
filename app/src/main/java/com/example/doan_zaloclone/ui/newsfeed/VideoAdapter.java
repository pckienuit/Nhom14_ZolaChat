package com.example.doan_zaloclone.ui.newsfeed;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.doan_zaloclone.R;
import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private List<String> videoIds;

    public VideoAdapter(List<String> videoIds) {
        this.videoIds = videoIds;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_short, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        String videoId = videoIds.get(position);

        // 1. Tải ảnh Thumbnail từ YouTube API (Dùng Glide)
        String thumbnailUrl = "https://img.youtube.com/vi/" + videoId + "/0.jpg";
        Glide.with(holder.itemView.getContext())
                .load(thumbnailUrl)
                .into(holder.imgThumbnail);

        // 2. Thiết lập WebView (nhưng chưa load ngay để tiết kiệm RAM)
        holder.itemView.setOnClickListener(v -> {
            holder.imgThumbnail.setVisibility(View.GONE);
            holder.imgPlayIcon.setVisibility(View.GONE);
            holder.webViewVideo.setVisibility(View.VISIBLE);

            loadYouTubeVideo(holder.webViewVideo, videoId);
        });
    }

    private void loadYouTubeVideo(WebView webView, String videoId) {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());

        // Chuỗi HTML để nhúng video mượt mà
        String html = "<html><body style=\"margin:0;padding:0;\">" +
                "<iframe width=\"100%\" height=\"100%\" src=\"https://www.youtube.com/embed/" + videoId +
                "?autoplay=1&modestbranding=1\" frameborder=\"0\" allowfullscreen></iframe>" +
                "</body></html>";

        webView.loadData(html, "text/html", "utf-8");
    }

    @Override
    public int getItemCount() {
        return videoIds.size();
    }

    // Quan trọng: Giải phóng WebView khi item bị cuộn đi để tránh lag/tràn bộ nhớ
    @Override
    public void onViewRecycled(@NonNull VideoViewHolder holder) {
        super.onViewRecycled(holder);
        holder.webViewVideo.loadUrl("about:blank");
        holder.webViewVideo.setVisibility(View.GONE);
        holder.imgThumbnail.setVisibility(View.VISIBLE);
        holder.imgPlayIcon.setVisibility(View.VISIBLE);
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumbnail, imgPlayIcon;
        WebView webViewVideo;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumbnail = itemView.findViewById(R.id.imgThumbnail);
            imgPlayIcon = itemView.findViewById(R.id.imgPlayIcon);
            webViewVideo = itemView.findViewById(R.id.webViewVideo);
        }
    }
}
