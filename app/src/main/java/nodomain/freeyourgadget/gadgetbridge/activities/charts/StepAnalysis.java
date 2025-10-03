/*  Copyright (C) 2020-2024 Petr Vaněk

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.HeartRateUtils;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySession;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;

public class StepAnalysis {
    protected static final Logger LOG = LoggerFactory.getLogger(StepAnalysis.class);
    private int totalDailySteps = 0;

    public List<ActivitySession> calculateStepSessions(List<? extends ActivitySample> samples) {
        LOG.debug("get all samples activity sessions: {}", samples.size());
        List<ActivitySession> result = new ArrayList<>();
        ActivityUser activityUser = new ActivityUser();
        final int MIN_SESSION_LENGTH = 60 * GBApplication.getPrefs().getInt("chart_list_min_session_length", 5);
        final int MAX_IDLE_PHASE_LENGTH = 60 * GBApplication.getPrefs().getInt("chart_list_max_idle_phase_length", 5);
        final int MIN_STEPS_PER_MINUTE = GBApplication.getPrefs().getInt("chart_list_min_steps_per_minute", 40);
        int stepLengthCm = activityUser.getStepLengthCm();
        final double STEP_LENGTH_M = stepLengthCm * 0.01;
        final double MIN_SESSION_INTENSITY = Math.max(0, Math.min(1, MIN_STEPS_PER_MINUTE * 0.01));
        totalDailySteps = 0;

        ActivitySample previousSample = null;
        Date sessionStart = null;
        Date sessionEnd;
        int activeSteps = 0; //steps that we count
        int activeDistanceCm = 0;
        int stepsBetweenActivePeriods = 0; //steps during time when we maybe take a rest but then restart
        int distanceBetweenActivePeriods = 0;
        int durationSinceLastActiveStep = 0;
        ActivityKind activityKind;

        List<Integer> heartRateSum = new ArrayList<>();
        List<Integer> heartRateBetweenActivePeriodsSum = new ArrayList<>();

        float activeIntensity = 0;
        float intensityBetweenActivePeriods = 0;
        HeartRateUtils heartRateUtilsInstance = HeartRateUtils.getInstance();

        for (ActivitySample sample : samples) {
            int steps = sample.getSteps();
            if (steps > 0) {
                totalDailySteps += steps;
            }

            /*
             * FIXME This should only consider non-sleep samples. However, this always had the wrong
             *  check for that, so it processed everything. In #3977, that was corrected, which
             *  introduces a regression for some devices such as the Amazfit Bip. Processing everything
             *  seems to work, but this logic needs to be reviewed.
             */
            if (!(sample instanceof TrailingActivitySample)) { //trailing samples have wrong date and make trailing activity have 0 duration

                if (sessionStart == null) {
                    sessionStart = getDateFromSample(sample);
                    if (sample.getSteps() >= 0) {
                        activeSteps = sample.getSteps();
                    } else {
                        activeSteps = 0;
                    }
                    if (sample.getDistanceCm() >= 0) {
                        activeDistanceCm = sample.getDistanceCm();
                    } else if (activeSteps > 0) {
                        activeDistanceCm = activeSteps * stepLengthCm;
                    } else {
                        activeDistanceCm = 0;
                    }
                    activeIntensity = sample.getIntensity();
                    heartRateSum = new ArrayList<>();
                    if (heartRateUtilsInstance.isValidHeartRateValue(sample.getHeartRate())) {
                        heartRateSum.add(sample.getHeartRate());
                    }
                    durationSinceLastActiveStep = 0;
                    stepsBetweenActivePeriods = 0;
                    distanceBetweenActivePeriods = 0;
                    heartRateBetweenActivePeriodsSum = new ArrayList<>();
                    previousSample = null;
                }
                if (previousSample != null) {
                    int durationSinceLastSample = sample.getTimestamp() - previousSample.getTimestamp();

                    if (sample.getSteps() > MIN_STEPS_PER_MINUTE || //either some steps
                            (sample.getIntensity() > MIN_SESSION_INTENSITY && sample.getSteps() > 0)) { //or some intensity plus at least one step
                        activeSteps += sample.getSteps() + stepsBetweenActivePeriods;
                        if (sample.getDistanceCm() >= 0) {
                            activeDistanceCm += sample.getDistanceCm() + distanceBetweenActivePeriods;
                        } else {
                            activeDistanceCm += sample.getSteps() * stepLengthCm + distanceBetweenActivePeriods;
                        }
                        activeIntensity += sample.getIntensity() + intensityBetweenActivePeriods;
                        if (heartRateUtilsInstance.isValidHeartRateValue(sample.getHeartRate())) {
                            heartRateSum.add(sample.getHeartRate());
                        }
                        heartRateSum.addAll(heartRateBetweenActivePeriodsSum);
                        heartRateBetweenActivePeriodsSum = new ArrayList<>();
                        stepsBetweenActivePeriods = 0;
                        distanceBetweenActivePeriods = 0;
                        intensityBetweenActivePeriods = 0;
                        durationSinceLastActiveStep = 0;

                    } else { //short break data to remember, we will add it to the rest later, if break not too long
                        if (sample.getSteps() >= 0) {
                            stepsBetweenActivePeriods += sample.getSteps();
                        }
                        if (sample.getDistanceCm() >= 0) {
                            distanceBetweenActivePeriods += sample.getDistanceCm();
                        } else if (sample.getSteps() > 0) {
                            distanceBetweenActivePeriods += sample.getSteps() * stepLengthCm;
                        }
                        if (heartRateUtilsInstance.isValidHeartRateValue(sample.getHeartRate())) {
                            heartRateBetweenActivePeriodsSum.add(sample.getHeartRate());
                        }
                        durationSinceLastActiveStep += durationSinceLastSample;
                        intensityBetweenActivePeriods += sample.getIntensity();
                    }
                    if (durationSinceLastActiveStep >= MAX_IDLE_PHASE_LENGTH) { //break too long, we split here

                        int current = sample.getTimestamp();
                        int starting = (int) (sessionStart.getTime() / 1000);
                        int session_length = current - starting - durationSinceLastActiveStep;

                        if (session_length >= MIN_SESSION_LENGTH) { //valid activity session
                            int heartRateAverage = heartRateSum.size() > 0 ? calculateSumOfInts(heartRateSum) / heartRateSum.size() : 0;
                            float distance = activeDistanceCm * 0.01f;
                            sessionEnd = new Date((sample.getTimestamp() - durationSinceLastActiveStep) * 1000L);
                            activityKind = detect_activity_kind(session_length, activeSteps, heartRateAverage, activeIntensity);
                            ActivitySession activitySession = new ActivitySession(sessionStart, sessionEnd, activeSteps, heartRateAverage, activeIntensity, distance, activityKind);
                            //activitySession.setSessionType(ActivitySession.SESSION_ONGOING);
                            result.add(activitySession);
                        }
                        sessionStart = null;
                    }
                }
                previousSample = sample;
            }
        }
        //trailing activity: make sure we show the last portion of the data as well in case no further activity is recorded yet

        if (sessionStart != null) {
            int current = previousSample.getTimestamp();
            int starting = (int) (sessionStart.getTime() / 1000);
            int session_length = current - starting - durationSinceLastActiveStep;

            if (session_length >= MIN_SESSION_LENGTH) {
                int heartRateAverage = heartRateSum.size() > 0 ? calculateSumOfInts(heartRateSum) / heartRateSum.size() : 0;
                float distance = activeDistanceCm * 0.01f;
                sessionEnd = getDateFromSample(previousSample);
                activityKind = detect_activity_kind(session_length, activeSteps, heartRateAverage, activeIntensity);
                ActivitySession ongoingActivity = new ActivitySession(sessionStart, sessionEnd, activeSteps, heartRateAverage, activeIntensity, distance, activityKind);
                ongoingActivity.setSessionType(ActivitySession.SESSION_ONGOING);
                result.add(ongoingActivity);
            }
        }
        return result;
    }

    public ActivitySession calculateSummary(Collection<ActivitySession> sessions, boolean empty) {

        Date startTime = null;
        Date endTime = null;
        int stepsSum = 0;
        int heartRateAverage = 0;
        List<Integer> heartRateSum = new ArrayList<>();
        int distanceSum = 0;
        float intensitySum = 0;
        int sessionCount;
        long durationSum = 0;

        for (ActivitySession session : sessions) {
            startTime = session.getStartTime();
            endTime = session.getEndTime();
            durationSum += endTime.getTime() - startTime.getTime();
            stepsSum += session.getActiveSteps();
            distanceSum += session.getDistance();
            heartRateSum.add(session.getHeartRateAverage());
            intensitySum += session.getIntensity();
        }

        sessionCount = sessions.size();
        if (heartRateSum.size() > 0) {
            heartRateAverage = calculateSumOfInts(heartRateSum) / heartRateSum.size();
        }
        startTime = new Date(0);
        endTime = new Date(durationSum);

        ActivitySession stepSessionSummary = new ActivitySession(startTime, endTime,
                stepsSum, heartRateAverage, intensitySum, distanceSum, ActivityKind.UNKNOWN);

        stepSessionSummary.setSessionCount(sessionCount);
        stepSessionSummary.setSessionType(ActivitySession.SESSION_SUMMARY);
        stepSessionSummary.setEmptySummary(empty);


        stepSessionSummary.setTotalDaySteps(totalDailySteps);
        return stepSessionSummary;
    }

    public ActivitySession getOngoingSessions(Iterable<ActivitySession> sessions) {

        for (ActivitySession session : sessions) {
            if (session.getSessionType() == ActivitySession.SESSION_ONGOING) {
                return session;
            }
        }
        return null;
    }

    private int calculateSumOfInts(Iterable<Integer> samples) {
        int result = 0;
        for (Integer sample : samples) {
            result += sample;
        }
        return result;
    }

    private ActivityKind detect_activity_kind(int session_length, int activeSteps, int heartRateAverage, float intensity) {
        final int MIN_STEPS_PER_MINUTE_FOR_RUN = GBApplication.getPrefs().getInt("chart_list_min_steps_per_minute_for_run", 120);
        int spm = (int) (activeSteps / (session_length / 60));
        if (spm > MIN_STEPS_PER_MINUTE_FOR_RUN) {
            return ActivityKind.RUNNING;
        }
        if (activeSteps > 200) {
            return ActivityKind.WALKING;
        }
        if (heartRateAverage > 90 && intensity > 15) { //needs tuning
            return ActivityKind.EXERCISE;
        }
        return ActivityKind.ACTIVITY;
    }

    private Date getDateFromSample(ActivitySample sample) {
        return new Date(sample.getTimestamp() * 1000L);
    }
}
