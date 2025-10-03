/*  Copyright (C) 2024 Me7c7

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.p2p;


import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SYNC_CALENDAR;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiP2PManager;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiUploadManager;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendFileUploadInfo;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.calendar.CalendarEvent;
import nodomain.freeyourgadget.gadgetbridge.util.calendar.CalendarManager;

public class HuaweiP2PCalendarService extends HuaweiBaseP2PService {
    private final Logger LOG = LoggerFactory.getLogger(HuaweiP2PCalendarService.class);

    public final int OPERATION_ADD = 1;
    public final int OPERATION_DELETE = 2;
    public final int OPERATION_UPDATE = 3;
    
    public static final String MODULE = "hw.unitedevice.calendarapp";

    private final AtomicBoolean isRegistered = new AtomicBoolean(false);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable syncCallback = () -> sendCalendarCmd((byte) 0x01, (byte) 0x01, null);

    private List<CalendarEvent> lastCalendarEvents = null;

    public HuaweiP2PCalendarService(HuaweiP2PManager manager) {
        super(manager);
        LOG.info("HuaweiP2PCalendarService");
    }

    public static HuaweiP2PCalendarService getRegisteredInstance(HuaweiP2PManager manager) {
        return (HuaweiP2PCalendarService) manager.getRegisteredService(HuaweiP2PCalendarService.MODULE);
    }

    @Override
    public String getModule() {
        return HuaweiP2PCalendarService.MODULE;
    }

    @Override
    public String getPackage() {
        if (manager.getSupportProvider().getHuaweiCoordinator().supportsExternalCalendarService())
            return "com.huawei.ohos.calendar";
        return "in.huawei.calendar";
    }

    @Override
    public String getFingerprint() {
        if (manager.getSupportProvider().getHuaweiCoordinator().supportsExternalCalendarService())
            return "com.huawei.ohos.calendar_BCgpfcWNSKWgvxsSILxooQZyAmKYsFQnMTibnfrKQqK9M0ABtXH+GbsOscsnVvVc5qIDiFEyEOYMSF7gJ7Vb5Mc=";
        return "SystemApp";
    }

    @Override
    public void registered() {
        isRegistered.set(true);
        startSynchronization();
    }

    @Override
    public void unregister() {
        isRegistered.set(false);
        handler.removeCallbacks(syncCallback);
    }

    private void startSynchronization() {
        sendCalendarCmd((byte) 0x02, (byte) 0x01, null); // download calendar request but it does not work on my device
        sendCalendarCmd((byte) 0x01, (byte) 0x01, null); // send sync upload request
    }

    public void restartSynchronization() {
        if (isRegistered.get()) {
            scheduleUpdate(2000);
        }
    }

    public void scheduleUpdate(long delay) {
        handler.removeCallbacks(syncCallback);
        if (isRegistered.get()) {
            handler.postDelayed(syncCallback, delay);
        }
    }

    private void sendCalendarCmd(byte command, byte commandData, HuaweiP2PCallback callback) {
        sendPing((code, data) -> {
            if ((byte) code != (byte) 0xca)
                return;
            // NOTE: basically this is TLV with one tag 0x1, But I have no reasons to create additional classes for this.
            sendCommand(new byte[]{command, 0x01, 0x01, commandData}, callback);
        });
    }

    public void onAddCalendarEvent(final CalendarEventSpec calendarEventSpec) {
        LOG.info("onAddCalendarEvent {}", calendarEventSpec.id);
        scheduleUpdate(2000);
    }

    public void onDeleteCalendarEvent(final byte type, long id) {
        LOG.info("onDeleteCalendarEvent {} {}", type, id);
        scheduleUpdate(2000);
    }

    private String truncateToHexBytes(String str, int count) {
        if(str == null)
            return "";
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > count) {
            int len = (int)Math.ceil(count/2.0) + 1;
            ByteBuffer buffer = ByteBuffer.wrap(bytes, 0, len - 3);
            CharBuffer cb = CharBuffer.allocate(len);
            CharsetDecoder cd = StandardCharsets.UTF_8.newDecoder();
            cd.onMalformedInput(CodingErrorAction.IGNORE);
            cd.decode(buffer, cb, true);
            cd.flush(cb);
            return new String(cb.array(), 0, cb.position()) + "...";
        }
        return str;
    }


    private String getFileCalendarName() {
        long millis = System.currentTimeMillis();
        return String.format(Locale.ROOT, "calendar_data_%d.json", millis);
    }

    private String prepareEventUUID(long id, long begin) {
        return String.valueOf(id) + "_" + begin;
    }

    static <T> T valueOrEmpty(T val, T def) {
        return val != null ? val : def;
    }

    private JsonObject calendarEventToJson(CalendarEvent calendarEvent, int operation) {
        JsonObject ret = new JsonObject();

        // NOTE: Calendar contain reminders already in required format. But GB reformat them.
        // So we need to reformat them back.
        StringBuilder reminders = new StringBuilder();
        for (long rem : calendarEvent.getRemindersAbsoluteTs()) {
            reminders.append(String.valueOf((calendarEvent.getBegin() - rem) / 60 / 1000L)).append(",");
        }

        String eventUUID = prepareEventUUID(calendarEvent.getId(), calendarEvent.getBegin());

        long startDate;
        long endDate;

        if (calendarEvent.isAllDay()) {
            startDate = DateTimeUtils.utcDateTimeToLocal(calendarEvent.getBegin());
            endDate = DateTimeUtils.utcDateTimeToLocal(calendarEvent.getEnd());
        } else {
            startDate = calendarEvent.getBegin();
            endDate = calendarEvent.getEnd();
        }

        ret.addProperty("account_name", truncateToHexBytes(calendarEvent.getCalAccountName(), 64));
        ret.addProperty("account_type", truncateToHexBytes(calendarEvent.getCalAccountType(), 64));
        ret.addProperty("all_day", calendarEvent.isAllDay() ? 1 : 0);
        ret.addProperty("calendar_color", calendarEvent.getColor());
        ret.addProperty("calendar_displayName", truncateToHexBytes(calendarEvent.getCalName(), 64));
        ret.addProperty("calendar_id", valueOrEmpty(calendarEvent.getCalendarId(), "0"));
        ret.addProperty("description", truncateToHexBytes(calendarEvent.getDescription(), 512));
        ret.addProperty("dtend", endDate);
        ret.addProperty("dtstart", startDate);
        ret.addProperty("event_id", String.valueOf(calendarEvent.getId()));
        ret.addProperty("event_location", truncateToHexBytes(calendarEvent.getLocation(), 256));
        ret.addProperty("event_uuid", eventUUID);
        ret.addProperty("has_alarm", (reminders.length() == 0) ? 0 : 1);
        ret.addProperty("minutes", reminders.toString());
        ret.addProperty("operation", operation);
        ret.addProperty("rrule", valueOrEmpty(calendarEvent.getRrule(),""));
        // TODO: Retrieve from CalendarContract.CalendarAlerts, field state
        // TODO: see handleData function command ID 3 for details
        ret.addProperty("state", -1);
        ret.addProperty("title", truncateToHexBytes(calendarEvent.getTitle(), 500));
        return ret;
    }

    private JsonObject calendarDeletedEventToJson(CalendarEvent calendarEvent) {
        JsonObject ret = new JsonObject();

        String eventUUID = prepareEventUUID(calendarEvent.getId(), calendarEvent.getBegin());

        ret.addProperty("account_name", "");
        ret.addProperty("account_type", "");
        ret.addProperty("all_day", 0);
        ret.addProperty("calendar_color", 0);
        ret.addProperty("calendar_displayName", "");
        ret.addProperty("calendar_id", "");
        ret.addProperty("description", "");
        ret.addProperty("dtend", 0);
        ret.addProperty("dtstart", 0);
        ret.addProperty("event_id", String.valueOf(calendarEvent.getId()));
        ret.addProperty("event_location", "");
        ret.addProperty("event_uuid", eventUUID);
        ret.addProperty("has_alarm", 0);
        ret.addProperty("minutes", "");
        ret.addProperty("operation", OPERATION_DELETE);
        ret.addProperty("rrule", "");
        ret.addProperty("state", 0);
        ret.addProperty("title", "");
        return ret;
    }

    private List<CalendarEvent> getCalendarEventList() {
        final CalendarManager upcomingEvents = new CalendarManager(manager.getSupportProvider().getContext(), manager.getSupportProvider().getDevice().getAddress());
        return upcomingEvents.getCalendarEventList(7);
    }

    private JsonArray getFullCalendarData() {
        final List<CalendarEvent> calendarEvents = getCalendarEventList();

        JsonArray events = new JsonArray();
        for (final CalendarEvent calendarEvent : calendarEvents) {
            events.add(calendarEventToJson(calendarEvent, OPERATION_ADD));
        }

        lastCalendarEvents = calendarEvents;
        return events;
    }

    private JsonArray getUpdateCalendarData() {
        final List<CalendarEvent> calendarEvents = getCalendarEventList();

        List<CalendarEvent> newEvents = new ArrayList<>();
        List<CalendarEvent> updatedEvents = new ArrayList<>();

        Map<Pair<Long, Long>, CalendarEvent> lastEventsIds = new HashMap<>();
        for (CalendarEvent evt : lastCalendarEvents) {
            lastEventsIds.put(new Pair<>(evt.getId(),  evt.getBegin()), evt);
        }

        for (CalendarEvent evt : calendarEvents) {
            CalendarEvent lastEvt = lastEventsIds.remove((new Pair<>(evt.getId(),  evt.getBegin())));
            if (lastEvt == null) {
                newEvents.add(evt);
            } else {
                if(!lastEvt.equals(evt)) {
                    updatedEvents.add(evt);
                }
            }
        }

        List<CalendarEvent> removedEvents = new ArrayList<>(lastEventsIds.values());


        JsonArray events = new JsonArray();

        for (final CalendarEvent calendarEvent : updatedEvents) {
            LOG.info("Update: {}", prepareEventUUID(calendarEvent.getId(), calendarEvent.getBegin()));
            events.add(calendarEventToJson(calendarEvent, OPERATION_UPDATE));
        }

        for (final CalendarEvent calendarEvent : newEvents) {
            LOG.info("New: {}", prepareEventUUID(calendarEvent.getId(), calendarEvent.getBegin()));
            events.add(calendarEventToJson(calendarEvent, OPERATION_ADD));
        }

        for (final CalendarEvent calendarEvent : removedEvents) {
            LOG.info("Remove: {}", prepareEventUUID(calendarEvent.getId(), calendarEvent.getBegin()));
            events.add(calendarDeletedEventToJson(calendarEvent));
        }

        lastCalendarEvents = calendarEvents;
        return events;
    }

    private byte[] getCalendarFileContent(String majorVersion, short minorVersion, JsonArray scheduleList) {
        JsonObject syncData = new JsonObject();
        syncData.addProperty("major", majorVersion);
        syncData.addProperty("minor", minorVersion);
        syncData.add("scheduleList", scheduleList);

        String data = new Gson().toJson(syncData);
        LOG.info(data);

        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        ByteBuffer sendData = ByteBuffer.allocate(dataBytes.length + 8); // 8 is data header
        //NOTE: minor version is short in response but in this case it writes as integer
        sendData.putInt(minorVersion);
        sendData.putInt(dataBytes.length);
        sendData.put(dataBytes);

        return sendData.array();
    }

    private boolean sendCalendarFile(String majorVersion, short minorVersion, JsonArray calendarData) {
        LOG.info("Send calendar file upload info");

        if (majorVersion == null || majorVersion.isEmpty()) {
            majorVersion = new String(this.manager.getSupportProvider().getAndroidId(), StandardCharsets.UTF_8);
        }

        byte[] data = getCalendarFileContent(majorVersion, minorVersion, calendarData);

        HuaweiUploadManager.FileUploadInfo fileInfo = new HuaweiUploadManager.FileUploadInfo();

        fileInfo.setFileType((byte) 7);
        fileInfo.setFileName(getFileCalendarName());
        fileInfo.setBytes(data);
        fileInfo.setSrcPackage(this.getModule());
        fileInfo.setDstPackage(this.getPackage());
        fileInfo.setSrcFingerprint(this.getLocalFingerprint());
        fileInfo.setDstFingerprint(this.getFingerprint());

        fileInfo.setFileUploadCallback(new HuaweiUploadManager.FileUploadCallback() {
            @Override
            public void onUploadStart() {
                // TODO: set device as busy in this case. But maybe exists another way to do this. Currently user see text on device card.
                // Also text should be changed
                manager.getSupportProvider().getDevice().setBusyTask(R.string.updating_firmware, manager.getSupportProvider().getContext());
                manager.getSupportProvider().getDevice().sendDeviceUpdateIntent(manager.getSupportProvider().getContext());
            }

            @Override
            public void onUploadProgress(int progress) {
            }

            @Override
            public void onUploadComplete() {
                if (manager.getSupportProvider().getDevice().isBusy()) {
                    manager.getSupportProvider().getDevice().unsetBusyTask();
                    manager.getSupportProvider().getDevice().sendDeviceUpdateIntent(manager.getSupportProvider().getContext());
                }
                if(lastCalendarEvents == null) {
                    scheduleUpdate(100);
                }
            }

            @Override
            public void onError(int code) {
                // TODO: maybe we should retry resend on error, at least 3 times.
                // currently I don't understand the mandatory of this action because file sends always successfully,
            }
        });

        HuaweiUploadManager huaweiUploadManager = this.manager.getSupportProvider().getUploadManager();

        huaweiUploadManager.setFileUploadInfo(fileInfo);

        try {
            SendFileUploadInfo sendFileUploadInfo = new SendFileUploadInfo(this.manager.getSupportProvider(), huaweiUploadManager);
            sendFileUploadInfo.doPerform();
        } catch (IOException e) {
            LOG.error("Failed to send file upload info", e);
        }
        return true;
    }


    private boolean syncCalendarEvents(String majorVersion, short minorVersion, short scheduleCount) {
        LOG.info("Sync calendar file upload info");

        JsonArray calendarData;
        if (TextUtils.isEmpty(majorVersion) || lastCalendarEvents == null || minorVersion == 0) {
            if(lastCalendarEvents == null && minorVersion != 0) {
                minorVersion = 0;
                calendarData = new JsonArray();
            } else {
                calendarData = getFullCalendarData();
                if (calendarData.isEmpty()) {
                    if (minorVersion == 0 && !TextUtils.isEmpty(majorVersion)) {
                        return false;
                    }
                    minorVersion = 0;
                } else {
                    minorVersion++;
                }
            }
        } else {
            calendarData = getUpdateCalendarData();
            if (calendarData.isEmpty())
                return false;
        }


        if (calendarData.size() > scheduleCount) {
            JsonArray newData = new JsonArray();
            for(int i = 0; i< scheduleCount; i++) {
                newData.add(calendarData.get(i));
            }
            calendarData = newData;
        }

        return sendCalendarFile(majorVersion, minorVersion, calendarData);
    }

    @Override
    public void handleData(byte[] data) {
        try {
            byte commandId = data[0];
            if (commandId == 1) {
                HuaweiTLV tlv = new HuaweiTLV();
                tlv.parse(data, 1, data.length - 1);
                byte operateMode = -1;
                String majorVersion = null;
                short minorVersion = -1;
                short scheduleCount = -1;

                if (tlv.contains(0x1))
                    operateMode = tlv.getByte(0x1);
                if (tlv.contains(0x2))
                    majorVersion = tlv.getString(0x2).trim();
                if (tlv.contains(0x3))
                    minorVersion = tlv.getShort(0x3);
                if (tlv.contains(0x4))
                    scheduleCount = tlv.getShort(0x4);

                LOG.info("Operate mode: {} Major: {} Minor: {} Schedule Count: {}", operateMode, majorVersion, minorVersion, scheduleCount);

                // NOTE: device can initiate calendar sync. So we need to check and answer properly.
                final boolean syncEnabled = GBApplication.getDeviceSpecificSharedPrefs(manager.getSupportProvider().getDevice().getAddress()).getBoolean(PREF_SYNC_CALENDAR, false);
                if(!syncEnabled) {
                    sendCalendarCmd((byte) 0x01, (byte) 0x07, null);  //sync disabled
                    return;
                }

                if (operateMode != -1 && majorVersion != null && minorVersion != -1 && scheduleCount != -1) {
                    if (operateMode == 3) {
                        sendCalendarCmd((byte) 0x01, (byte) 0x03, null);
                    }
                    if (operateMode == 2 || operateMode == 3) {

                        //TODO:
                        //sendCalendarCmd((byte) 0x01, (byte) 0x06, null);  //no permissions

                        // NOTE: scheduleCount is a max number of events to send. It suitable only if supportsExternalCalendarService not set
                        //external calendar synchronization only supported on Harmony devices. I don't know how to deal with this.
                        if (!manager.getSupportProvider().getHuaweiCoordinator().supportsExternalCalendarService()) {
                            if (!syncCalendarEvents(majorVersion, minorVersion, scheduleCount)) {
                                sendCalendarCmd((byte) 0x01, (byte) 0x04, null);  //No sync required
                            }
                        }
                    }
                }

            } else if (commandId == 3) {
                HuaweiTLV tlv = new HuaweiTLV();
                tlv.parse(data, 1, data.length - 1);
                //TODO: wearable sends this command if calendar event status changed.
                // For example one of the quick action buttons on the reminders notification wes pressed.
                // There are two buttons:
                //   Snooze - status 0
                //   Close - status 2
                // It should be send to Android or stored to local GB database. And should be used during next synchronization.
                // Should be saved to CalendarContract.CalendarAlerts, column "state".
                // CalendarContract.CalendarAlertsColumns.STATE
                // Values:
                //    STATE_DISMISSED = 2
                //    STATE_FIRED = 1
                //    STATE_SCHEDULED = 0
                // Currently GB does not support CALENDAR_WRITE permission so it is not possible.
                // Additional research required.
                LOG.info("calendarRequest");
                if (tlv.contains(0x1))
                    LOG.info("eventId: {}", tlv.getString(0x1).trim());
                if (tlv.contains(0x2))
                    LOG.info("calendarId: {}", tlv.getString(0x2).trim());
                if (tlv.contains(0x3))
                    LOG.info("accountName: {}", tlv.getString(0x3).trim());
                if (tlv.contains(0x4))
                    LOG.info("accountType: {}", tlv.getString(0x4).trim());
                if (tlv.contains(0x5))
                    LOG.info("state: {}", tlv.getByte(0x5)); //state: 0 - snooze, 2 - close. Quick actions on watch
                if (tlv.contains(0x6))
                    LOG.info("eventUuid: {}", tlv.getString(0x6).trim());
            } else {
                LOG.info("Unknown command");
            }
        } catch (HuaweiPacket.MissingTagException e) {
            LOG.error("P2P handle packet: tag is missing");
        }
    }
}
