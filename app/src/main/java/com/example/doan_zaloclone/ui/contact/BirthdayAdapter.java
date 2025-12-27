package com.example.doan_zaloclone.ui.contact;

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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * BirthdayAdapter - Adapter for displaying friends' birthdays
 */
public class BirthdayAdapter extends RecyclerView.Adapter<BirthdayAdapter.BirthdayViewHolder> {

    private List<User> birthdays;

    public BirthdayAdapter(List<User> birthdays) {
        this.birthdays = birthdays;
    }

    public void updateBirthdays(List<User> newBirthdays) {
        this.birthdays = newBirthdays;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BirthdayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_birthday, parent, false);
        return new BirthdayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BirthdayViewHolder holder, int position) {
        User user = birthdays.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return birthdays.size();
    }

    static class BirthdayViewHolder extends RecyclerView.ViewHolder {
        private ImageView avatarImage;
        private TextView nameText;
        private TextView birthdayText;
        private TextView daysUntilText;

        public BirthdayViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImage = itemView.findViewById(R.id.avatarImage);
            nameText = itemView.findViewById(R.id.nameText);
            birthdayText = itemView.findViewById(R.id.birthdayText);
            daysUntilText = itemView.findViewById(R.id.daysUntilText);
        }

        public void bind(User user) {
            // Set name
            nameText.setText(user.getName());

            // Set birthday
            birthdayText.setText(user.getBirthday());

            // Set avatar
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(user.getAvatarUrl())
                        .placeholder(R.drawable.ic_avatar)
                        .circleCrop()
                        .into(avatarImage);
            } else {
                avatarImage.setImageResource(R.drawable.ic_avatar);
            }

            // Calculate and display days until birthday
            int daysUntil = getDaysUntilBirthday(user.getBirthday());
            if (daysUntil == 0) {
                daysUntilText.setText("🎉 Hôm nay!");
                daysUntilText.setTextColor(itemView.getContext().getResources().getColor(R.color.colorPrimary, null));
            } else if (daysUntil == 1) {
                daysUntilText.setText("Ngày mai");
            } else if (daysUntil < 7) {
                daysUntilText.setText("Còn " + daysUntil + " ngày");
            } else if (daysUntil < 30) {
                daysUntilText.setText("Còn " + (daysUntil / 7) + " tuần");
            } else {
                daysUntilText.setText("Còn " + (daysUntil / 30) + " tháng");
            }
        }

        private int getDaysUntilBirthday(String birthdayStr) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date birthday = sdf.parse(birthdayStr);
                if (birthday == null) return 0;

                Calendar today = Calendar.getInstance();
                Calendar birthdayCal = Calendar.getInstance();
                birthdayCal.setTime(birthday);

                // Set birthday to this year
                birthdayCal.set(Calendar.YEAR, today.get(Calendar.YEAR));

                // If birthday already passed, use next year
                if (birthdayCal.before(today)) {
                    birthdayCal.add(Calendar.YEAR, 1);
                }

                long diff = birthdayCal.getTimeInMillis() - today.getTimeInMillis();
                return (int) (diff / (1000 * 60 * 60 * 24));
            } catch (Exception e) {
                return 0;
            }
        }
    }
}
