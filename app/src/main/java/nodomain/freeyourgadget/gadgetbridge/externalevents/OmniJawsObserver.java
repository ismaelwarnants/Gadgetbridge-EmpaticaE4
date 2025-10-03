/*  Copyright (C) 2017-2024 Carsten Pfeiffer, Daniele Gobbetti, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.weather.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.weather.WeatherMapper;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;


public class OmniJawsObserver extends ContentObserver {

    private static final Logger LOG = LoggerFactory.getLogger(OmniJawsObserver.class);

    private static final String SERVICE_PACKAGE = "org.omnirom.omnijaws";
    public static final Uri WEATHER_URI = Uri.parse("content://org.omnirom.omnijaws.provider/weather");
    private static final Uri SETTINGS_URI = Uri.parse("content://org.omnirom.omnijaws.provider/settings");

    private Context mContext;
    private boolean mInstalled;
    private boolean mEnabled = false;
    private boolean mMetric = true;


    private final String[] WEATHER_PROJECTION = new String[]{
            "city",
            "condition", //unused, see below
            "condition_code",
            "temperature",
            "humidity",
            "forecast_low",
            "forecast_high",
            "forecast_condition",
            "forecast_condition_code",
            "time_stamp",
            "forecast_date",
            "wind_speed",
            "wind_direction"
    };

    private final String[] SETTINGS_PROJECTION = new String[]{
            "enabled",
            "units"
    };

    public OmniJawsObserver(Handler handler) throws NameNotFoundException {
        super(handler);
        mContext = GBApplication.getContext();
        mInstalled = isOmniJawsServiceAvailable();
        LOG.info("OmniJaws installation status: " + mInstalled);
        checkSettings();
        LOG.info("OmniJaws is enabled: " + mEnabled);
        if (!mEnabled) {
            throw new NameNotFoundException();
        }

        updateWeather();
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        LOG.info("Weather update received");
        checkSettings();
        if (!mEnabled) {
            LOG.info("Provider was disabled, ignoring.");
            return;
        }
        queryWeather();
    }

    private void queryWeather() {
        Cursor c = mContext.getContentResolver().query(WEATHER_URI, WEATHER_PROJECTION, null, null, null);
        if (c != null) {
            try {

                WeatherSpec weatherSpec = new WeatherSpec();
                weatherSpec.setForecasts(new ArrayList<>());

                int count = c.getCount();
                if (count > 0) {
                    for (int i = 0; i < count; i++) {
                        c.moveToPosition(i);
                        if (i == 0) {

                            weatherSpec.setLocation(c.getString(0));
                            weatherSpec.setCurrentConditionCode(WeatherMapper.mapToOpenWeatherMapCondition(c.getInt(2)));
                            weatherSpec.setCurrentCondition(WeatherMapper.getConditionString(mContext, weatherSpec.getCurrentConditionCode()));
                            //alternatively the following would also be possible
                            //weatherSpec.currentCondition = c.getString(1);

                            weatherSpec.setCurrentTemp(toKelvin(c.getFloat(3)));
                            weatherSpec.setCurrentHumidity((int) c.getFloat(4));

                            weatherSpec.setWindSpeed(toKmh(c.getFloat(11)));
                            weatherSpec.setWindDirection(c.getInt(12));
                            weatherSpec.setTimestamp((int) (Long.parseLong(c.getString(9)) / 1000));
                        } else if (i == 1) {
                            weatherSpec.setTodayMinTemp(toKelvin(c.getFloat(5)));
                            weatherSpec.setTodayMaxTemp(toKelvin(c.getFloat(6)));
                        } else {

                            WeatherSpec.Daily gbForecast = new WeatherSpec.Daily();
                            gbForecast.setMinTemp(toKelvin(c.getFloat(5)));
                            gbForecast.setMaxTemp(toKelvin(c.getFloat(6)));
                            gbForecast.setConditionCode(WeatherMapper.mapToOpenWeatherMapCondition(c.getInt(8)));
                            weatherSpec.getForecasts().add(gbForecast);
                        }
                    }
                }

                ArrayList<WeatherSpec> weatherSpecs = new ArrayList<>(Collections.singletonList(weatherSpec));
                Weather.setWeatherSpec(weatherSpecs);
                GBApplication.deviceService().onSendWeather();

            } finally {
                c.close();
            }
        }

    }

    private void updateWeather() {
        Intent updateIntent = new Intent(Intent.ACTION_MAIN)
                .setClassName(SERVICE_PACKAGE, SERVICE_PACKAGE + ".WeatherService");
        updateIntent.setAction(SERVICE_PACKAGE + ".ACTION_UPDATE");
        mContext.startService(updateIntent);
    }

    private boolean isOmniJawsServiceAvailable() throws NameNotFoundException {
        final PackageManager pm = mContext.getPackageManager();
        pm.getPackageInfo("org.omnirom.omnijaws", 0);
        return true;
    }

    private void checkSettings() {
        if (!mInstalled) {
            return;
        }
        final Cursor c = mContext.getContentResolver().query(SETTINGS_URI, SETTINGS_PROJECTION,
                null, null, null);
        if (c != null) {
            int count = c.getCount();
            if (count == 1) {
                c.moveToPosition(0);
                mEnabled = c.getInt(0) == 1 ? true : false;
                mMetric = c.getInt(1) == 0 ? true : false;
            }
        }
    }

    private int toKelvin(double temperature) {
        if (mMetric) {
            return (int) (temperature + 273.15);
        }
        return (int) ((temperature - 32) * 0.5555555555555556D + 273.15);
    }

    private float toKmh(float speed) {
        if (mMetric) {
            return speed;
        }
        return (speed * 1.61f);
    }
}