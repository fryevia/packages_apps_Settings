/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.bluetooth;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import com.android.settings.R;

/**
 * BluetoothPermissionRequest is a receiver to receive Bluetooth connection
 * access request.
 */
public final class BluetoothPermissionRequest extends BroadcastReceiver {

    private static final String TAG = "BluetoothPermissionRequest";
    private static final boolean DEBUG = Utils.V;
    public static final int NOTIFICATION_ID = android.R.drawable.stat_sys_data_bluetooth;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (DEBUG) Log.d(TAG, "onReceive");

        if (action.equals(BluetoothDevice.ACTION_CONNECTION_ACCESS_REQUEST)) {
            // convert broadcast intent into activity intent (same action string)
            BluetoothDevice device =
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int requestType = intent.getIntExtra(BluetoothDevice.EXTRA_ACCESS_REQUEST_TYPE,
                                                 BluetoothDevice.REQUEST_TYPE_PROFILE_CONNECTION);
            String returnPackage = intent.getStringExtra(BluetoothDevice.EXTRA_PACKAGE_NAME);
            String returnClass = intent.getStringExtra(BluetoothDevice.EXTRA_CLASS_NAME);

            Intent connectionAccessIntent = new Intent(action);
            connectionAccessIntent.setClass(context, BluetoothPermissionActivity.class);
            connectionAccessIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            connectionAccessIntent.putExtra(BluetoothDevice.EXTRA_ACCESS_REQUEST_TYPE, requestType);
            connectionAccessIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
            connectionAccessIntent.putExtra(BluetoothDevice.EXTRA_PACKAGE_NAME, returnPackage);
            connectionAccessIntent.putExtra(BluetoothDevice.EXTRA_CLASS_NAME, returnClass);

            String deviceAddress = device != null ? device.getAddress() : null;

            PowerManager powerManager =
                (PowerManager) context.getSystemService(Context.POWER_SERVICE);

            if (powerManager.isScreenOn() &&
                LocalBluetoothPreferences.shouldShowDialogInForeground(context, deviceAddress) ) {
                context.startActivity(connectionAccessIntent);
            } else {
                // Put up a notification that leads to the dialog

                // Create an intent triggered by clicking on the
                // "Clear All Notifications" button

                Intent deleteIntent = new Intent(BluetoothDevice.ACTION_CONNECTION_ACCESS_REPLY);
                deleteIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
                deleteIntent.putExtra(BluetoothDevice.EXTRA_CONNECTION_ACCESS_RESULT,
                        BluetoothDevice.CONNECTION_ACCESS_NO);

                Notification notification = new Notification(android.R.drawable.stat_sys_data_bluetooth,
                    context.getString(R.string.bluetooth_connection_permission_request),
                    System.currentTimeMillis());
                String deviceName = device != null ? device.getName() : null;
                notification.setLatestEventInfo(context,
                    context.getString(R.string.bluetooth_connection_permission_request),
                    context.getString(R.string.bluetooth_connection_notif_message, deviceName),
                    PendingIntent.getActivity(context, 0, connectionAccessIntent, 0));
                notification.flags = Notification.FLAG_AUTO_CANCEL |
                                     Notification.FLAG_ONLY_ALERT_ONCE;
                notification.defaults = Notification.DEFAULT_SOUND;
                notification.deleteIntent = PendingIntent.getBroadcast(context, 0, deleteIntent, 0);

                NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(NOTIFICATION_ID, notification);
            }
        } else if (action.equals(BluetoothDevice.ACTION_CONNECTION_ACCESS_CANCEL)) {
            // Remove the notification
            NotificationManager manager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(NOTIFICATION_ID);
        }
   }
}
