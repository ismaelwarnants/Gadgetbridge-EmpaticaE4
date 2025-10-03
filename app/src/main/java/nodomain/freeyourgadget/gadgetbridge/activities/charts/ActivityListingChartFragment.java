/*  Copyright (C) 2020-2024 José Rebelo, Petr Vaněk

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.Chart;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySession;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FormatUtils;

public class ActivityListingChartFragment extends AbstractActivityChartFragment<ActivityListingChartFragment.MyChartsData> {
    protected static final Logger LOG = LoggerFactory.getLogger(ActivityListingChartFragment.class);
    int tsDateTo;

    private View rootView;
    private ActivityListingAdapter stepListAdapter;
    private TextView stepsDateView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_steps_list, container, false);
        getChartsHost().enableSwipeRefresh(false);
        RecyclerView stepsList = rootView.findViewById(R.id.itemListView);
        stepListAdapter = new ActivityListingAdapter(getContext());
        stepsList.setAdapter(stepListAdapter);
        stepsList.setLayoutManager(new LinearLayoutManager(requireContext()));

        stepListAdapter.setOnItemClickListener(position -> {
            ActivitySession item = stepListAdapter.getItem(position);
            if (item.getSessionType() != ActivitySession.SESSION_SUMMARY) {
                int tsFrom = (int) (item.getStartTime().getTime() / 1000);
                int tsTo = (int) (item.getEndTime().getTime() / 1000);
                showDetail(tsFrom, tsTo, item, getChartsHost().getDevice());
            }
        });

        stepsDateView = rootView.findViewById(R.id.stepsDateView);
        FloatingActionButton fab;
        fab = rootView.findViewById(R.id.fab);
        fab.setVisibility(View.VISIBLE);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDashboard(tsDateTo, getChartsHost().getDevice());
            }
        });

        refresh();
        return rootView;
    }


    @Override
    public String getTitle() {
        return "Steps list";
    }

    @Override
    protected boolean isSingleDay() {
        return true;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ChartsHost.REFRESH.equals(action)) {
            // TODO: use LimitLines to visualize smart alarms?
            refresh();
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    protected MyChartsData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        List<? extends ActivitySample> activitySamples;
        activitySamples = getSamples(db, device);
        List<ActivitySession> stepSessions = null;
        ActivitySession ongoingSession = null;
        StepAnalysis stepAnalysis = new StepAnalysis();
        boolean isEmptySummary = false;

        if (activitySamples != null) {
            stepSessions = stepAnalysis.calculateStepSessions(activitySamples);
            if (stepSessions.size() == 0) {
                isEmptySummary = true;
            }
            ActivitySession stepSessionsSummary = stepAnalysis.calculateSummary(stepSessions, isEmptySummary);
            stepSessions.add(0, stepSessionsSummary);
            ActivitySession emptySession = new ActivitySession();
            emptySession.setSessionType(ActivitySession.SESSION_EMPTY);
            stepSessions.add(emptySession); //this is to have an empty item at the end to be able to use FAB without it blocking anything
            ongoingSession = stepAnalysis.getOngoingSessions(stepSessions);
        }
        return new MyChartsData(stepSessions, ongoingSession);
    }

    @Override
    protected void updateChartsnUIThread(MyChartsData mcd) {
        if (mcd == null) {
            return;
        }
        if (mcd.getStepSessions() == null) {
            return;
        }

        //noinspection RedundantIfStatement
        if (mcd.getStepSessions().size() == 0) {
            getChartsHost().enableSwipeRefresh(true); //enable pull to refresh, might be needed
        } else {
            getChartsHost().enableSwipeRefresh(false); //disable pull to refresh as it collides with swipable view
        }

        Date activityDate = new Date(tsDateTo * 1000L);
        stepsDateView.setText(DateTimeUtils.formatDate(activityDate, DateUtils.FORMAT_SHOW_WEEKDAY));

        if (GBApplication.getPrefs().getBoolean("charts_show_ongoing_activity", true)) {
            if (mcd.getOngoingSession() != null && DateUtils.isToday(activityDate.getTime())) {
                showOngoingActivitySnackbar(mcd.getOngoingSession());
            }
        }
        stepListAdapter.setItems(mcd.getStepSessions(), true);
    }

    @Override
    protected void renderCharts() {
    }

    @Override
    protected void setupLegend(Chart<?> chart) {
    }

    @Override
    protected List<? extends ActivitySample> getSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(tsTo * 1000L); //we need today initially, which is the end of the time range
        day.set(Calendar.HOUR_OF_DAY, 0); //and we set time for the start and end of the same day
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        tsFrom = (int) (day.getTimeInMillis() / 1000);
        tsTo = tsFrom + 24 * 60 * 60 - 1;
        tsDateTo = tsTo;
        return getAllSamples(db, device, tsFrom, tsTo);
    }

    private void showOngoingActivitySnackbar(ActivitySession ongoingSession) {

        String distanceLabel = FormatUtils.getFormattedDistanceLabel(ongoingSession.getDistance());
        String stepLabel = String.valueOf(ongoingSession.getActiveSteps());
        String durationLabel = DateTimeUtils.formatDurationHoursMinutes(ongoingSession.getEndTime().getTime() - ongoingSession.getStartTime().getTime(), TimeUnit.MILLISECONDS);
        String hrLabel = String.valueOf(ongoingSession.getHeartRateAverage());
        String activityName = ongoingSession.getActivityKind().getLabel(requireContext());
        int icon = ongoingSession.getActivityKind().getIcon();

        String text = String.format("%s:\u00A0%s, %s:\u00A0%s, %s:\u00A0%s, %s:\u00A0%s", activityName, durationLabel, getString(R.string.heart_rate), hrLabel, getString(R.string.steps), stepLabel, getString(R.string.distance), distanceLabel);
        final Snackbar snackbar = Snackbar.make(rootView, text, 1000 * 8);

        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(requireContext().getResources().getColor(R.color.accent));
        snackbar.setActionTextColor(Color.WHITE);
        snackbar.setAction(getString(R.string.dialog_hide).toUpperCase(), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        snackbar.dismiss();
                    }
                }
        );
        snackbar.show();
    }

    private void showDashboard(int date, GBDevice device) {
        FragmentManager fm = requireActivity().getSupportFragmentManager();
        ActivityListingDashboard listingDashboardFragment = ActivityListingDashboard.newInstance(date, device);
        listingDashboardFragment.show(fm, "activity_list_total_dashboard");
    }

    private void showDetail(int tsFrom, int tsTo, ActivitySession item, GBDevice device) {
        FragmentManager fm = requireActivity().getSupportFragmentManager();
        ActivityListingDetail listingDetailFragment = ActivityListingDetail.newInstance(tsFrom, tsTo, item, device);
        listingDetailFragment.show(fm, "activity_list_detail");
    }

    protected static final class MyChartsData extends ChartsData {
        private final List<ActivitySession> stepSessions;
        private final ActivitySession ongoingSession;

        MyChartsData(List<ActivitySession> stepSessions, ActivitySession ongoingSession) {
            this.stepSessions = stepSessions;
            this.ongoingSession = ongoingSession;
        }

        public List<ActivitySession> getStepSessions() {
            return stepSessions;
        }

        public ActivitySession getOngoingSession() {
            return this.ongoingSession;
        }
    }
}
