package com.cw.tv_yt.ui;


import com.cw.tv_yt.R;

import androidx.fragment.app.FragmentActivity;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.RowPresenter;

class CustomListRowPresenter extends ListRowPresenter {

   private int mInitialSelectedPosition;
   FragmentActivity act;

   public CustomListRowPresenter(FragmentActivity _act, int position) {
      this.mInitialSelectedPosition = position;
      act = _act;
   }

   @Override
   protected void onBindRowViewHolder(RowPresenter.ViewHolder holder, Object item) {
      super.onBindRowViewHolder(holder, item);

      ViewHolder vh = (ListRowPresenter.ViewHolder) holder;

      // set focus for selected category
      ListRow row = (ListRow)item;
      String categoryRowHeader = act.getString(R.string.current_category_title);
      if( row.getHeaderItem().getName().contains(categoryRowHeader))
         vh.getGridView().setSelectedPosition(mInitialSelectedPosition);
   }

}
