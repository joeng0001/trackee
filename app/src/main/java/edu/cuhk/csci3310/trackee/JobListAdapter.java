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

import java.util.LinkedList;

public class JobListAdapter extends RecyclerView.Adapter<JobListAdapter.JobViewHolder> {
    private Context context;
    private LayoutInflater mInflater;
    private LinkedList<JSONObject> jobList;

    class JobViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout jobLayout;
        final JobListAdapter mAdapter;

        public JobViewHolder(View itemView, JobListAdapter adapter) {
            super(itemView);
            this.jobLayout = (LinearLayout)itemView.findViewById(R.id.jobLayout);
            this.mAdapter = adapter;

            // page navigation while click event
            this.jobLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Get the position of the item that was clicked.
                    int position = getLayoutPosition();
                    try {
                        //pass needed data and directg to the detailActivity page
                        Intent intent=new Intent(context, DetailActivity.class);
                        intent.putExtra("title",jobList.get(position).getString("title"));
                        intent.putExtra("remarks",jobList.get(position).getString("remarks"));
                        intent.putExtra("start_time",jobList.get(position).getString("start_time"));
                        intent.putExtra("end_time",jobList.get(position).getString("end_time"));
                        intent.putExtra("creation_flag",false);
                        intent.putExtra("job_index",position);
                        intent.putExtra("location",jobList.get(position).getString("location"));
                        context.startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            });
        };
    }

    public JobListAdapter(Context context, LinkedList<JSONObject> jobList) {
        mInflater = LayoutInflater.from(context);
        this.context=context;
        this.jobList=jobList;
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View mItemView = mInflater.inflate(R.layout.joblist_item, parent, false);
        return new JobViewHolder(mItemView, this);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        try{
            //init place holder
            JSONObject obj=jobList.get(position);

            TextView title=holder.jobLayout.findViewById(R.id.jobTitle);
            TextView remarks=holder.jobLayout.findViewById(R.id.jobRemarks);
            TextView time=holder.jobLayout.findViewById(R.id.jobEndTime);
            TextView location=holder.jobLayout.findViewById(R.id.jobLocation);

            title.setText(obj.getString("title"));
            location.setText(obj.getString("location"));
            remarks.setText(obj.getString("remarks"));
            time.setText("Start At :"+ obj.getString("start_time") +"  End At: "+obj.getString("end_time"));

        }
        catch(Exception e){
            e.printStackTrace();
        }
    }


    @Override
    public int getItemCount() {
        return this.jobList.size();
    }
}
