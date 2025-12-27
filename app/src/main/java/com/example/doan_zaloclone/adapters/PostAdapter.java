package com.example.doan_zaloclone.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.FireBasePost;
import com.google.firebase.auth.FirebaseAuth;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private Context context;
    private List<FireBasePost> postList;
    private PrettyTime prettyTime;
    private String currentUserId;
    private OnPostInteractionListener listener;

    public interface OnPostInteractionListener {
        void onLikeClick(String postId);
        void onCommentClick(String postId);
    }

    // Constructor
    public PostAdapter(Context context, List<FireBasePost> postList, OnPostInteractionListener listener) {
        this.context = context;
        this.postList = postList;
        this.listener = listener;
        this.prettyTime = new PrettyTime(new Locale("vi"));
        
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate layout item_post.xml
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        FireBasePost post = postList.get(position);

        // 1. Gán Tên User
        if (post.getUserName() != null) {
            holder.tvUserName.setText(post.getUserName());
        } else {
            holder.tvUserName.setText("Người dùng ẩn danh");
        }

        // 2. Gán Nội dung bài viết
        holder.tvContent.setText(post.getContent());

        // 3. Xử lý thời gian (ví dụ: "10 phút trước")
        if (post.getTimestamp() > 0) {
            String timeAgo = prettyTime.format(new Date(post.getTimestamp()));
            holder.tvTime.setText(timeAgo);
        } else {
            holder.tvTime.setText("Vừa xong");
        }

        // 4. Load Avatar User (nếu có URL)
        if (post.getUserAvatar() != null && !post.getUserAvatar().isEmpty()) {
            Glide.with(context)
                    .load(post.getUserAvatar())
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.ic_launcher_background);
        }

        // 5. Load Ảnh bài đăng (nếu có)
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            holder.imgContent.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(post.getImageUrl())
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(holder.imgContent);
        } else {
            holder.imgContent.setVisibility(View.GONE);
        }

        // 6. Hiển thị số lượt like
        holder.tvLikeCount.setText(String.valueOf(post.getLikeCount()));
        
        // 7. Xử lý trạng thái Like (đổi màu nút)
        if (post.isLikedBy(currentUserId)) {
            holder.btnLike.setColorFilter(Color.parseColor("#1E88E5")); // Màu xanh Zalo
            holder.btnLike.setImageResource(android.R.drawable.btn_star_big_on);
        } else {
            holder.btnLike.setColorFilter(Color.parseColor("#757575")); // Màu xám
            holder.btnLike.setImageResource(android.R.drawable.btn_star_big_off);
        }
        
        // 8. Sự kiện Click
        holder.btnLike.setOnClickListener(v -> {
            if (listener != null) listener.onLikeClick(post.getPostId());
        });
        
        holder.btnComment.setOnClickListener(v -> {
            if (listener != null) listener.onCommentClick(post.getPostId());
        });
        
        holder.tvCommentCount.setOnClickListener(v -> {
            if (listener != null) listener.onCommentClick(post.getPostId());
        });
    }

    @Override
    public int getItemCount() {
        if (postList != null) {
            return postList.size();
        }
        return 0;
    }

    /**
     * ViewHolder class để ánh xạ các view từ item_post.xml
     */
    public static class PostViewHolder extends RecyclerView.ViewHolder {

        CircleImageView imgAvatar;
        TextView tvUserName, tvTime, tvContent, tvLikeCount, tvCommentCount;
        ImageView imgContent, btnLike, btnComment;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);

            // 1. Thông tin người dùng
            imgAvatar = itemView.findViewById(R.id.imgAvatarPost);
            tvUserName = itemView.findViewById(R.id.tvUserNamePost);
            tvTime = itemView.findViewById(R.id.tvTimePost);

            // 2. Nội dung bài viết
            tvContent = itemView.findViewById(R.id.tvContentPost);
            imgContent = itemView.findViewById(R.id.imgContentPost);

            // 3. Tương tác (Like/Comment)
            btnLike = itemView.findViewById(R.id.btnLike);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            btnComment = itemView.findViewById(R.id.btnComment);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
        }
    }
}
