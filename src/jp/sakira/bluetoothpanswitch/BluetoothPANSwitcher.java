package jp.sakira.bluetoothpanswitch;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Suzuki on 2013/07/04.
 */
public class BluetoothPANSwitcher extends Service {
  public static final String keyBTPANConnected = "BTPANConnected";
  public static final String RequestType = "RequestType";
  public static final int RequestStart = 0;
  public static final int RequestDisconnect = 1;
  public static final int RequestSwitch = 2;
  public static final int RequestStop = 3;

  private static final int NotificationId = 1;

  private static final int BluetoothProfile_PAN = 5;

  private static Method[] methodsOfBluetoothPAN(BluetoothProfile btpan) {
    final Method[] methods = new Method[3];
    final Class[] params = { BluetoothDevice.class };
    try {
      methods[0] = btpan.getClass().getMethod("connect", params);
      methods[1] = btpan.getClass().getMethod("disconnect", params);
      methods[2] = btpan.getClass().getMethod("getConnectionState", params);
    } catch (NoSuchMethodException e) {
      Log.i("btpanswich", "" + e);
      return null;
    }
    return methods;
  }

  private static Object callMethodOfBluetoothPAN(final BluetoothProfile proxy,
                                                 final Method method,
                                                 final BluetoothDevice param) {
    try {
      return method.invoke(proxy, new Object[]{ param });
    } catch (IllegalArgumentException e) {
      Log.i("btpanswitch", "" + e); return null;
    } catch (InvocationTargetException e) {
      Log.i("btpanswitch", "" + e); return null;
    } catch (IllegalAccessException e) {
      Log.i("btpanswitch", "" + e); return null;
    }
  }

  private BluetoothDevice bluetoothDevice() {
    Context context = getApplicationContext();
    SharedPreferences settings =
        getSharedPreferences(BTPANSwitchActivity.PrefName, 0);
    String deviceMac = settings.getString("device", null);

    Set<BluetoothDevice> bDevices = BluetoothAdapter
      .getDefaultAdapter().getBondedDevices();
    Iterator<BluetoothDevice> bDeviceIterator = bDevices.iterator();
    while (bDeviceIterator.hasNext()) {
      BluetoothDevice bD = bDeviceIterator.next();
      if (bD.getAddress().equals(deviceMac))
        return bD;
    }
    return null;
  }

  private void setNotification(final boolean connect) {
    final Context context = getApplicationContext();

    final Intent intent = new Intent(context, BluetoothPANSwitcher.class);
    intent.putExtra(RequestType, RequestSwitch);
    final PendingIntent pintent =
        PendingIntent.getService(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT);
    final Notification noti;
    if (connect)
      noti = new NotificationCompat.Builder(context)
          .setContentTitle("Bluetooth PAN Switch")
          .setContentText("Connected")
          .setSmallIcon(R.drawable.notification_connect_icon)
          .setContentIntent(pintent)
          .build();
    else
      noti = new NotificationCompat.Builder(context)
          .setContentTitle("Bluetooth PAN Switch")
          .setContentText("Disconnected")
          .setSmallIcon(R.drawable.notification_disconnect_icon)
          .setContentIntent(pintent)
          .build();

    noti.flags = Notification.FLAG_NO_CLEAR;
    final NotificationManager nmanager =
        (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
    nmanager.notify(NotificationId, noti);
  }

  private void removeNotification() {
    final NotificationManager nmanager =
        (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
    nmanager.cancel(NotificationId);
  }

  private void showToast(final boolean connect) {
    final String msg = connect ? "BTPAN Connecting..." : "BTPAN Disconnecting...";
    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    super.onStartCommand(intent, flags, startId);

    final SharedPreferences pref = getSharedPreferences(BTPANSwitchActivity.PrefName, 0);
    final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

    if (adapter == null || !adapter.isEnabled()) {
      removeNotification();
    } else if (!pref.getBoolean(BTPANSwitchActivity.keyEnabled, false)) {
      removeNotification();
    } else if (intent != null) {
      final int request = intent.getIntExtra(RequestType, RequestStart);

      if (request == RequestStart) {
        setNotification(pref.getBoolean(keyBTPANConnected, false));
      } else if (request == RequestDisconnect) {
        pref.edit().putBoolean(keyBTPANConnected, false).commit();
        setNotification(false);
      } else if (request == RequestSwitch) {
        final boolean connect = !pref.getBoolean(keyBTPANConnected, false);
        final SharedPreferences.Editor edit = pref.edit();
        edit.putBoolean(keyBTPANConnected, connect);
        edit.commit();
        switchPAN(connect);
        showToast(connect);
        setNotification(connect);
      } else if (request == RequestStop) {
        removeNotification();
      }
    }

    stopService(intent);
    return Service.START_NOT_STICKY;
  }

  private boolean switchPAN(final boolean connect) {
    final Context context = getApplicationContext();
    Log.i("btpanswitch", "onBind started");

    final BluetoothDevice device = bluetoothDevice();
    if (device == null) return false;

    BluetoothAdapter mAdp = BluetoothAdapter.getDefaultAdapter();
    mAdp.getProfileProxy(context, new BluetoothProfile.ServiceListener() {

      @Override
      public void onServiceDisconnected(int profile) {
      }

      @Override
      public void onServiceConnected(int profile,
                                     BluetoothProfile proxy) {
        final Method[] methods = methodsOfBluetoothPAN(proxy);
        if (methods == null) return;
        callMethodOfBluetoothPAN(proxy, methods[connect ? 0 : 1], device);
      }
    }, BluetoothProfile_PAN);
    return true;
  }
}
