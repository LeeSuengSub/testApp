package com.example.testapp.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testapp.MainActivity;
import com.example.testapp.R;
import com.example.testapp.singleton.Singleton;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private List<String> imagePaths;
    private Context context;

    public ImageAdapter(Context context, List<String> imagePaths) {
        this.context = context;
        this.imagePaths = imagePaths;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        final String imagePath = imagePaths.get(position);
        String imageName = null;
        Glide.with(context).load(imagePath).into(holder.imageView);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 이미지 경로를 MainActivity로 전달
                Intent intent = new Intent(context, MainActivity.class);
                intent.putExtra("selected_image_path", imagePath);

                String[] str = imagePath.split("/");
                Singleton singleton = Singleton.getInstance();
                singleton.setSelectedMapFile(str[str.length -1]);
                context.startActivity(intent);

            }
        });
    }

    @Override
    public int getItemCount() {
        return imagePaths.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {

        ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
        }
    }
}
