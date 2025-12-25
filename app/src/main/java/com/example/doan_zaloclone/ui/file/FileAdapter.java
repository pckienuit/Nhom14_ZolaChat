package com.example.doan_zaloclone.ui.file;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.FileCategory;
import com.example.doan_zaloclone.models.FileItem;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying files in different layouts
 * Supports grid layout for media and list layout for files/links
 */
public class FileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final FileCategory category;
    private List<FileItem> fileItems = new ArrayList<>();
    private OnItemClickListener clickListener;

    public FileAdapter(FileCategory category) {
        this.category = category;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (category) {
            case MEDIA:
                View mediaView = inflater.inflate(R.layout.item_file_media, parent, false);
                return new MediaViewHolder(mediaView);
            case FILES:
                View fileView = inflater.inflate(R.layout.item_file_document, parent, false);
                return new DocumentViewHolder(fileView);
            case LINKS:
                View linkView = inflater.inflate(R.layout.item_file_link, parent, false);
                return new LinkViewHolder(linkView);
            default:
                throw new IllegalArgumentException("Unknown category: " + category);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        FileItem item = fileItems.get(position);

        if (holder instanceof MediaViewHolder) {
            ((MediaViewHolder) holder).bind(item, clickListener);
        } else if (holder instanceof DocumentViewHolder) {
            ((DocumentViewHolder) holder).bind(item, clickListener);
        } else if (holder instanceof LinkViewHolder) {
            ((LinkViewHolder) holder).bind(item, clickListener);
        }
    }

    @Override
    public int getItemCount() {
        return fileItems.size();
    }

    /**
     * Update the entire list of files
     */
    public void updateFiles(List<FileItem> newFiles) {
        this.fileItems = newFiles != null ? new ArrayList<>(newFiles) : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * Append more files for pagination
     */
    public void appendFiles(List<FileItem> moreFiles) {
        if (moreFiles == null || moreFiles.isEmpty()) return;

        int startPosition = fileItems.size();
        fileItems.addAll(moreFiles);
        notifyItemRangeInserted(startPosition, moreFiles.size());
    }

    /**
     * Set click listener for file items
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    // ========== ViewHolders ==========

    public interface OnItemClickListener {
        void onItemClick(FileItem item, int position);
    }

    /**
     * ViewHolder for media items (images/videos) in grid layout
     */
    static class MediaViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnailImageView;
        TextView videoDurationTextView;
        TextView senderNameTextView;
        TextView dateTextView;

        MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnailImageView = itemView.findViewById(R.id.thumbnailImageView);
            videoDurationTextView = itemView.findViewById(R.id.videoDurationTextView);
            senderNameTextView = itemView.findViewById(R.id.senderNameTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
        }

        void bind(FileItem item, OnItemClickListener clickListener) {
            // Load thumbnail
            String imageUrl = item.getFileUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .thumbnail(0.1f)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_file)
                        .error(R.drawable.ic_file)
                        .centerCrop()
                        .into(thumbnailImageView);
            } else {
                thumbnailImageView.setImageResource(R.drawable.ic_file);
            }

            // Show video duration if video
            if (item.isVideo()) {
                videoDurationTextView.setVisibility(View.VISIBLE);
                // TODO: Extract actual video duration
                videoDurationTextView.setText("Video");
            } else {
                videoDurationTextView.setVisibility(View.GONE);
            }

            // Sender name
            String senderName = item.getSenderName();
            senderNameTextView.setText(senderName != null ? senderName : "User");

            // Date
            dateTextView.setText(item.getFormattedDate());

            // Click listener
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onItemClick(item, getAdapterPosition());
                }
            });
        }
    }

    /**
     * ViewHolder for document files in list layout
     */
    static class DocumentViewHolder extends RecyclerView.ViewHolder {
        ImageView fileIconImageView;
        TextView fileNameTextView;
        TextView fileInfoTextView;
        TextView senderNameTextView;
        TextView dateTextView;

        DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            fileIconImageView = itemView.findViewById(R.id.fileIconImageView);
            fileNameTextView = itemView.findViewById(R.id.fileNameTextView);
            fileInfoTextView = itemView.findViewById(R.id.fileInfoTextView);
            senderNameTextView = itemView.findViewById(R.id.senderNameTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
        }

        void bind(FileItem item, OnItemClickListener clickListener) {
            // File name
            fileNameTextView.setText(item.getDisplayName());

            // File info (type and size)
            String fileType = item.getFileTypeLabel();
            String fileSize = item.getFormattedFileSize();
            fileInfoTextView.setText(fileType + " • " + fileSize);

            // File icon based on type
            int iconRes = getFileIcon(item);
            fileIconImageView.setImageResource(iconRes);

            // Sender name
            String senderName = item.getSenderName();
            senderNameTextView.setText(senderName != null ? senderName : "User");

            // Date
            dateTextView.setText(item.getFormattedDate());

            // Click listener
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onItemClick(item, getAdapterPosition());
                }
            });
        }

        private int getFileIcon(FileItem item) {
            String fileType = item.getFileTypeLabel().toLowerCase();

            if (fileType.contains("pdf")) {
                return R.drawable.ic_file_pdf;
            } else if (fileType.contains("doc")) {
                return R.drawable.ic_file_word;
            } else if (fileType.contains("xls") || fileType.contains("xlsx")) {
                return R.drawable.ic_file_excel;
            } else if (fileType.contains("zip") || fileType.contains("rar")) {
                // Use generic file icon for ZIP files
                return R.drawable.ic_file;
            } else if (fileType.contains("ppt") || fileType.contains("pptx")) {
                return R.drawable.ic_file_powerpoint;
            } else {
                return R.drawable.ic_file;
            }
        }
    }

    // ========== Click Listener Interface ==========

    /**
     * ViewHolder for link items in list layout
     */
    static class LinkViewHolder extends RecyclerView.ViewHolder {
        ImageView linkIconImageView;
        TextView linkTitleTextView;
        TextView linkUrlTextView;
        TextView senderNameTextView;
        TextView dateTextView;

        LinkViewHolder(@NonNull View itemView) {
            super(itemView);
            linkIconImageView = itemView.findViewById(R.id.linkIconImageView);
            linkTitleTextView = itemView.findViewById(R.id.linkTitleTextView);
            linkUrlTextView = itemView.findViewById(R.id.linkUrlTextView);
            senderNameTextView = itemView.findViewById(R.id.senderNameTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
        }

        void bind(FileItem item, OnItemClickListener clickListener) {
            // Link title (domain)
            linkTitleTextView.setText(item.getDisplayName());

            // Full URL
            String url = item.getExtractedUrl();
            linkUrlTextView.setText(url != null ? url : "");

            // Sender name
            String senderName = item.getSenderName();
            senderNameTextView.setText(senderName != null ? senderName : "User");

            // Date
            dateTextView.setText(item.getFormattedDate());

            // Click listener
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onItemClick(item, getAdapterPosition());
                }
            });
        }
    }
}
