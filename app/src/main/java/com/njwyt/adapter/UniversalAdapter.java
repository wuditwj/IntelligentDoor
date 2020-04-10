package com.njwyt.adapter;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class UniversalAdapter<T> extends RecyclerView.Adapter<UniversalAdapter.ViewHolder> {

    private ArrayList<T> mDataList = new ArrayList<>();
    private AdapterView adapterView;
    private int itemLayout;
    private int itemVariable;

    /**
     * @param data 数据
     * @param itemLayout item的样式
     * @param itemVariable item的variable中的对象写为：BR.XXXXX
     * @param adapterView item的View回调
     * */
    public UniversalAdapter(ArrayList<T> data, int itemLayout, int itemVariable, AdapterView adapterView) {
        mDataList.addAll(data);
        this.itemLayout = itemLayout;
        this.itemVariable = itemVariable;
        this.adapterView = adapterView;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        ViewDataBinding binding = DataBindingUtil.inflate(LayoutInflater
                .from(viewGroup.getContext()), itemLayout, viewGroup, false);
        ViewHolder holder = new ViewHolder(binding.getRoot());
        holder.setBinding(binding);
        return holder;
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    public void onBindViewHolder(UniversalAdapter.ViewHolder viewHolder, int i) {
        adapterView.getViewDataBinding(viewHolder, i);
        viewHolder.getBinding().setVariable(itemVariable, mDataList.get(i));
        viewHolder.getBinding().executePendingBindings();
    }

    @Override
    public int getItemCount() {
        if (mDataList == null) {
            return 0;
        }
        return mDataList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private ViewDataBinding binding;

        public ViewHolder(View itemView) {
            super(itemView);
        }

        public void setBinding(ViewDataBinding binding) {
            this.binding = binding;
        }

        public ViewDataBinding getBinding() {
            return this.binding;
        }
    }

    /**
     * 使用这个替代原来的notifyDataSetChanged
     * @param mDataList
     */
    public void refresh(ArrayList<T> mDataList) {
        this.mDataList = mDataList;
        notifyDataSetChanged();

    }

    /**
     * 单独刷新指定item
     * @param mDataList
     * @param position
     */
    public void refreshItem(ArrayList<T> mDataList, int position) {
        this.mDataList = mDataList;
        notifyItemChanged(position);
    }

    public interface AdapterView {
        /**
         * 获得每个item的view
         * viewDataBinding强制转换成自定义的binding即可使用
         * @param viewHolder
         * @param position
         * */
        void getViewDataBinding(UniversalAdapter.ViewHolder viewHolder, int position);
    }
}
