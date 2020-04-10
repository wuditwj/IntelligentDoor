package com.njwyt.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.njwyt.db.ReservoirHelper;
import com.njwyt.entity.Theme;
import com.njwyt.intelligentdoor.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/10/9.
 */

public class ThemeRecyclerViewAdapter extends RecyclerView.Adapter<ThemeRecyclerViewAdapter.MyViewHold> {
    List<Theme> list;
    private LayoutInflater inflater;
    //声明一个接口
    private OnItemClickListener onItemClick;

    public ThemeRecyclerViewAdapter(Context context) {
        list = new ArrayList<>();
        inflater = LayoutInflater.from(context);
    }

    public void add(Theme theme) {
        list.add(theme);
    }

    public Theme getItem(int position) {
        return list.get(position);
    }

    @Override
    public MyViewHold onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_theme_recycler, null);
        MyViewHold viewHold = new MyViewHold(view);
        viewHold.image = view.findViewById(R.id.item_image);
        viewHold.text = view.findViewById(R.id.item_text);
        viewHold.layout = view.findViewById(R.id.item_layout);
        viewHold.frameLayout = view.findViewById(R.id.fl_selected);
        return viewHold;
    }

    @Override
    public void onBindViewHolder(MyViewHold holder, final int position) {
        final Theme theme = list.get(position);

        if (theme.getImage() == ReservoirHelper.getTheme()) {
            // 显示已被选择的勾
            holder.frameLayout.setVisibility(View.VISIBLE);
        } else {
            holder.frameLayout.setVisibility(View.GONE);
        }

        holder.image.setBackgroundResource(theme.getImage());
        holder.text.setText(theme.getText());
        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onItemClick != null) {
                    onItemClick.OnItemClickListeners(theme, position);
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class MyViewHold extends RecyclerView.ViewHolder {

        public MyViewHold(View itemView) {
            super(itemView);
        }

        FrameLayout frameLayout;
        ImageView image;
        TextView text;
        LinearLayout layout;
    }

    public interface OnItemClickListener {
        void OnItemClickListeners(Theme theme, int position);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        onItemClick = onItemClickListener;
    }
}
