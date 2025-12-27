package com.example.doan_zaloclone.ui.room;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.utils.FileUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adapter for displaying file previews in the file picker bottom sheet
 */
public class FilePreviewAdapter extends RecyclerView.Adapter<FilePreviewAdapter.FileViewHolder> {

    private final List<FileItem> files;
    private final Set<Uri> selectedFiles;
    private final OnSelectionChangedListener listener;
    public FilePreviewAdapter(List<FileItem> files, OnSelectionChangedListener listener) {
        this.files = files != null ? files : new ArrayList<>();
        this.selectedFiles = new HashSet<>();
        this.listener = listener;

        // Select all by default
        for (FileItem file : this.files) {
            selectedFiles.add(file.uri);
        }
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file_preview, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileItem file = files.get(position);
        holder.bind(file, selectedFiles.contains(file.uri));
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public List<FileItem> getSelectedFiles() {
        List<FileItem> selected = new ArrayList<>();
        for (FileItem file : files) {
            if (selectedFiles.contains(file.uri)) {
                selected.add(file);
            }
        }
        return selected;
    }

    public int getSelectedCount() {
        return selectedFiles.size();
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    public static class FileItem {
        public Uri uri;
        public String name;
        public long size;
        public String mimeType;

        public FileItem(Uri uri, String name, long size, String mimeType) {
            this.uri = uri;
            this.name = name;
            this.size = size;
            this.mimeType = mimeType;
        }
    }

    class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView fileIcon;
        TextView fileName;
        TextView fileSize;
        CheckBox fileCheckbox;

        FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileIcon = itemView.findViewById(R.id.fileIcon);
            fileName = itemView.findViewById(R.id.fileName);
            fileSize = itemView.findViewById(R.id.fileSize);
            fileCheckbox = itemView.findViewById(R.id.fileCheckbox);
        }

        void bind(FileItem file, boolean isSelected) {
            fileName.setText(file.name);
            fileSize.setText(FileUtils.formatFileSize(file.size));
            fileCheckbox.setChecked(isSelected);

            // Set icon based on file type
            int iconResId = FileUtils.getFileIcon(file.mimeType);
            fileIcon.setImageResource(iconResId);

            // Handle click on entire item
            itemView.setOnClickListener(v -> toggleSelection(file));
            fileCheckbox.setOnClickListener(v -> toggleSelection(file));
        }

        void toggleSelection(FileItem file) {
            if (selectedFiles.contains(file.uri)) {
                selectedFiles.remove(file.uri);
                fileCheckbox.setChecked(false);
            } else {
                selectedFiles.add(file.uri);
                fileCheckbox.setChecked(true);
            }

            if (listener != null) {
                listener.onSelectionChanged(selectedFiles.size());
            }
        }
    }
}
