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
            // For sent requests, show recipient (toUser) info
            String name = request.getToUserName();
            if (name == null || name.isEmpty()) {
                name = "Người dùng";
            }
            
            String email = request.getToUserEmail();
            
            txtName.setText(name);
            if (email != null && !email.isEmpty()) {
                txtInfo.setText(email);
            } else {
                txtInfo.setText("Đang chờ phản hồi");
            }

            btnRecall.setOnClickListener(v -> {
                if (listener != null) listener.onRecall(request);
            });
        }
    }
}
