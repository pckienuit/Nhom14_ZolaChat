package com.example.doan_zaloclone.ui.contact;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.FriendRequest;

import java.util.List;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.ViewHolder> {

    private List<FriendRequest> requests;
    private FriendRequestListener listener;

    public interface FriendRequestListener {
        void onAccept(FriendRequest request);
        void onReject(FriendRequest request);
    }

    public FriendRequestAdapter(List<FriendRequest> requests, FriendRequestListener listener) {
        this.requests = requests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_friend_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FriendRequest request = requests.get(position);
        holder.bind(request, listener);
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public void updateRequests(List<FriendRequest> newRequests) {
        android.util.Log.d("FriendRequestAdapter", "updateRequests called. Old size: " + this.requests.size() + ", New size: " + newRequests.size());
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new FriendRequestDiffCallback(this.requests, newRequests));
        this.requests = newRequests;
        diffResult.dispatchUpdatesTo(this);
        android.util.Log.d("FriendRequestAdapter", "updateRequests completed. Current size: " + this.requests.size());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView nameTextView;
        private TextView emailTextView;
        private Button acceptButton;
        private Button rejectButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            emailTextView = itemView.findViewById(R.id.emailTextView);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
        }

        public void bind(FriendRequest request, FriendRequestListener listener) {
            String name = request.getName();
            String email = request.getEmail();
            
            android.util.Log.d("FriendRequestAdapter", "Binding request - Name: " + name + ", Email: " + email + 
                              ", FromUserName: " + request.getFromUserName() + ", FromUserEmail: " + request.getFromUserEmail());
            
            nameTextView.setText(name != null ? name : "Unknown User");
            emailTextView.setText(email != null ? email : "No email");

            acceptButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAccept(request);
                }
            });

            rejectButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReject(request);
                }
            });
        }
    }
    
    // DiffUtil Callback for efficient list updates
    private static class FriendRequestDiffCallback extends DiffUtil.Callback {
        private final List<FriendRequest> oldList;
        private final List<FriendRequest> newList;
        
        public FriendRequestDiffCallback(List<FriendRequest> oldList, List<FriendRequest> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }
        
        @Override
        public int getOldListSize() {
            return oldList.size();
        }
        
        @Override
        public int getNewListSize() {
            return newList.size();
        }
        
        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            String oldId = oldList.get(oldItemPosition).getId();
            String newId = newList.get(newItemPosition).getId();
            
            if (oldId == null || newId == null) {
                android.util.Log.e("FriendRequestAdapter", "Null ID detected! oldId=" + oldId + ", newId=" + newId);
                return false;
            }
            
            return oldId.equals(newId);
        }
        
        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            FriendRequest oldReq = oldList.get(oldItemPosition);
            FriendRequest newReq = newList.get(newItemPosition);
            
            return oldReq.getName().equals(newReq.getName()) &&
                   oldReq.getEmail().equals(newReq.getEmail());
        }
    }
}
