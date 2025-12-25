package com.example.doan_zaloclone.ui.file;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.doan_zaloclone.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.Calendar;

/**
 * Dialog for filtering files by time with preset and custom options
 */
public class TimeFilterDialog extends DialogFragment {

    private TimeFilterListener listener;
    private TimeFilterPreset currentPreset = TimeFilterPreset.NONE;
    // UI Components
    private ChipGroup presetChipGroup;
    private Chip chipYesterday;
    private Chip chipLastWeek;
    private Chip chipLastMonth;
    private Chip chipCustom;
    private LinearLayout customDateLayout;
    // Date pickers
    private NumberPicker startDayPicker;
    private NumberPicker startMonthPicker;
    private NumberPicker startYearPicker;
    private NumberPicker endDayPicker;
    private NumberPicker endMonthPicker;
    private NumberPicker endYearPicker;

    public static TimeFilterDialog newInstance() {
        return new TimeFilterDialog();
    }

    public void setTimeFilterListener(TimeFilterListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_time_filter, null);

        initViews(view);
        setupPresetChips();
        setupDatePickers();
        setupButtons(view);

        builder.setView(view);
        return builder.create();
    }

    private void initViews(View view) {
        presetChipGroup = view.findViewById(R.id.presetChipGroup);
        chipYesterday = view.findViewById(R.id.chipYesterday);
        chipLastWeek = view.findViewById(R.id.chipLastWeek);
        chipLastMonth = view.findViewById(R.id.chipLastMonth);
        chipCustom = view.findViewById(R.id.chipCustom);
        customDateLayout = view.findViewById(R.id.customDateLayout);

        startDayPicker = view.findViewById(R.id.startDayPicker);
        startMonthPicker = view.findViewById(R.id.startMonthPicker);
        startYearPicker = view.findViewById(R.id.startYearPicker);
        endDayPicker = view.findViewById(R.id.endDayPicker);
        endMonthPicker = view.findViewById(R.id.endMonthPicker);
        endYearPicker = view.findViewById(R.id.endYearPicker);
    }

    private void setupPresetChips() {
        presetChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipCustom) {
                customDateLayout.setVisibility(View.VISIBLE);
                currentPreset = TimeFilterPreset.CUSTOM;
            } else {
                customDateLayout.setVisibility(View.GONE);

                if (checkedId == R.id.chipYesterday) {
                    currentPreset = TimeFilterPreset.YESTERDAY;
                } else if (checkedId == R.id.chipLastWeek) {
                    currentPreset = TimeFilterPreset.LAST_WEEK;
                } else if (checkedId == R.id.chipLastMonth) {
                    currentPreset = TimeFilterPreset.LAST_MONTH;
                } else {
                    currentPreset = TimeFilterPreset.NONE;
                }
            }
        });
    }

    private void setupDatePickers() {
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH) + 1; // 1-based
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

        // Setup Day pickers (1-31)
        startDayPicker.setMinValue(1);
        startDayPicker.setMaxValue(31);
        startDayPicker.setValue(currentDay);
        endDayPicker.setMinValue(1);
        endDayPicker.setMaxValue(31);
        endDayPicker.setValue(currentDay);

        // Setup Month pickers (1-12)
        startMonthPicker.setMinValue(1);
        startMonthPicker.setMaxValue(12);
        startMonthPicker.setValue(currentMonth);
        endMonthPicker.setMinValue(1);
        endMonthPicker.setMaxValue(12);
        endMonthPicker.setValue(currentMonth);

        // Setup Year pickers (2020 to current year + 1)
        startYearPicker.setMinValue(2020);
        startYearPicker.setMaxValue(currentYear + 1);
        startYearPicker.setValue(currentYear);
        endYearPicker.setMinValue(2020);
        endYearPicker.setMaxValue(currentYear + 1);
        endYearPicker.setValue(currentYear);

        // Set formatter to show 2 digits for day and month
        NumberPicker.Formatter twoDigitFormatter = value -> String.format("%02d", value);
        startDayPicker.setFormatter(twoDigitFormatter);
        startMonthPicker.setFormatter(twoDigitFormatter);
        endDayPicker.setFormatter(twoDigitFormatter);
        endMonthPicker.setFormatter(twoDigitFormatter);
    }

    private void setupButtons(View view) {
        Button cancelButton = view.findViewById(R.id.cancelButton);
        Button applyButton = view.findViewById(R.id.applyButton);

        cancelButton.setOnClickListener(v -> dismiss());

        applyButton.setOnClickListener(v -> {
            if (listener != null) {
                if (currentPreset == TimeFilterPreset.CUSTOM) {
                    Long startDate = getDateFromPickers(startDayPicker, startMonthPicker, startYearPicker, true);
                    Long endDate = getDateFromPickers(endDayPicker, endMonthPicker, endYearPicker, false);
                    listener.onTimeFilterApplied(TimeFilterPreset.CUSTOM, startDate, endDate);
                } else if (currentPreset != TimeFilterPreset.NONE) {
                    DateRange range = calculatePresetRange(currentPreset);
                    listener.onTimeFilterApplied(currentPreset, range.startDate, range.endDate);
                } else {
                    // No filter selected, clear filter
                    listener.onTimeFilterApplied(TimeFilterPreset.NONE, null, null);
                }
            }
            dismiss();
        });
    }

    private Long getDateFromPickers(NumberPicker dayPicker, NumberPicker monthPicker,
                                    NumberPicker yearPicker, boolean isStartOfDay) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, yearPicker.getValue());
        calendar.set(Calendar.MONTH, monthPicker.getValue() - 1); // 0-based
        calendar.set(Calendar.DAY_OF_MONTH, dayPicker.getValue());

        if (isStartOfDay) {
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
        }
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTimeInMillis();
    }

    private DateRange calculatePresetRange(TimeFilterPreset preset) {
        Calendar endCal = Calendar.getInstance();
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 0);

        Calendar startCal = (Calendar) endCal.clone();

        switch (preset) {
            case YESTERDAY:
                // Yesterday: from 00:00:00 yesterday to 23:59:59 yesterday
                startCal.add(Calendar.DAY_OF_MONTH, -1);
                startCal.set(Calendar.HOUR_OF_DAY, 0);
                startCal.set(Calendar.MINUTE, 0);
                startCal.set(Calendar.SECOND, 0);
                endCal.add(Calendar.DAY_OF_MONTH, -1);
                break;

            case LAST_WEEK:
                // Last 7 days: from 00:00:00 7 days ago to now
                startCal.add(Calendar.DAY_OF_MONTH, -7);
                startCal.set(Calendar.HOUR_OF_DAY, 0);
                startCal.set(Calendar.MINUTE, 0);
                startCal.set(Calendar.SECOND, 0);
                endCal = Calendar.getInstance(); // Current time
                break;

            case LAST_MONTH:
                // Last 30 days: from 00:00:00 30 days ago to now
                startCal.add(Calendar.DAY_OF_MONTH, -30);
                startCal.set(Calendar.HOUR_OF_DAY, 0);
                startCal.set(Calendar.MINUTE, 0);
                startCal.set(Calendar.SECOND, 0);
                endCal = Calendar.getInstance(); // Current time
                break;
        }

        return new DateRange(startCal.getTimeInMillis(), endCal.getTimeInMillis());
    }

    public enum TimeFilterPreset {
        NONE,
        YESTERDAY,
        LAST_WEEK,
        LAST_MONTH,
        CUSTOM
    }

    public interface TimeFilterListener {
        void onTimeFilterApplied(TimeFilterPreset preset, Long startDate, Long endDate);
    }

    private static class DateRange {
        final Long startDate;
        final Long endDate;

        DateRange(Long startDate, Long endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }
}
