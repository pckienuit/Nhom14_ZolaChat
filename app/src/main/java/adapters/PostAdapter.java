package adapters;

import android.content.Context;
import android.view.LayoutInflater;import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doan_zaloclone.R; // Đã sửa package name theo tên project thực tế
import com.example.doan_zaloclone.models.FireBasePost; // Đã sửa đường dẫn import model

import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private Context context;
    private List<FireBasePost> postList;
    private PrettyTime prettyTime;

    // Constructor
    public PostAdapter(Context context, List<FireBasePost> postList) {
        this.context = context;
        this.postList = postList;
        // Khởi tạo PrettyTime với Locale tiếng Việt
        this.prettyTime = new PrettyTime(new Locale("vi"));
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
        // Kiểm tra timestamp để tránh lỗi NullPointerException
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
                    .placeholder(R.drawable.ic_launcher_background) // Dùng ảnh mặc định có sẵn
                    .error(R.drawable.ic_launcher_background)
                    .into(holder.imgAvatar);
        } else {
            // Set ảnh mặc định nếu không có avatar
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
            // Ẩn ImageView nếu bài viết không có ảnh
            holder.imgContent.setVisibility(View.GONE);
        }

        // 6. Tạm thời ẩn các số liệu chưa có trong model
        // Nếu sau này model FireBasePost có likeCount, bạn có thể bỏ comment
        // holder.tvLikeCount.setText(String.valueOf(post.getLikeCount()));
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

            // Ánh xạ ID tương ứng trong file item_post.xml
            // Đảm bảo các ID này trùng khớp với file XML item_post.xml bạn đã tạo
            // Ánh xạ ID tương ứng trong file item_post.xml
            // Đảm bảo các ID này trùng khớp với file XML item_post.xml bạn đã tạo
            imgAvatar = itemView.findViewById(R.id.imgAvatarPost);
            tvUserName = itemView.findViewById(R.id.tvUserNamePost);
            tvTime = itemView.findViewById(R.id.tvTimePost);
            tvContent = itemView.findViewById(R.id.tvContentPost);
            imgContent = itemView.findViewById(R.id.imgContentPost);

            // Các nút tương tác
            btnLike = itemView.findViewById(R.id.btnLike);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            btnComment = itemView.findViewById(R.id.btnComment);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);

        }
    }
}

