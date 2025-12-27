package com.example.doan_zaloclone.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Comment;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private Context context;
    private List<Comment> commentList;
    private PrettyTime prettyTime;

    public CommentAdapter(Context context, List<Comment> commentList) {
        this.context = context;
        this.commentList = commentList;
        this.prettyTime = new PrettyTime(new Locale("vi"));
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);

        holder.tvUserName.setText(comment.getUserName());
        holder.tvContent.setText(comment.getContent());

        if (comment.getTimestamp() > 0) {
            String timeAgo = prettyTime.format(new Date(comment.getTimestamp()));
            holder.tvTime.setText(timeAgo);
        }

        if (comment.getUserAvatar() != null && !comment.getUserAvatar().isEmpty()) {
            Glide.with(context)
                    .load(comment.getUserAvatar())
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.ic_launcher_background);
        }
    }

    @Override
    public int getItemCount() {
        return commentList != null ? commentList.size() : 0;
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {

        CircleImageView imgAvatar;
        TextView tvUserName, tvContent, tvTime;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatarComment);
            tvUserName = itemView.findViewById(R.id.tvUserNameComment);
            tvContent = itemView.findViewById(R.id.tvContentComment);
            tvTime = itemView.findViewById(R.id.tvTimeComment);
        }
    }
}
