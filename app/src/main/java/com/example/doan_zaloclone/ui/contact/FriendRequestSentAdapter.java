package com.example.doan_zaloclone.ui.contact;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.FriendRequest;

import java.util.ArrayList;
import java.util.List;

public class FriendRequestSentAdapter extends RecyclerView.Adapter<FriendRequestSentAdapter.ViewHolder> {

    private List<FriendRequest> requests = new ArrayList<>();
    private final OnRequestActionListener listener;

    public interface OnRequestActionListener {
        void onRecall(FriendRequest request);
    }

    public FriendRequestSentAdapter(OnRequestActionListener listener) {
        this.listener = listener;
    }

    public void setRequests(List<FriendRequest> requests) {
        this.requests = requests != null ? requests : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_request_sent, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(requests.get(position));
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView txtName;
        TextView txtInfo;
        Button btnRecall;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            txtName = itemView.findViewById(R.id.txtName);
            txtInfo = itemView.findViewById(R.id.txtInfo);
            btnRecall = itemView.findViewById(R.id.btnRecall);
        }

        void bind(FriendRequest request) {
            // For sent requests, we show "To User" name
            // Assuming FriendRequest model stores it or we need to fetch it?
            // Current FriendRequest model has 'toUserId' but maybe not 'toUserName'.
            // Use 'name' field if it exists, or fallback.
            
            // Checking FriendRequest model from previous file viewing:
            // request.setFromUserName(...)
            // It parses from map. It doesn't seem to explicitly store 'toUserName'.
            
            String name = request.getName(); // Legacy logic often used 'name' for the counterparty
            if (name == null || name.isEmpty()) name = "Người dùng (" + request.getToUserId() + ")";
            
            txtName.setText(name);
            txtInfo.setText("Muốn kết bạn");

            btnRecall.setOnClickListener(v -> {
                if (listener != null) listener.onRecall(request);
            });
        }
    }
}
