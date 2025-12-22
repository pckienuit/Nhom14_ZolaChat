package com.example.doan_zaloclone.ui.room;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.User;

import java.util.List;

/**
 * Adapter for selecting a single contact to send as business card
 */
public class ContactSelectionAdapter extends RecyclerView.Adapter<ContactSelectionAdapter.ViewHolder> {
    
    private Context context;
    private List<User> friends;
    private OnContactClickListener listener;
    
    public interface OnContactClickListener {
        void onContactClick(User user);
    }
    
    public ContactSelectionAdapter(Context context, List<User> friends, OnContactClickListener listener) {
        this.context = context;
        this.friends = friends;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact_selection, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(friends.get(position));
    }
    
    @Override
    public int getItemCount() {
        return friends.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView avatarImage;
        private TextView nameText;
        private TextView emailText;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImage = itemView.findViewById(R.id.avatarImage);
            nameText = itemView.findViewById(R.id.nameText);
            emailText = itemView.findViewById(R.id.emailText);
        }
        
        public void bind(User user) {
            nameText.setText(user.getName());
            
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                emailText.setText(user.getEmail());
                emailText.setVisibility(View.VISIBLE);
            } else {
                emailText.setVisibility(View.GONE);
            }
            
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                Glide.with(context)
                    .load(user.getAvatarUrl())
                    .placeholder(R.drawable.ic_avatar)
                    .into(avatarImage);
            } else {
                avatarImage.setImageResource(R.drawable.ic_avatar);
            }
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onContactClick(user);
                }
            });
        }
    }
}
