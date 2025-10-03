package nodomain.freeyourgadget.gadgetbridge.activities.workouts;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_CM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_KG;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_KILOMETERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_LB;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_METERS_PER_SECOND;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_MINUTES_PER_100_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_MINUTES_PER_100_YARDS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_MINUTES_PER_KM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_MM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS_PER_100_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS_PER_100_YARDS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS_PER_KM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS_PER_M;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public class WorkoutValueFormatter {
    private static final Logger LOG = LoggerFactory.getLogger(WorkoutValueFormatter.class);

    private boolean show_raw_data = false;

    private final String units;
    private final String UNIT_IMPERIAL;
    private final String UNIT_METRIC;
    private final DecimalFormat df = new DecimalFormat("#.##");

    public WorkoutValueFormatter() {
        this.units = GBApplication.getPrefs().getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, GBApplication.getContext().getString(R.string.p_unit_metric));
        this.UNIT_IMPERIAL = GBApplication.getContext().getString(R.string.p_unit_imperial);
        this.UNIT_METRIC = GBApplication.getContext().getString(R.string.p_unit_metric);
    }

    public void toggleRawData() {
        this.show_raw_data = !show_raw_data;
    }

    public String formatValue(final Object rawValue, String unit, boolean showUnit) {
        if (rawValue == null) {
            return GBApplication.getContext().getString(R.string.stats_empty_value);
        }

        if (ActivitySummaryEntries.UNIT_RAW_STRING.equals(unit)) {
            return String.valueOf(rawValue);
        }

        if (rawValue instanceof CharSequence || ActivitySummaryEntries.UNIT_STRING.equals(unit)) {
            // we could optimize here a bit and only do this for particular activities (swim at the moment...)
            try {
                return getStringResourceByName(String.valueOf(rawValue));
            } catch (final Exception e) {
                LOG.error("Failed to get string resource by name for {}", rawValue);
                return String.valueOf(rawValue);
            }
        }

        if (!(rawValue instanceof Number)) {
            return String.valueOf(rawValue);
        }

        double value = ((Number) rawValue).doubleValue();

        if (!show_raw_data) {
            //special casing here + imperial units handling

            if (UNIT_MM.equals(unit)) {
                if (value > 1000) {
                    unit = UNIT_METERS;
                    value /= 1000d;
                } else if (value > 100) {
                    unit = UNIT_CM;
                    value /= 10d;
                }
            }

            switch (unit) {
                case UNIT_KG:
                    if (units.equals(UNIT_IMPERIAL)) {
                        value = value * 2.2046226f;
                        unit = UNIT_LB;
                    }
                    break;
                case UNIT_LB:
                    if (units.equals(UNIT_METRIC)) {
                        value = value / 2.2046226f;
                        unit = UNIT_KG;
                    }
                    break;
                case UNIT_CM:
                    if (units.equals(UNIT_IMPERIAL)) {
                        value = value * 0.0328084;
                        unit = "ft";
                    }
                    break;
                case UNIT_METERS_PER_SECOND:
                    if (units.equals(UNIT_IMPERIAL)) {
                        value = value * 2.236936D;
                        unit = "mi_h";
                    } else { //metric
                        value = value * 3.6;
                        unit = "km_h";
                    }
                    break;
                case UNIT_SECONDS_PER_M:
                    if (units.equals(UNIT_IMPERIAL)) {
                        value = value * (1609.344 / 60D);
                        unit = "minutes_mi";
                    } else { //metric
                        value = value * (1000 / 60D);
                        unit = "minutes_km";
                    }
                    break;
                case UNIT_SECONDS_PER_KM:
                    if (units.equals(UNIT_IMPERIAL)) {
                        value = value / 60D * 1.609344;
                        unit = "minutes_mi";
                    } else { //metric
                        value = value / 60D;
                        unit = "minutes_km";
                    }
                    break;
                case UNIT_KILOMETERS:
                    if (units.equals(UNIT_IMPERIAL)) {
                        value = value * 0.621371D;
                        unit = "mi";
                    }
                    break;
                case UNIT_METERS:
                    if (units.equals(UNIT_IMPERIAL)) {
                        value = value * 3.28084D;
                        unit = "ft";
                        if (value > 6000) {
                            value = value * 0.0001893939D;
                            unit = "mi";
                        }
                    } else { //metric
                        if (value > 2000) {
                            value = value / 1000;
                            unit = "km";
                        }
                    }
                    break;
                case UNIT_SECONDS_PER_100_METERS:
                    if (units.equals(UNIT_IMPERIAL)) {
                        value = (value * 0.9144) / 60D;
                        unit = UNIT_MINUTES_PER_100_YARDS;
                    } else { //metric
                        value = value / 60D;
                        unit = UNIT_MINUTES_PER_100_METERS;
                    }
                    break;
                case UNIT_SECONDS_PER_100_YARDS:
                    if (units.equals(UNIT_IMPERIAL)) {
                        value = value / 60D;
                        unit = UNIT_MINUTES_PER_100_YARDS;
                    } else { //metric
                        value = (value * 1.0936133D) / 60D;
                        unit = UNIT_MINUTES_PER_100_METERS;
                    }
                    break;
            }
        }

        if (unit.equals("seconds") && !show_raw_data && showUnit) { //rather then plain seconds, show formatted duration
            return DateTimeUtils.formatDurationHoursMinutes((long) value, TimeUnit.SECONDS);
        } else if (unit.equals("minutes_km") || unit.equals("minutes_mi") || unit.equals("minutes_100m") || unit.equals("minutes_100yd")) {
            // Format pace
            String format = showUnit ? "%d:%02d %s" : "%d:%02d";
            return String.format(
                    Locale.getDefault(),
                    format,
                    (int) Math.floor(value), (int) Math.round(60 * (value - (int) Math.floor(value))),
                    getStringResourceByName(unit)
            );
        } else {
            String format = showUnit ? "%s %s" : "%s";
            return String.format(format, df.format(value), getStringResourceByName(unit));
        }
    }

    public String formatValue(final Object rawValue, String unit) {
        return formatValue(rawValue, unit, true);
    }

    public String getStringResourceByName(String aString) {
        String packageName = BuildConfig.APPLICATION_ID;
        int resId = GBApplication.getContext().getResources().getIdentifier(aString, "string", packageName);
        if (resId == 0) {
            //LOG.warn("SportsActivity " + "Missing string in strings:" + aString);
            return aString;
        } else {
            return GBApplication.getContext().getString(resId);
        }
    }
}
