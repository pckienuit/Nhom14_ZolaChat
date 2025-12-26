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

public class FriendRequestReceivedAdapter extends RecyclerView.Adapter<FriendRequestReceivedAdapter.ViewHolder> {

    private List<FriendRequest> requests = new ArrayList<>();
    private final OnRequestActionListener listener;

    public interface OnRequestActionListener {
        void onAccept(FriendRequest request);
        void onDecline(FriendRequest request);
    }

    public FriendRequestReceivedAdapter(OnRequestActionListener listener) {
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
                .inflate(R.layout.item_friend_request_received, parent, false);
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
        TextView txtMessage;
        View layoutMessage;
        Button btnAccept, btnReject;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            txtName = itemView.findViewById(R.id.txtName);
            txtInfo = itemView.findViewById(R.id.txtInfo);
            txtMessage = itemView.findViewById(R.id.txtMessage);
            layoutMessage = itemView.findViewById(R.id.layoutMessage);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }

        void bind(FriendRequest request) {
            String name = request.getFromUserName();
            if (name == null || name.isEmpty()) name = "Người dùng Zalo";
            txtName.setText(name);

            // Avatar placeholder (first letter)
            // Ideally load image
            // Glide.with(itemView)...

            // Info
            // Format timestamp to date
            // SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
            // String date = sdf.format(new Date(request.getTimestamp()));
            txtInfo.setText("Tìm kiếm qua số điện thoại"); // Placeholder or use real data

            // Message
            layoutMessage.setVisibility(View.GONE); // Hide unless message content exists

            btnAccept.setOnClickListener(v -> {
                if (listener != null) listener.onAccept(request);
            });

            btnReject.setOnClickListener(v -> {
                if (listener != null) listener.onDecline(request);
            });
        }
    }
}
