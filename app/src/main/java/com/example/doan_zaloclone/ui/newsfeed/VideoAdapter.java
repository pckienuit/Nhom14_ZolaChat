package com.example.doan_zaloclone.ui.newsfeed;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.VideoItem;
import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private List<VideoItem> videoItems;

    public VideoAdapter(List<VideoItem> videoItems) {
        this.videoItems = videoItems;
    }

    public void updateVideos(List<VideoItem> newVideos) {
        this.videoItems.clear();
        this.videoItems.addAll(newVideos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_short, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        VideoItem videoItem = videoItems.get(position);

        // 1. Tải ảnh Thumbnail từ URL đã lấy (Dùng Glide)
        Glide.with(holder.itemView.getContext())
                .load(videoItem.getThumbnailUrl())
                .into(holder.imgThumbnail);

        holder.txtVideoTitle.setText(videoItem.getTitle());

        // 2. Thiết lập WebView (nhưng chưa load ngay để tiết kiệm RAM)
        holder.itemView.setOnClickListener(v -> {
            holder.imgThumbnail.setVisibility(View.GONE);
            holder.imgPlayIcon.setVisibility(View.GONE);
            holder.txtVideoTitle.setVisibility(View.GONE);
            holder.webViewVideo.setVisibility(View.VISIBLE);

            loadYouTubeVideo(holder.webViewVideo, videoItem.getVideoId());
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
        return videoItems.size();
    }

    // Quan trọng: Giải phóng WebView khi item bị cuộn đi để tránh lag/tràn bộ nhớ
    @Override
    public void onViewRecycled(@NonNull VideoViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.webViewVideo != null) {
            holder.webViewVideo.loadUrl("about:blank");
            holder.webViewVideo.setVisibility(View.GONE);
        }
        holder.imgThumbnail.setVisibility(View.VISIBLE);
        holder.imgPlayIcon.setVisibility(View.VISIBLE);
        holder.txtVideoTitle.setVisibility(View.VISIBLE);
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumbnail, imgPlayIcon;
        WebView webViewVideo;
        TextView txtVideoTitle;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumbnail = itemView.findViewById(R.id.imgThumbnail);
            imgPlayIcon = itemView.findViewById(R.id.imgPlayIcon);
            webViewVideo = itemView.findViewById(R.id.webViewVideo);
            txtVideoTitle = itemView.findViewById(R.id.txtVideoTitle);
        }
    }
}
