package com.example.doan_zaloclone.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.activities.CreatePostActivity; // Đảm bảo bạn đã có Activity này
import com.example.doan_zaloclone.models.FireBasePost;
import com.example.doan_zaloclone.adapters.PostAdapter; // Import đúng Adapter bạn vừa sửa

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NewsFeedFragment extends Fragment {

    private RecyclerView rvPosts;
    private PostAdapter postAdapter;
    private List<FireBasePost> postList;
    private FirebaseFirestore db;
    private LinearLayout layoutPostInput;

    // Constructor rỗng bắt buộc
    public NewsFeedFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Lưu ý: R.layout.recycler_view là file XML chứa RecyclerView mà bạn đã tạo
        // Nếu bạn đặt tên file XML khác (ví dụ: fragment_news_feed.xml), hãy sửa lại ở đây
        return inflater.inflate(R.layout.recycler_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Khởi tạo Firestore
        db = FirebaseFirestore.getInstance();

        // 2. Ánh xạ View (Khớp ID trong file XML layout của bạn)
        rvPosts = view.findViewById(R.id.rvPosts);
        layoutPostInput = view.findViewById(R.id.layoutPostInput);

        // 3. Setup RecyclerView
        rvPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        postList = new ArrayList<>();

        // Khởi tạo Adapter
        postAdapter = new PostAdapter(getContext(), postList);
        rvPosts.setAdapter(postAdapter);

        // 4. Sự kiện click vào thanh đăng bài -> Mở màn hình CreatePostActivity
        if (layoutPostInput != null) {
            layoutPostInput.setOnClickListener(v -> {
                // Kiểm tra xem CreatePostActivity đã tồn tại chưa, nếu chưa hãy tạo nó
                Intent intent = new Intent(getContext(), CreatePostActivity.class);
                startActivity(intent);
            });
        }

        // 5. Gọi hàm tải dữ liệu
        loadPostsFromFirebase();
    }

    private void loadPostsFromFirebase() {
        // Lấy dữ liệu từ collection "posts", sắp xếp mới nhất lên đầu
        db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Firestore", "Listen failed.", error);
                        return;
                    }

                    if (value != null) {
                        postList.clear(); // Xóa list cũ
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            // Chuyển đổi Document thành Object FireBasePost
                            FireBasePost post = doc.toObject(FireBasePost.class);
                            postList.add(post);
                        }
                        // Cập nhật giao diện
                        postAdapter.notifyDataSetChanged();
                    }
                });
    }
}
