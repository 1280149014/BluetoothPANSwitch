package jp.sakira.bluetoothpanswitch;

import java.util.ArrayList;
import java.util.Iterator;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;

public class BTPANSwitchActivity extends Activity {
	public static final String PrefName = "Default";
  public static final String keyEnabled = "Enabled";

  private CheckBox notificationCheckBox;
	private  Spinner deviceSelector;
	SharedPreferences settings;
	protected ArrayList<BluetoothDevice> bDevices = new ArrayList<BluetoothDevice>();
	private BluetoothAdapter bAdapter;
	private final static int REQUEST_ENABLE_BT = 0;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.empty);

    bAdapter = BluetoothAdapter.getDefaultAdapter();
    if(bAdapter == null){
      AlertDialog alertDialog = new AlertDialog.Builder(this).create();
      alertDialog.setTitle("No Bluetooth adapter found...");
      alertDialog.setMessage("This App will now exit");
      alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
          new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              BTPANSwitchActivity.this.finish();
            }
          });
      alertDialog.setIcon(R.drawable.launcher);
      alertDialog.show();
    }

    final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    if (adapter == null) return;
    if (!adapter.isEnabled()) {
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }else{
      this.setupControls();
    }
  }

  private void setupControls() {
    setContentView(R.layout.main);

    TextView desc = (TextView) findViewById(R.id.description_text);
    CharSequence styledText = Html.fromHtml(getResources().getString(R.string.description_text));
    desc.setText(styledText);

    settings = getApplicationContext().getSharedPreferences(PrefName, 0);

    deviceSelector = (Spinner) findViewById(R.id.deviceSelector);

    int selectedDevice = -1;

    Iterator<BluetoothDevice> devicesSetIterator = BluetoothAdapter.getDefaultAdapter().getBondedDevices().iterator();
    while(devicesSetIterator.hasNext()){
      bDevices.add(devicesSetIterator.next());
      if (bDevices.get(bDevices.size()-1).getAddress().equals(settings.getString("device", "")))
        selectedDevice = bDevices.size()-1;
    }

    BluetoothDeviceAdapter devicesAdapter = new BluetoothDeviceAdapter(getBaseContext(), 0, bDevices);
    deviceSelector.setAdapter(devicesAdapter);
    if(selectedDevice != -1)
      deviceSelector.setSelection(selectedDevice);

    final boolean enabled = settings.getBoolean(keyEnabled, false);
    notificationCheckBox = (CheckBox)findViewById(R.id.notification_enable);
    notificationCheckBox.setChecked(enabled);
    notificationCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean enabled_) {
        final SharedPreferences pref = getSharedPreferences(PrefName, 0);
        final SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(keyEnabled, enabled_);
        editor.commit();

        final Intent intent = new Intent(getApplicationContext(), BluetoothPANSwitcher.class);
        intent.putExtra(BluetoothPANSwitcher.RequestType,
            enabled_ ? BluetoothPANSwitcher.RequestStart : BluetoothPANSwitcher.RequestStop);
        getApplicationContext().startService(intent);
      }
    });

    final Intent intent = new Intent(getApplicationContext(), BluetoothPANSwitcher.class);
    intent.putExtra(BluetoothPANSwitcher.RequestType,
        enabled ? BluetoothPANSwitcher.RequestStart : BluetoothPANSwitcher.RequestStop);
    getApplicationContext().startService(intent);
  }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
    	switch(requestCode){
    	case REQUEST_ENABLE_BT:
    		if (resultCode == Activity.RESULT_OK){
    			this.setupControls();
    		}else{
            	AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            	alertDialog.setTitle("Bluetooth adapter not enabled...");
            	alertDialog.setMessage("This App will now exit");
            	alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            	      public void onClick(DialogInterface dialog, int which) {
            	 
            	    	  BTPANSwitchActivity.this.finish();
            	 
            	    } });
            	alertDialog.setIcon(R.drawable.launcher);
            	alertDialog.show();
    		}
    		break;
    	}
    }

  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    switch(item.getItemId()) {
      case R.id.menu_save:
        SharedPreferences.Editor editor = settings.edit();
        BluetoothDevice bd = (BluetoothDevice) deviceSelector.getSelectedItem();
        editor.putString("device", bd.getAddress());
        editor.commit();

        final Intent intent = new Intent(getApplicationContext(), BluetoothPANSwitcher.class);
        startService(intent);

        BTPANSwitchActivity.this.finish();
        return true;
    }

    return super.onMenuItemSelected(featureId, item);
  }

  class BluetoothDeviceAdapter extends ArrayAdapter<BluetoothDevice>{

    public BluetoothDeviceAdapter(Context context, int textViewResourceId, ArrayList<BluetoothDevice> objects) {
      super(context, textViewResourceId, objects);
    }

    @Override
    public View getDropDownView(int position, View convertView,
                                ViewGroup parent) {
      return getCustomView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      return getCustomView(position, convertView, parent);
    }

    public View getCustomView(int position, View convertView, ViewGroup parent) {
      LayoutInflater inflater=getLayoutInflater();
      View row=inflater.inflate(R.layout.spinnerrow, parent, false);
      TextView label=(TextView) row.findViewById(R.id.textlabel);
      label.setText(bDevices.get(position).getName());

      return row;
    }
  }
}