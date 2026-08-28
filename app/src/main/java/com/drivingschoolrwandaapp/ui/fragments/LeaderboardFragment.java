package com.drivingschoolrwandaapp.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.models.entities.LeaderboardEntry;
import com.drivingschoolrwandaapp.repository.Resource;
import com.drivingschoolrwandaapp.viewmodel.LeaderboardViewModel;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LeaderboardFragment extends Fragment {

    private LeaderboardViewModel viewModel;
    private LeaderboardAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private LinearLayout errorContainer;
    private LinearLayout emptyContainer;
    private RecyclerView recyclerView;
    private TextView errorMessage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_leaderboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(LeaderboardViewModel.class);

        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        progressBar = view.findViewById(R.id.progress_bar);
        errorContainer = view.findViewById(R.id.error_container);
        emptyContainer = view.findViewById(R.id.empty_container);
        recyclerView = view.findViewById(R.id.leaderboard_recycler);
        errorMessage = view.findViewById(R.id.error_message);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new LeaderboardAdapter();
        recyclerView.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());

        view.findViewById(R.id.retry_button).setOnClickListener(v -> viewModel.refresh());

        viewModel.getLeaderboard().observe(getViewLifecycleOwner(), this::handleLeaderboardState);
    }

    private void handleLeaderboardState(Resource<List<LeaderboardEntry>> resource) {
        if (resource == null) return;

        swipeRefresh.setRefreshing(false);

        switch (resource.status) {
            case LOADING:
                progressBar.setVisibility(View.VISIBLE);
                errorContainer.setVisibility(View.GONE);
                emptyContainer.setVisibility(View.GONE);
                recyclerView.setVisibility(View.GONE);
                break;

            case SUCCESS:
                progressBar.setVisibility(View.GONE);
                errorContainer.setVisibility(View.GONE);

                List<LeaderboardEntry> data = resource.data;
                if (data == null || data.isEmpty()) {
                    emptyContainer.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyContainer.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.setEntries(data);
                }
                break;

            case ERROR:
                progressBar.setVisibility(View.GONE);
                recyclerView.setVisibility(View.GONE);
                emptyContainer.setVisibility(View.GONE);
                errorContainer.setVisibility(View.VISIBLE);
                errorMessage.setText(resource.message);
                break;
        }
    }

    /**
     * RecyclerView adapter for leaderboard entries.
     */
    private class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

        private List<LeaderboardEntry> entries = new ArrayList<>();

        void setEntries(List<LeaderboardEntry> entries) {
            this.entries = entries;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_leaderboard, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LeaderboardEntry entry = entries.get(position);
            holder.bind(entry);
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView rankText;
            private final TextView nameText;
            private final TextView examsTakenText;
            private final TextView scoreText;
            private final TextView passRateText;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                rankText = itemView.findViewById(R.id.rank_text);
                nameText = itemView.findViewById(R.id.name_text);
                examsTakenText = itemView.findViewById(R.id.exams_taken_text);
                scoreText = itemView.findViewById(R.id.score_text);
                passRateText = itemView.findViewById(R.id.pass_rate_text);
            }

            void bind(LeaderboardEntry entry) {
                rankText.setText(String.valueOf(entry.getRank()));
                nameText.setText(entry.getName());
                examsTakenText.setText(getString(R.string.leaderboard_exams_taken, entry.getExamsTaken()));
                scoreText.setText(getString(R.string.percentage_score, entry.getAverageScore()));
                passRateText.setText(getString(R.string.leaderboard_pass_rate, entry.getPassRate()));
            }
        }
    }
}
