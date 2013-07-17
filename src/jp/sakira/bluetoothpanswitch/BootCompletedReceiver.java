package jp.sakira.bluetoothpanswitch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Suzuki on 2013/07/05.
 */
public class BootCompletedReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent btIntent) {
    Log.i("btpanswitch", "boot completed");
    final Intent intent = new Intent(context, BluetoothPANSwitcher.class);
    intent.putExtra(BluetoothPANSwitcher.RequestType, BluetoothPANSwitcher.RequestDisconnect);
    context.startService(intent);
  }

}
