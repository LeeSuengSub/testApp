package com.example.testapp.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapp.R;
import com.example.testapp.databinding.ListviewMapItemBinding;
import com.example.testapp.domain.MapFile;

import java.util.List;

public class MapFileAdapter extends RecyclerView.Adapter<MapFileAdapter.ViewHolder>{

    private final List<MapFile> mapFiles;
    private int selectedPosition = RecyclerView.NO_POSITION; // 선택된 아이템의 위치를 저장

    public MapFileAdapter(List<MapFile> mapFiles) {
        this.mapFiles = mapFiles;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ListviewMapItemBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.getContext()),
                R.layout.listview_map_item,
                parent,false
        );
        return new ViewHolder(binding);
    }

    @SuppressLint("RecyclerView")
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        MapFile mapFile = mapFiles.get(position);
        holder.binding.setDomain(mapFile);

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
        return mapFiles.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ListviewMapItemBinding binding;

        ViewHolder(ListviewMapItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

}
