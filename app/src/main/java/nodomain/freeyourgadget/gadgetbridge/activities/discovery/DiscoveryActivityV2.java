/*  Copyright (C) 2023-2024 Andreas Böhler, Daniel Dakhno, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.activities.discovery;

import static nodomain.freeyourgadget.gadgetbridge.util.GB.toast;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.AuthKeyActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.DebugActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.adapter.DeviceCandidateAdapter;
import nodomain.freeyourgadget.gadgetbridge.adapter.SimpleIconListAdapter;
import nodomain.freeyourgadget.gadgetbridge.adapter.SpinnerWithIconAdapter;
import nodomain.freeyourgadget.gadgetbridge.adapter.SpinnerWithIconItem;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.RunnableListIconItem;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BleNamesResolver;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.BondingInterface;
import nodomain.freeyourgadget.gadgetbridge.util.BondingUtil;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;


public class DiscoveryActivityV2 extends AbstractGBActivity implements AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener,
        BondingInterface,
        GBScanEventProcessor.Callback,
        MenuProvider {
    private static final Logger LOG = LoggerFactory.getLogger(DiscoveryActivityV2.class);

    private static final int CHILD_RESULT = 0x826983; // "RES" as ASCII hex
    private final Handler handler = new Handler();

    private static final long SCAN_DURATION = 30000; // 30s
    private static final long LIST_REFRESH_THRESHOLD_MS = 1000L;
    private long lastListRefresh = System.currentTimeMillis();

    private final ScanCallback bleScanCallback = new BleScanCallback();

    private ProgressBar bluetoothProgress;

    private DeviceCandidateAdapter deviceCandidateAdapter;
    private GBDeviceCandidate deviceTarget;
    private BluetoothAdapter adapter;

    private Button startButton;
    private boolean scanning;

    private ActivityResultLauncher<Intent> authKeyLauncher;

    private long selectedUnsupportedDeviceKey = DebugActivity.SELECT_DEVICE;

    private final Runnable stopRunnable = () -> {
        stopDiscovery();
        LOG.info("Discovery stopped by thread timeout.");
    };

    private final BroadcastReceiver bluetoothReceiver = new BluetoothReceiver();

    private final GBScanEventProcessor deviceFoundProcessor = new GBScanEventProcessor(this);

    // Array to back the adapter for the UI
    private final ArrayList<GBDeviceCandidate> deviceCandidates = new ArrayList<>();

    @RequiresApi(Build.VERSION_CODES.O)
    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHILD_RESULT && resultCode == RESULT_OK) {
            // A device with a custom pairing activity has finished and indicated that the discovery activity should be
            // closed.
            finish();
        } else {
            BondingUtil.handleActivityResult(this, requestCode, resultCode, data);
        }
    }

    @Nullable
    private GBDeviceCandidate getCandidateFromMAC(final BluetoothDevice device) {
        for (final GBDeviceCandidate candidate : deviceCandidates) {
            if (candidate.getMacAddress().equals(device.getAddress())) {
                return candidate;
            }
        }
        LOG.warn("This shouldn't happen unless the list somehow emptied itself, device MAC: {}", device.getAddress());
        return null;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadSettings();

        setContentView(R.layout.activity_discovery);

        addMenuProvider(this);

        startButton = findViewById(R.id.discovery_start);
        startButton.setOnClickListener(v -> toggleDiscovery());

        bluetoothProgress = findViewById(R.id.discovery_progressbar);
        bluetoothProgress.setProgress(0);
        bluetoothProgress.setIndeterminate(true);
        bluetoothProgress.setVisibility(View.GONE);

        deviceCandidateAdapter = new DeviceCandidateAdapter(this, deviceCandidates);

        final ListView deviceCandidatesView = findViewById(R.id.discovery_device_candidates_list);
        deviceCandidatesView.setAdapter(deviceCandidateAdapter);
        deviceCandidatesView.setOnItemClickListener(this);
        deviceCandidatesView.setOnItemLongClickListener(this);

        registerBroadcastReceivers();

        checkAndRequestLocationPermission();

        authKeyLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        final Intent data = result.getData();
                        if (data == null) {
                            // Should never happen
                            GB.toast(this, "Auth data is null", Toast.LENGTH_LONG, GB.ERROR);
                            return;
                        }
                        final GBDeviceCandidate deviceCandidate = data.getParcelableExtra(AuthKeyActivity.EXTRA_DEVICE_CANDIDATE_RESULT);
                        final DeviceType deviceType = DeviceHelper.getInstance().resolveDeviceType(deviceCandidate);
                        startPair(deviceCandidate, deviceType.getDeviceCoordinator());
                    }
                });

        if (!startDiscovery()) {
            /* if we couldn't start scanning, go back to the main page.
            A toast will have been shown explaining what's wrong */
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("deviceCandidates", deviceCandidates);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        final List<Parcelable> restoredCandidates = savedInstanceState.getParcelableArrayList("deviceCandidates");
        if (restoredCandidates != null) {
            deviceCandidates.clear();
            for (final Parcelable p : restoredCandidates) {
                final GBDeviceCandidate candidate = (GBDeviceCandidate) p;
                deviceCandidates.add(candidate);
            }
        }
    }

    @Override
    protected void onDestroy() {
        unregisterBroadcastReceivers();
        stopDiscovery();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        unregisterBroadcastReceivers();
        stopDiscovery();
        super.onStop();
    }

    @Override
    protected void onPause() {
        unregisterBroadcastReceivers();
        stopDiscovery();
        super.onPause();
    }

    @Override
    protected void onResume() {
        loadSettings();
        registerBroadcastReceivers();
        super.onResume();
    }

    private void refreshDeviceList(final boolean throttle) {
        handler.post(() -> {
            if (throttle && System.currentTimeMillis() - lastListRefresh < LIST_REFRESH_THRESHOLD_MS) {
                return;
            }

            LOG.debug("Refreshing device list");

            // Clear and re-populate the list. deviceFoundProcessor keeps insertion order, so newer devices
            // will still be at the end
            deviceCandidates.clear();
            deviceCandidates.addAll(deviceFoundProcessor.getDevices());

            deviceCandidateAdapter.notifyDataSetChanged();

            lastListRefresh = System.currentTimeMillis();
        });
    }

    private void toggleDiscovery() {
        if (scanning) {
            stopDiscovery();
        } else {
            startDiscovery();
        }
    }

    private boolean startDiscovery() {
        if (scanning) {
            LOG.warn("Not starting discovery, because already scanning.");
            return false;
        }

        LOG.info("Starting discovery");
        startButton.setText(getString(R.string.discovery_stop_scanning));

        deviceFoundProcessor.clear();
        deviceFoundProcessor.start();

        refreshDeviceList(false);

        // Pre-add currently connected devices, as those will not trigger discovery events
        // Paired devices that are not connected do not need to be added, as those will be discovered
        try {
            final Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
            for (final BluetoothDevice device : pairedDevices) {
                try {
                    final Method isConnectedMethod = device.getClass().getMethod("isConnected");
                    final Boolean isConnected = (Boolean) isConnectedMethod.invoke(device);
                    if (isConnected != null && isConnected) {
                        LOG.debug("Pre-adding already bonded device {}", device.getAddress());
                        deviceFoundProcessor.scheduleProcessing(new GBScanEvent(device, (short) -1, null));
                    }
                } catch (final Exception e) {
                    LOG.error("Failed to check whether {} is connected", device.getAddress());
                }
            }
        } catch (final SecurityException e) {
            LOG.error("Failed to pre-add paired devices", e);
        }

        try {
            if (!ensureBluetoothReady()) {
                toast(DiscoveryActivityV2.this, getString(R.string.discovery_enable_bluetooth), Toast.LENGTH_SHORT, GB.ERROR);
                return false;
            }

            if (GB.supportsBluetoothLE()) {
                startBTLEDiscovery();
            }
            startBTDiscovery();

            bluetoothProgress.setVisibility(View.VISIBLE);
        } catch (final SecurityException e) {
            LOG.error("SecurityException on startDiscovery");
            deviceFoundProcessor.stop();
            return false;
        }

        setScanning(true);

        return true;
    }

    private void stopDiscovery() {
        LOG.info("Stopping discovery");
        try {
            stopBTDiscovery();
            stopBLEDiscovery();
        } catch (final SecurityException e) {
            LOG.error("SecurityException on stopDiscovery");
        }
        setScanning(false);
        deviceFoundProcessor.stop();
        handler.removeMessages(0, stopRunnable);

        // Refresh the device list one last time when finishing
        refreshDeviceList(false);
    }

    public void setScanning(final boolean scanning) {
        this.scanning = scanning;
        if (scanning) {
            startButton.setText(getString(R.string.discovery_stop_scanning));
        } else {
            startButton.setText(getString(R.string.discovery_start_scanning));
            bluetoothProgress.setVisibility(View.GONE);
        }
    }

    @RequiresPermission("android.permission.BLUETOOTH_SCAN")
    private void startBTLEDiscovery() {
        LOG.info("Starting BLE discovery");

        handler.removeMessages(0, stopRunnable);
        handler.sendMessageDelayed(getPostMessage(stopRunnable), SCAN_DURATION);

        // Filters being non-null would be a very good idea with background scan, but in this case,
        // not really required.
        // TODO getScanFilters maybe
        adapter.getBluetoothLeScanner().startScan(null, getScanSettings(), bleScanCallback);

        LOG.debug("Bluetooth LE discovery started successfully");
    }

    @RequiresPermission("android.permission.BLUETOOTH_SCAN")
    private void stopBLEDiscovery() {
        if (adapter == null) {
            return;
        }

        final BluetoothLeScanner bluetoothLeScanner = adapter.getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            LOG.warn("Could not get BluetoothLeScanner()!");
            return;
        }

        if (bleScanCallback == null) {
            LOG.warn("newLeScanCallback == null!");
            return;
        }

        try {
            bluetoothLeScanner.stopScan(bleScanCallback);
        } catch (final NullPointerException e) {
            LOG.warn("Internal NullPointerException when stopping the scan!");
            return;
        }

        LOG.debug("Stopped BLE discovery");
    }

    /**
     * Starts a regular Bluetooth scan
     */
    @RequiresPermission("android.permission.BLUETOOTH_SCAN")
    private void startBTDiscovery() {
        LOG.info("Starting BT discovery");
        try {
            // LineageOS quirk, can't stop scan properly,
            // if scan has been started by something else
            stopBTDiscovery();
        } catch (final Exception ignored) {
        }
        handler.removeMessages(0, stopRunnable);
        handler.sendMessageDelayed(getPostMessage(stopRunnable), SCAN_DURATION);

        if (adapter.startDiscovery()) {
            LOG.debug("Discovery started successfully");
        } else {
            LOG.error("Discovery starting failed");
        }
    }

    @RequiresPermission("android.permission.BLUETOOTH_SCAN")
    private void stopBTDiscovery() {
        if (adapter == null) return;
        adapter.cancelDiscovery();
        LOG.info("Stopped BT discovery");
    }

    private void bluetoothStateChanged(final int newState) {
        if (newState == BluetoothAdapter.STATE_ON) {
            this.adapter = BluetoothAdapter.getDefaultAdapter();
            startButton.setEnabled(true);
        } else {
            this.adapter = null;
            startButton.setEnabled(false);
            bluetoothProgress.setVisibility(View.GONE);
        }
    }

    private boolean checkBluetoothAvailable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                LOG.warn("No BLUETOOTH_SCAN permission");
                this.adapter = null;
                return false;
            }
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                LOG.warn("No BLUETOOTH_CONNECT permission");
                this.adapter = null;
                return false;
            }
        }

        final BluetoothManager bluetoothService = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (bluetoothService == null) {
            LOG.warn("No bluetooth service available");
            this.adapter = null;
            return false;
        }

        final BluetoothAdapter adapter = bluetoothService.getAdapter();
        if (adapter == null) {
            LOG.warn("No bluetooth adapter available");
            this.adapter = null;
            return false;
        }

        if (!adapter.isEnabled()) {
            LOG.warn("Bluetooth not enabled");
            final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            this.adapter = null;
            return false;
        }

        this.adapter = adapter;
        return true;
    }

    @RequiresPermission("android.permission.BLUETOOTH_SCAN")
    private boolean ensureBluetoothReady() {
        final boolean available = checkBluetoothAvailable();
        startButton.setEnabled(available);

        if (available) {
            adapter.cancelDiscovery();
            // must not return the result of cancelDiscovery()
            // appears to return false when currently not scanning
            return true;
        }
        return false;
    }

    private static ScanSettings getScanSettings() {
        final ScanSettings.Builder builder = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);

        builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
        builder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE);
        builder.setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED);
        }

        return builder.build();
    }

    private List<ScanFilter> getScanFilters() {
        final List<ScanFilter> allFilters = new ArrayList<>();
        for (DeviceType deviceType : DeviceType.values()) {
            allFilters.addAll(deviceType.getDeviceCoordinator().createBLEScanFilters());
        }
        return allFilters;
    }

    private Message getPostMessage(final Runnable runnable) {
        final Message message = Message.obtain(handler, runnable);
        message.obj = runnable;
        return message;
    }

    private void showWarnDialog(@StringRes final int message) {
        new MaterialAlertDialogBuilder(getContext())
                .setMessage(message)
                .setPositiveButton(R.string.ok, (dialog, whichButton) -> {
                })
                .show();
    }

    private void checkAndRequestLocationPermission() {
        /* This is more or less a copy of what's in ControlCenterv2, but
        we do this in case the permissions weren't requested since there
        is no way we can scan without this stuff */
        List<String> wantedPermissions = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            LOG.error("No permission to access coarse location!");
            wantedPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            LOG.error("No permission to access fine location!");
            wantedPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        // if we need location permissions, request both together to avoid a bunch of dialogs
        if (wantedPermissions.size() > 0) {
            toast(DiscoveryActivityV2.this, getString(R.string.error_no_location_access), Toast.LENGTH_SHORT, GB.ERROR);
            ActivityCompat.requestPermissions(this, wantedPermissions.toArray(new String[0]), 0);
            wantedPermissions.clear();
        }
        // Now we have to request background location separately!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                LOG.error("No permission to access background location!");
                toast(DiscoveryActivityV2.this, getString(R.string.error_no_location_access), Toast.LENGTH_SHORT, GB.ERROR);
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 0);
            }
        }
        // Now, we can request Bluetooth permissions....
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                LOG.error("No permission to access Bluetooth scanning!");
                toast(DiscoveryActivityV2.this, getString(R.string.error_no_bluetooth_scan), Toast.LENGTH_SHORT, GB.ERROR);
                wantedPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                LOG.error("No permission to access Bluetooth connection!");
                toast(DiscoveryActivityV2.this, getString(R.string.error_no_bluetooth_connect), Toast.LENGTH_SHORT, GB.ERROR);
                wantedPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }
        if (wantedPermissions.size() > 0) {
            GB.toast(this, getString(R.string.permission_granting_mandatory), Toast.LENGTH_LONG, GB.ERROR);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this, wantedPermissions.toArray(new String[0]), 0);
            } else {
                ActivityResultLauncher<String[]> requestMultiplePermissionsLauncher =
                        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
                            if (!isGranted.containsValue(false)) {
                                // Permission is granted. Continue the action or workflow in your app.
                                // should we do startDiscovery here??
                            } else {
                                // Explain to the user that the feature is unavailable because the feature requires a permission that the user has denied.
                                GB.toast(this, getString(R.string.permission_granting_mandatory), Toast.LENGTH_LONG, GB.ERROR);
                            }
                        });
                requestMultiplePermissionsLauncher.launch(wantedPermissions.toArray(new String[0]));
            }
        }

        LocationManager locationManager = (LocationManager) DiscoveryActivityV2.this.getSystemService(Context.LOCATION_SERVICE);
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                // Do nothing
                LOG.debug("Some location provider is enabled, assuming location is enabled");
            } else {
                toast(DiscoveryActivityV2.this, getString(R.string.require_location_provider), Toast.LENGTH_LONG, GB.ERROR);
                DiscoveryActivityV2.this.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                // We can't be sure location was enabled, cancel scan start and wait for new user action
                toast(DiscoveryActivityV2.this, getString(R.string.error_location_enabled_mandatory), Toast.LENGTH_SHORT, GB.ERROR);
                return;
            }
        } catch (final Exception ex) {
            LOG.error("Exception when checking location status", ex);
        }
        LOG.info("Permissions seems to be fine for scanning");
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
        final GBDeviceCandidate deviceCandidate = deviceCandidates.get(position);
        if (deviceCandidate == null) {
            LOG.error("Device candidate clicked, but item not found");
            return;
        }

        preparePair(deviceCandidate);
    }

    private void preparePair(final GBDeviceCandidate deviceCandidate) {
        DeviceType deviceType = DeviceHelper.getInstance().resolveDeviceType(deviceCandidate);

        if (!deviceType.isSupported()) {
            LOG.warn("Unsupported device candidate {}", deviceCandidate);
            copyDetailsToClipboard(deviceCandidate);
            return;
        }

        stopDiscovery();

        final DeviceCoordinator coordinator = deviceType.getDeviceCoordinator();
        LOG.info("Using device candidate {} with coordinator {}", deviceCandidate, coordinator.getClass());

        if (coordinator.suggestUnbindBeforePair() && deviceCandidate.isBonded()) {
            new MaterialAlertDialogBuilder(getContext())
                    .setTitle(R.string.unbind_before_pair_title)
                    .setMessage(R.string.unbind_before_pair_message)
                    .setIcon(R.drawable.ic_warning_gray)
                    .setPositiveButton(R.string.ok, (dialog, whichButton) -> {
                        checkAuthKeyAndPair(deviceCandidate, coordinator);
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {
            checkAuthKeyAndPair(deviceCandidate, coordinator);
        }
    }

    private void checkAuthKeyAndPair(final GBDeviceCandidate deviceCandidate, final DeviceCoordinator coordinator) {
        if (coordinator.getBondingStyle() == DeviceCoordinator.BONDING_STYLE_REQUIRE_KEY) {
            final Intent authIntent = AuthKeyActivity.Companion.newIntent(this, deviceCandidate);
            authKeyLauncher.launch(authIntent);
        } else {
            startPair(deviceCandidate, coordinator);
        }
    }

    private void startPair(final GBDeviceCandidate deviceCandidate, final DeviceCoordinator coordinator) {
        final Class<? extends Activity> pairingActivity = coordinator.getPairingActivity();
        if (pairingActivity != null) {
            final Intent intent = new Intent(this, pairingActivity);
            intent.putExtra(DeviceCoordinator.EXTRA_DEVICE_CANDIDATE, deviceCandidate);
            intent.putParcelableArrayListExtra(DeviceCoordinator.EXTRA_DEVICE_ALL_CANDIDATES, deviceCandidates);
            startActivityForResult(intent, CHILD_RESULT);
        } else {
            if (coordinator.getBondingStyle() == DeviceCoordinator.BONDING_STYLE_NONE ||
                    coordinator.getBondingStyle() == DeviceCoordinator.BONDING_STYLE_LAZY) {
                LOG.info("No bonding needed, according to coordinator, so connecting right away");
                BondingUtil.connectThenComplete(this, deviceCandidate);
                return;
            }

            try {
                this.deviceTarget = deviceCandidate;
                BondingUtil.initiateCorrectBonding(this, deviceCandidate, coordinator);
            } catch (final Exception e) {
                LOG.error("Error pairing device {}", deviceCandidate.getMacAddress(), e);
            }
        }
    }

    private void copyDetailsToClipboard(final GBDeviceCandidate deviceCandidate) {
        final List<String> deviceDetails = new ArrayList<>();
        deviceDetails.add(deviceCandidate.getName());
        deviceDetails.add(deviceCandidate.getMacAddress());
        try {
            for (final ParcelUuid uuid : deviceCandidate.getServiceUuids()) {
                deviceDetails.add(uuid.getUuid().toString());
            }
        } catch (final Exception e) {
            LOG.error("Error collecting device uuids", e);
        }
        final String clipboardData = TextUtils.join(", ", deviceDetails);
        final ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        final ClipData clip = ClipData.newPlainText(deviceCandidate.getName(), clipboardData);
        clipboard.setPrimaryClip(clip);
        toast(this, "Device details copied to clipboard", Toast.LENGTH_SHORT, GB.INFO);
    }

    @Override
    public boolean onItemLongClick(final AdapterView<?> adapterView, final View view, final int position, final long id) {
        stopDiscovery();

        final GBDeviceCandidate deviceCandidate = deviceCandidates.get(position);
        if (deviceCandidate == null) {
            LOG.error("Device candidate long clicked, but item not found");
            return true;
        }

        final List<RunnableListIconItem> longClickItems = new ArrayList<>(3);

        final DeviceType deviceType = DeviceHelper.getInstance().resolveDeviceType(deviceCandidate);

        final GBDevice existingDevice = GBApplication.app().getDeviceManager()
                .getDeviceByAddress(deviceCandidate.getMacAddress());

        if (existingDevice == null) {
            longClickItems.add(new RunnableListIconItem(
                    getString(R.string.add_test_device),
                    R.drawable.ic_warning,
                    () -> showUnsupportedDeviceDialog(deviceCandidate)
            ));
        }

        final DeviceCoordinator coordinator = deviceType.getDeviceCoordinator();
        final GBDevice device = DeviceHelper.getInstance().toSupportedDevice(deviceCandidate);

        if (coordinator.getSupportedDeviceSpecificAuthenticationSettings().length > 0) {
            longClickItems.add(new RunnableListIconItem(
                    getString(R.string.auth_settings),
                    R.drawable.ic_vpn_key,
                    () -> {
                        final Intent startIntent = new Intent(DiscoveryActivityV2.this, DeviceSettingsActivity.class);
                        startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                        startIntent.putExtra(DeviceSettingsActivity.MENU_ENTRY_POINT, DeviceSettingsActivity.MENU_ENTRY_POINTS.AUTH_SETTINGS);
                        startActivity(startIntent);
                    }
            ));
        }

        if (coordinator.getDeviceSpecificSettings(device) != null) {
            longClickItems.add(new RunnableListIconItem(
                    getString(R.string.pref_header_device_spec_settings),
                    R.drawable.ic_settings,
                    () -> {
                        final Intent startIntent = new Intent(DiscoveryActivityV2.this, DeviceSettingsActivity.class);
                        startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                        startIntent.putExtra(DeviceSettingsActivity.MENU_ENTRY_POINT, DeviceSettingsActivity.MENU_ENTRY_POINTS.DEVICE_SETTINGS);
                        startActivity(startIntent);
                    }
            ));
        }

        if (longClickItems.isEmpty()) {
            LOG.error("Device candidate long clicked, but there are no options");
            return true;
        }

        final SimpleIconListAdapter adapter = new SimpleIconListAdapter(this, longClickItems);

        new MaterialAlertDialogBuilder(this)
                .setAdapter(adapter, (dialog, i1) -> longClickItems.get(i1).getAction().run())
                .setTitle(deviceCandidate.getName())
                .setNegativeButton(android.R.string.cancel, (dialogInterface, i1) -> {
                })
                .create()
                .show();

        return true;
    }

    private void showUnsupportedDeviceDialog(final GBDeviceCandidate deviceCandidate) {
        LOG.info("Unsupported device candidate selected: {}", deviceCandidate);

        final Map<String, Pair<Long, Integer>> allDevices = DebugActivity.getAllSupportedDevices(getApplicationContext());

        final LinearLayout linearLayout = new LinearLayout(DiscoveryActivityV2.this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        final ArrayList<SpinnerWithIconItem> deviceListArray = new ArrayList<>();
        for (Map.Entry<String, Pair<Long, Integer>> item : allDevices.entrySet()) {
            deviceListArray.add(new SpinnerWithIconItem(item.getKey(), item.getValue().first, item.getValue().second));
        }
        final SpinnerWithIconAdapter deviceListAdapter = new SpinnerWithIconAdapter(
                DiscoveryActivityV2.this,
                R.layout.spinner_with_image_layout,
                R.id.spinner_item_text,
                deviceListArray
        );

        final Spinner deviceListSpinner = new Spinner(DiscoveryActivityV2.this);
        deviceListSpinner.setAdapter(deviceListAdapter);
        deviceListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent, final View view, final int pos, final long id) {
                final SpinnerWithIconItem selectedItem = (SpinnerWithIconItem) parent.getItemAtPosition(pos);
                selectedUnsupportedDeviceKey = selectedItem.getId();
            }

            @Override
            public void onNothingSelected(final AdapterView<?> arg0) {
            }
        });
        linearLayout.addView(deviceListSpinner);

        final LinearLayout macLayout = new LinearLayout(DiscoveryActivityV2.this);
        macLayout.setOrientation(LinearLayout.HORIZONTAL);
        macLayout.setPadding(20, 0, 20, 0);
        linearLayout.addView(macLayout);

        new MaterialAlertDialogBuilder(DiscoveryActivityV2.this)
                .setCancelable(true)
                .setTitle(R.string.add_test_device)
                .setView(linearLayout)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    if (selectedUnsupportedDeviceKey != DebugActivity.SELECT_DEVICE) {
                        final DeviceType deviceType = DeviceType.values()[(int) selectedUnsupportedDeviceKey];
                        deviceCandidate.setForcedType(deviceType);
                        preparePair(deviceCandidate);
                    }
                })
                .setNegativeButton(R.string.Cancel, (dialog, which) -> {
                })
                .show();
    }

    @Override
    public void onBondingComplete(final boolean success) {
        finish();
    }

    @Override
    public GBDeviceCandidate getCurrentTarget() {
        return this.deviceTarget;
    }

    @Override
    public boolean getAttemptToConnect() {
        return true;
    }

    @Override
    public void registerBroadcastReceivers() {
        final IntentFilter bluetoothIntents = new IntentFilter();
        bluetoothIntents.addAction(BluetoothDevice.ACTION_FOUND);
        bluetoothIntents.addAction(BluetoothDevice.ACTION_UUID);
        bluetoothIntents.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        bluetoothIntents.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        bluetoothIntents.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        ContextCompat.registerReceiver(this, bluetoothReceiver, bluetoothIntents, ContextCompat.RECEIVER_EXPORTED);
    }

    @Override
    public void unregisterBroadcastReceivers() {
        AndroidUtils.safeUnregisterBroadcastReceiver(this, bluetoothReceiver);
    }

    @Override
    public Context getContext() {
        return this;
    }

    private void loadSettings() {
        final Prefs prefs = GBApplication.getPrefs();
        deviceFoundProcessor.setDiscoverUnsupported(prefs.getBoolean("discover_unsupported_devices", false));
    }

    @Override
    public void onDeviceChanged() {
        refreshDeviceList(true);
    }

    @Override
    public void onCreateMenu(@NonNull final Menu menu, @NonNull final MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_discovery, menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull final MenuItem menuItem) {
        final int itemId = menuItem.getItemId();
        if (itemId == R.id.prefs_discovery_pairing) {
            final Intent intent = new Intent(DiscoveryActivityV2.this, DiscoveryPairingPreferenceActivity.class);
            startActivity(intent);
            return true;
        }
        return false;
    }

    private final class BluetoothReceiver extends BroadcastReceiver {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onReceive(final Context context, final Intent intent) {
            switch (Objects.requireNonNull(intent.getAction())) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED: {
                    LOG.debug("ACTION_DISCOVERY_STARTED");
                    break;
                }
                case BluetoothAdapter.ACTION_STATE_CHANGED: {
                    LOG.debug("ACTION_STATE_CHANGED ");
                    bluetoothStateChanged(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF));
                    break;
                }
                case BluetoothDevice.ACTION_FOUND: {
                    final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device == null) {
                        LOG.warn("ACTION_FOUND with null device");
                        return;
                    }
                    LOG.debug("ACTION_FOUND {}", device.getAddress());
                    final short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, GBDevice.RSSI_UNKNOWN);
                    deviceFoundProcessor.scheduleProcessing(new GBScanEvent(device, rssi, null));
                    break;
                }
                case BluetoothDevice.ACTION_UUID: {
                    final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device == null) {
                        LOG.warn("ACTION_UUID with null device");
                        return;
                    }
                    LOG.debug("ACTION_UUID {}", device.getAddress());
                    final short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, GBDevice.RSSI_UNKNOWN);
                    final Parcelable[] uuids = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                    final ParcelUuid[] uuids2 = AndroidUtils.toParcelUuids(uuids);
                    deviceFoundProcessor.scheduleProcessing(new GBScanEvent(device, rssi, uuids2));
                    break;
                }
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED: {
                    LOG.debug("ACTION_BOND_STATE_CHANGED");
                    final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null) {
                        final String macAddress = device.getAddress();
                        final String targetMacAddress = getMacAddress();
                        final int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                        final int prevBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                        LOG.info("Bond state changed for {} from {} to {}, target address {}",
                                macAddress,
                                BleNamesResolver.getBondStateString(prevBondState),
                                BleNamesResolver.getBondStateString(bondState),
                                targetMacAddress
                        );

                        if (targetMacAddress == null || !targetMacAddress.equalsIgnoreCase(macAddress)) {
                            LOG.debug("ignore due to MAC address: got {} but expected {}", macAddress, targetMacAddress);
                        } else if (bondState != BluetoothDevice.BOND_BONDED) {
                            LOG.debug("ignore due bonding state: {}", BleNamesResolver.getBondStateString(bondState));
                        } else {
                            BondingUtil.handleDeviceBonded((BondingInterface) context, getCandidateFromMAC(device));
                        }
                    }
                    break;
                }
            }
        }
    }

    private final class BleScanCallback extends ScanCallback {
        @Override
        public void onScanResult(final int callbackType, final ScanResult result) {
            super.onScanResult(callbackType, result);
            try {
                final ScanRecord scanRecord = result.getScanRecord();
                if (scanRecord == null) {
                    LOG.warn("Got a null scan record");
                    return;
                }
                ParcelUuid[] uuids = null;
                final List<ParcelUuid> serviceUuids = scanRecord.getServiceUuids();
                if (serviceUuids != null) {
                    uuids = serviceUuids.toArray(new ParcelUuid[0]);
                }
                final BluetoothDevice device = result.getDevice();
                final short rssi = (short) result.getRssi();
                LOG.debug(
                        "BLE scan record for {} ({}): len={}, rssi={}, uuids={}",
                        device.getAddress(),
                        scanRecord.getDeviceName(),
                        scanRecord.getBytes().length,
                        rssi,
                        uuids != null ? uuids.length : "null"
                );
                deviceFoundProcessor.scheduleProcessing(new GBScanEvent(device, rssi, uuids));
            } catch (final Exception e) {
                LOG.warn("Error handling BLE scan result", e);
            }
        }
    }
}
