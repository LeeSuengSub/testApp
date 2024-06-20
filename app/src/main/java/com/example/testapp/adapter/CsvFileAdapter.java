package com.example.testapp.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
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
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public CsvFileAdapter(List<CsvFile> csvFiles) {
        this.csvFiles = csvFiles;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
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

        // 아이템 클릭 이벤트 처리는 ViewHolder에서 수행
    }

    @Override
    public int getItemCount() {
        return csvFiles.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final ListviewCsvItemBinding binding;

        ViewHolder(ListviewCsvItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // itemView에 클릭 리스너 설정
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (listener != null && position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(v, position);
                    }
                }
            });
        }
    }

}
