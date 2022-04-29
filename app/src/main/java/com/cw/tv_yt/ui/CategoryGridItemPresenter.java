package com.cw.tv_yt.ui;

import android.view.View;

import com.cw.tv_yt.Utils;
import com.cw.tv_yt.presenter.GridItemPresenter;

import java.util.List;

import androidx.fragment.app.FragmentActivity;

class CategoryGridItemPresenter extends GridItemPresenter {

   FragmentActivity act;
   List<String> category_names;

   public CategoryGridItemPresenter(MainFragment mainFragment, List<String> _category_names) {
      super(mainFragment, _category_names);
      act = mainFragment.getActivity();
      category_names = _category_names;
   }

   @Override
   public void onBindViewHolder(ViewHolder viewHolder, Object item) {
      viewHolder.view.setOnLongClickListener(new View.OnLongClickListener() {
         @Override
         public boolean onLongClick(View v) {
            System.out.println("CategoryGridItemPresenter / onLongClick / category item = " + item);
            Utils.confirmDeleteCategory(act,category_names,(String)item);
            return false;
         }
      });
      super.onBindViewHolder(viewHolder, item);
   }
}
