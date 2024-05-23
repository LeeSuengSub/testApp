package com.example.testapp.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapp.R;
import com.example.testapp.databinding.ListviewCsvItemBinding;
import com.example.testapp.domain.CsvFile;

import java.util.List;

public class CsvFileAdapter extends RecyclerView.Adapter<CsvFileAdapter.ViewHolder>{

    private final List<CsvFile> csvFiles;
    private int selectedPosition = RecyclerView.NO_POSITION; // 선택된 아이템의 위치를 저장

    public CsvFileAdapter(List<CsvFile> csvFiles) {
        this.csvFiles = csvFiles;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ListviewCsvItemBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.getContext()),
                R.layout.listview_csv_item,
                parent,false
        );
        return new ViewHolder(binding);
    }

    @SuppressLint("RecyclerView")
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        CsvFile csvFile = csvFiles.get(position);
        holder.binding.setDomain(csvFile);

        // 선택 상태에 따라 배경 색상 변경
        if (position == selectedPosition) {
            // 선택된 아이템의 배경 색상 설정 (예: 파란색)
            holder.itemView.setBackgroundColor(holder.itemView.getContext().getColor(android.R.color.holo_blue_light));
        } else {
            // 선택되지 않은 아이템의 배경 색상 설정 (예: 기본 색상)
            holder.itemView.setBackgroundColor(holder.itemView.getContext().getColor(android.R.color.transparent));
        }

        // 아이템 클릭 이벤트 처리
        holder.itemView.setOnClickListener(v -> {
            int previousSelectedPosition = selectedPosition;
            selectedPosition = position;

            // 선택 상태가 변경되면 이전 선택 아이템과 새 선택 아이템을 다시 바인딩
            notifyItemChanged(previousSelectedPosition);
            notifyItemChanged(selectedPosition);
        });
    }

    @Override
    public int getItemCount() {
        return csvFiles.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ListviewCsvItemBinding binding;

        ViewHolder(ListviewCsvItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

}
