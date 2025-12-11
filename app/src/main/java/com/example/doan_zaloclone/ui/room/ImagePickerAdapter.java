package com.example.doan_zaloclone.ui.room;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doan_zaloclone.R;

import java.util.ArrayList;
import java.util.List;

public class ImagePickerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final int TYPE_CAMERA = 0;
    private static final int TYPE_PHOTO = 1;
    
    private final List<Uri> allPhotos;
    private final List<Uri> selectedPhotos = new ArrayList<>();
    private final OnItemClickListener listener;
    
    public interface OnItemClickListener {
        void onCameraClick();
        void onPhotoClick(Uri photoUri, int position);
    }
    
    public ImagePickerAdapter(List<Uri> photos, OnItemClickListener listener) {
        this.allPhotos = photos;
        this.listener = listener;
    }
    
    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_CAMERA : TYPE_PHOTO;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_CAMERA) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_picker_camera, parent, false);
            return new CameraViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_picker_photo, parent, false);
            return new PhotoViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof PhotoViewHolder) {
            Uri photoUri = allPhotos.get(position - 1); // -1 because first item is camera
            ((PhotoViewHolder) holder).bind(photoUri, position);
        } else if (holder instanceof CameraViewHolder) {
            ((CameraViewHolder) holder).bind();
        }
    }
    
    @Override
    public int getItemCount() {
        return allPhotos.size() + 1; // +1 for camera button
    }
    
    public List<Uri> getSelectedPhotos() {
        return new ArrayList<>(selectedPhotos);
    }
    
    public void clearSelection() {
        selectedPhotos.clear();
        notifyDataSetChanged();
    }
    
    // Camera ViewHolder
    class CameraViewHolder extends RecyclerView.ViewHolder {
        
        public CameraViewHolder(@NonNull View itemView) {
            super(itemView);
        }
        
        public void bind() {
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCameraClick();
                }
            });
        }
    }
    
    // Photo ViewHolder
    class PhotoViewHolder extends RecyclerView.ViewHolder {
        private final ImageView photoImageView;
        private final View selectionOverlay;
        private final View checkboxBackground;
        private final TextView selectionNumber;
        
        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            photoImageView = itemView.findViewById(R.id.photoImageView);
            selectionOverlay = itemView.findViewById(R.id.selectionOverlay);
            checkboxBackground = itemView.findViewById(R.id.checkboxBackground);
            selectionNumber = itemView.findViewById(R.id.selectionNumber);
        }
        
        public void bind(Uri photoUri, int position) {
            // Load photo with Glide
            Glide.with(itemView.getContext())
                    .load(photoUri)
                    .centerCrop()
                    .into(photoImageView);
            
            // Check if selected
            int selectionIndex = selectedPhotos.indexOf(photoUri);
            boolean isSelected = selectionIndex != -1;
            
            // Update UI based on selection state
            selectionOverlay.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            checkboxBackground.setSelected(isSelected);
            
            if (isSelected) {
                selectionNumber.setVisibility(View.VISIBLE);
                selectionNumber.setText(String.valueOf(selectionIndex + 1));
            } else {
                selectionNumber.setVisibility(View.GONE);
            }
            
            // Handle click
            itemView.setOnClickListener(v -> {
                if (isSelected) {
                    // Deselect
                    selectedPhotos.remove(photoUri);
                } else {
                    // Select
                    selectedPhotos.add(photoUri);
                }
                notifyDataSetChanged(); // Refresh to update numbers
                
                if (listener != null) {
                    listener.onPhotoClick(photoUri, position);
                }
            });
        }
    }
}
