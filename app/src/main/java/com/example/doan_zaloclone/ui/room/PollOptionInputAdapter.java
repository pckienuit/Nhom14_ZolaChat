package com.example.doan_zaloclone.ui.room;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for poll option inputs in the create poll screen
 */
public class PollOptionInputAdapter extends RecyclerView.Adapter<PollOptionInputAdapter.OptionViewHolder> {

    private final List<String> options;
    private final OnOptionChangeListener listener;

    public PollOptionInputAdapter(List<String> options, OnOptionChangeListener listener) {
        this.options = options != null ? options : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public OptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_poll_option_input, parent, false);
        return new OptionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OptionViewHolder holder, int position) {
        holder.bind(options.get(position), position);
    }

    @Override
    public int getItemCount() {
        return options.size();
    }

    /**
     * Add a new empty option
     */
    public void addOption() {
        options.add("");
        notifyItemInserted(options.size() - 1);
        if (listener != null) {
            listener.onOptionChanged();
        }
    }

    /**
     * Remove option at position
     */
    public void removeOption(int position) {
        if (position >= 0 && position < options.size()) {
            options.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, options.size());
            if (listener != null) {
                listener.onOptionChanged();
            }
        }
    }

    /**
     * Get all non-empty options
     */
    public List<String> getValidOptions() {
        List<String> validOptions = new ArrayList<>();
        for (String option : options) {
            if (option != null && !option.trim().isEmpty()) {
                validOptions.add(option.trim());
            }
        }
        return validOptions;
    }

    /**
     * Get total count including empty options
     */
    public int getTotalCount() {
        return options.size();
    }

    public interface OnOptionChangeListener {
        void onOptionChanged();
    }

    class OptionViewHolder extends RecyclerView.ViewHolder {
        private final EditText optionEditText;
        private final ImageButton removeButton;
        private TextWatcher textWatcher;

        OptionViewHolder(@NonNull View itemView) {
            super(itemView);
            optionEditText = itemView.findViewById(R.id.optionEditText);
            removeButton = itemView.findViewById(R.id.removeButton);
        }

        void bind(String option, int position) {
            // Remove previous text watcher to avoid triggering during setup
            if (textWatcher != null) {
                optionEditText.removeTextChangedListener(textWatcher);
            }

            // Set hint based on position
            optionEditText.setHint("Phương án " + (position + 1));

            // Set current text
            optionEditText.setText(option);

            // Move cursor to end
            optionEditText.setSelection(option.length());

            // Create new text watcher for this position
            textWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    int adapterPosition = getAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION && adapterPosition < options.size()) {
                        options.set(adapterPosition, s.toString());
                        if (listener != null) {
                            listener.onOptionChanged();
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            };

            optionEditText.addTextChangedListener(textWatcher);

            // Handle remove button click
            removeButton.setOnClickListener(v -> {
                int adapterPosition = getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    removeOption(adapterPosition);
                }
            });

            // Hide remove button if only 2 options left (minimum required)
            removeButton.setVisibility(options.size() <= 2 ? View.INVISIBLE : View.VISIBLE);
        }
    }
}
