package edu.cuhk.csci3310.trackee;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.LinkedList;

public class HistoryJobListAdapter extends RecyclerView.Adapter<HistoryJobListAdapter.HistoryJobViewHolder> {
    private Context context;
    private LayoutInflater mInflater;

    private LinkedList<JSONObject> jobHistoryList;


    //private final Intent intent ;
    class HistoryJobViewHolder extends RecyclerView.ViewHolder {

        private LinearLayout jobHistoryLayout;

        final HistoryJobListAdapter mAdapter;

        public HistoryJobViewHolder(View itemView, HistoryJobListAdapter adapter) {
            super(itemView);
            this.jobHistoryLayout = (LinearLayout)itemView.findViewById(R.id.jobHistoryLayout);
            this.jobHistoryLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getLayoutPosition();
                    try {
                        //pass needed data and start the MapsActivity
                        Intent intent=new Intent(context, MapsActivity.class);
                        intent.putExtra("start_time",jobHistoryList.get(position).getString("start_time"));
                        intent.putExtra("end_time",jobHistoryList.get(position).getString("end_time"));
                        intent.putExtra("target_location",jobHistoryList.get(position).getString("location"));
                        context.startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            });
            this.mAdapter = adapter;
        };
    }

    public HistoryJobListAdapter(Context context, LinkedList<JSONObject> jobList) {
        mInflater = LayoutInflater.from(context);
        this.context=context;
        this.jobHistoryList=jobList;

    }

    @NonNull
    @Override
    public HistoryJobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View mItemView = mInflater.inflate(R.layout.joblist_history_item, parent, false);
        return new HistoryJobViewHolder(mItemView, this);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryJobViewHolder holder, int position) {
        try{
            //init place holder
            JSONObject obj=jobHistoryList.get(position);

            TextView title=holder.jobHistoryLayout.findViewById(R.id.jobHistoryTitle);
            TextView remarks=holder.jobHistoryLayout.findViewById(R.id.jobHistoryRemarks);
            TextView time=holder.jobHistoryLayout.findViewById(R.id.jobHistoryTime);

            title.setText(obj.getString("title"));
            remarks.setText(obj.getString("remarks"));
            time.setText("start_time : "+obj.getString("start_time")+"  "+ "end_time : " +obj.getString("end_time"));

        }
        catch(Exception e){
            e.printStackTrace();
        }
    }


    @Override
    public int getItemCount() {
        return this.jobHistoryList.size();
    }
}
