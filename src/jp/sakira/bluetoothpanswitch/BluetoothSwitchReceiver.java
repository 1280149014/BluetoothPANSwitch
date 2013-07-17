package jp.sakira.bluetoothpanswitch;

//import android.bluetooth.BluetoothPan;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
  import android.util.Log;

public class BluetoothSwitchReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent btIntent) {
    final int bt_state = btIntent.getExtras().getInt(
        android.bluetooth.BluetoothAdapter.EXTRA_STATE);
    if (bt_state == android.bluetooth.BluetoothAdapter.STATE_ON) {
      Log.i("btpanconn","bluetooth on");
      final Intent intent = new Intent(context, BluetoothPANSwitcher.class);
      intent.putExtra(BluetoothPANSwitcher.RequestType, BluetoothPANSwitcher.RequestDisconnect);
      context.startService(intent);
    } else if (bt_state == android.bluetooth.BluetoothAdapter.STATE_OFF) {
      Log.i("btpanconn","bluetooth off");
      final Intent intent = new Intent(context, BluetoothPANSwitcher.class);
      intent.putExtra(BluetoothPANSwitcher.RequestType, BluetoothPANSwitcher.RequestStop);
      context.startService(intent);
    }
	}
}
