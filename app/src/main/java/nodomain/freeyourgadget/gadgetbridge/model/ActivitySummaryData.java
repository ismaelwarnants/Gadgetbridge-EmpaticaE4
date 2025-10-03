/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummaryEntry;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummaryProgressEntry;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummarySimpleEntry;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummaryTableRowEntry;

/**
 * A small wrapper for a JSONObject, with helper methods to add activity summary data in the format
 * Gadgetbridge expects.
 */
public class ActivitySummaryData {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapterFactory(RuntimeTypeAdapterFactory
                    .of(ActivitySummaryEntry.class, "type")
                    .registerSubtype(ActivitySummarySimpleEntry.class, null) // no type for backwards compatibility
                    .registerSubtype(ActivitySummaryProgressEntry.class, "progress")
                    .registerSubtype(ActivitySummaryTableRowEntry.class, "tableRow")
                    .recognizeSubtypes()
            )
            //.serializeNulls()
            //.setPrettyPrinting()
            .create();

    private final LinkedHashMap<String, ActivitySummaryEntry> entries;

    public ActivitySummaryData() {
        this.entries = new LinkedHashMap<>();
    }

    public ActivitySummaryData(final LinkedHashMap<String, ActivitySummaryEntry> entries) {
        this.entries = entries;
    }

    public void add(final String key, final Number value, final String unit) {
        add(null, key, value, unit, false);
    }

    public void add(final String key, final Number value, final String unit, boolean forceDisplay) {
        add(null, key, value, unit, forceDisplay);
    }

    public void add(final String group, final String key, final Number value, final String unit) {
        add(group, key, value, unit, false);
    }

    public void add(final String group, final String key, final Number value, final String unit, boolean forceDisplay) {
        if (value != null && (value.doubleValue() != 0 || forceDisplay)) {
            entries.put(key, new ActivitySummarySimpleEntry(group, value, unit));
        }
    }

    public void add(final String key, final String value) {
        add(null, key, value);
    }

    public void add(final String group, final String key, final String value) {
        if (StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
            return;
        }

        entries.put(key, new ActivitySummarySimpleEntry(group, value, ActivitySummaryEntries.UNIT_STRING));
    }

    public void add(final String key, final ActivitySummaryEntry entry) {
        entries.put(key, entry);
    }

    public Set<String> getKeys() {
        return entries.keySet();
    }

    public ActivitySummaryEntry get(final String key) {
        return entries.get(key);
    }

    public boolean has(final String key) {
        return entries.containsKey(key);
    }

    public Number getNumber(final String key, final Number defaultValue) {
        final ActivitySummaryEntry entry = entries.get(key);
        if (!(entry instanceof ActivitySummarySimpleEntry)) {
            return defaultValue;
        }
        final ActivitySummarySimpleEntry simpleEntry = (ActivitySummarySimpleEntry) entry;
        final Object value = simpleEntry.getValue();
        if (!(value instanceof Number)) {
            return defaultValue;
        }

        return ((Number) value).doubleValue();
    }

    public boolean getBoolean(final String key, final boolean defaultValue) {
        final ActivitySummaryEntry entry = entries.get(key);
        if (!(entry instanceof ActivitySummarySimpleEntry)) {
            return defaultValue;
        }
        final ActivitySummarySimpleEntry simpleEntry = (ActivitySummarySimpleEntry) entry;
        final Object value = simpleEntry.getValue();
        if (value instanceof Boolean) {
            return (boolean) value;
        }

        if (!(value instanceof String)) {
            return defaultValue;
        }

        return Boolean.parseBoolean((String) value);
    }

    @NonNull
    public static ActivitySummaryData fromJson(final String string) {
        if (StringUtils.isBlank(string)) {
            return new ActivitySummaryData();
        }

        final Type type = new TypeToken<LinkedHashMap<String, ActivitySummaryEntry>>(){}.getType();
        final LinkedHashMap<String, ActivitySummaryEntry> entries = GSON.fromJson(string, type);

        return new ActivitySummaryData(entries);
    }

    @NonNull
    @Override
    public String toString() {
        return toJson();
    }

    public String toJson() {
        return GSON.toJson(entries);
    }

    public void addTotal(final Number value, final ActivityKind.CycleUnit unit) {
        switch (unit) {
            case STROKES:
                add(ActivitySummaryEntries.STROKES, value, ActivitySummaryEntries.UNIT_STROKES);
                break;
            case JUMPS:
                add(ActivitySummaryEntries.JUMPS, value, ActivitySummaryEntries.UNIT_JUMPS);
                break;
            case REPS:
                add(ActivitySummaryEntries.REPETITIONS, value, ActivitySummaryEntries.UNIT_REPS);
                break;
            case REVOLUTIONS:
                add(ActivitySummaryEntries.REVOLUTIONS, value, ActivitySummaryEntries.UNIT_REVS);
                break;
            default:
                add(ActivitySummaryEntries.STEPS, value, ActivitySummaryEntries.UNIT_STEPS);
        }
    }

    public void addCadenceAvg(final Number value, final ActivityKind.CycleUnit unit) {
        switch (unit) {
            case STROKES:
                add(ActivitySummaryEntries.STROKE_RATE_AVG, value, ActivitySummaryEntries.UNIT_STROKES_PER_MINUTE);
                break;
            case JUMPS:
                add(ActivitySummaryEntries.JUMP_RATE_AVG, value, ActivitySummaryEntries.UNIT_JUMPS_PER_MINUTE);
                break;
            case REPS:
                add(ActivitySummaryEntries.CADENCE_AVG, value, ActivitySummaryEntries.UNIT_REPS_PER_MINUTE);
                break;
            case REVOLUTIONS:
                add(ActivitySummaryEntries.CADENCE_AVG, value, ActivitySummaryEntries.UNIT_REVS_PER_MINUTE);
                break;
            default:
                add(ActivitySummaryEntries.CADENCE_AVG, value, ActivitySummaryEntries.UNIT_SPM);
        }
    }

    public void addCadenceMax(final Number value, final ActivityKind.CycleUnit unit) {
        switch (unit) {
            case STROKES:
                add(ActivitySummaryEntries.STROKE_RATE_MAX, value, ActivitySummaryEntries.UNIT_STROKES_PER_MINUTE);
                break;
            case JUMPS:
                add(ActivitySummaryEntries.JUMP_RATE_MAX, value, ActivitySummaryEntries.UNIT_JUMPS_PER_MINUTE);
                break;
            case REPS:
                add(ActivitySummaryEntries.CADENCE_MAX, value, ActivitySummaryEntries.UNIT_REPS_PER_MINUTE);
                break;
            case REVOLUTIONS:
                add(ActivitySummaryEntries.CADENCE_MAX, value, ActivitySummaryEntries.UNIT_REVS_PER_MINUTE);
                break;
            default:
                add(ActivitySummaryEntries.CADENCE_MAX, value, ActivitySummaryEntries.UNIT_SPM);
        }
    }
}
