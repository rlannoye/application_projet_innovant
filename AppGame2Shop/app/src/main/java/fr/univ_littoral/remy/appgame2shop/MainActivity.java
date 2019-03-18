package fr.univ_littoral.remy.appgame2shop;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.nfc.Tag;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_OBJECT = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final String DEVICE_OBJECT = "device_name";
    public static final int FORMULAIRE_ACTIVITY = 2;
    private final static int REQUEST_ENABLE_BLUETOOTH = 1;
    private Dialog dialog;
    private CommandController commandController;
    private BluetoothDevice connectingDevice;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> discoveredDevicesAdapter;
    private ArrayList<String> chatMessages;
    private ArrayAdapter<String> chatAdapter;

    private TextView status;

    Button btnConnect;
    Button inscription;
    Button buttonA;
    Button buttonB;
    Button buttonRight;
    Button buttonLeft;
    Button buttonUp;
    Button buttonDown;

    EditText nom;
    EditText prenom;
    EditText mail;

    public String nomsaisie;
    public String prenomsaisie;
    public String mailsaisie;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnConnect = findViewById(R.id.btn_connect);
        inscription = findViewById(R.id.inscription);
        buttonA = findViewById(R.id.buttonA);
        buttonB = findViewById(R.id.buttonB);
        buttonRight = findViewById(R.id.buttonRight);
        buttonLeft = findViewById(R.id.buttonLeft);
        buttonUp = findViewById(R.id.buttonUp);
        buttonDown = findViewById(R.id.buttonDown);

        //check device support bluetooth or not
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth est non disponible!", Toast.LENGTH_SHORT).show();
            finish();
        }

        //show bluetooth devices dialog when click connect button_rond
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPrinterPickDialog();
            }
        });
    }

    private Handler handler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case CommandController.STATE_CONNECTED:
                            setStatus("Connecté à: " + connectingDevice.getName());
                            btnConnect.setEnabled(false);
                            setContentView(R.layout.inscription);
                            break;
                        case CommandController.STATE_CONNECTING:
                            setStatus("Connexion en cours...");
                            btnConnect.setEnabled(false);
                            break;
                        case CommandController.STATE_LISTEN:
                        case CommandController.STATE_NONE:
                            setStatus("Non connecté");
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;

                    String writeMessage = new String(writeBuf);
                    chatMessages.add("Me: " + writeMessage);
                    chatAdapter.notifyDataSetChanged();
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;

                    String readMessage = new String(readBuf, 0, msg.arg1);
                    chatMessages.add(connectingDevice.getName() + ":  " + readMessage);
                    chatAdapter.notifyDataSetChanged();
                    break;
                case MESSAGE_DEVICE_OBJECT:
                    connectingDevice = msg.getData().getParcelable(DEVICE_OBJECT);
                    Toast.makeText(getApplicationContext(), "Connecté à " + connectingDevice.getName(),
                            Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString("toast"),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });


    private void showPrinterPickDialog() {
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.layout_bluetooth);
        dialog.setTitle("Bluetooth Devices");

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        checkBTPermissions();

        bluetoothAdapter.startDiscovery();

        //Initializing bluetooth adapters
        ArrayAdapter<String> pairedDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        discoveredDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        //locate listviews and attatch the adapters
        //ListView listView = (ListView) dialog.findViewById(R.id.pairedDeviceList);
        ListView listView2 = (ListView) dialog.findViewById(R.id.discoveredDeviceList);
        //listView.setAdapter(pairedDevicesAdapter);
        listView2.setAdapter(discoveredDevicesAdapter);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(discoveryFinishReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryFinishReceiver, filter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesAdapter.add(device.getName() + "\n" + device.getAddress() );
            }
        } else {
            pairedDevicesAdapter.add(getString(R.string.none_paired));
        }
/*
        //Handling listview item click event
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bluetoothAdapter.cancelDiscovery();
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                connectToDevice(address);
                dialog.dismiss();
            }

        });
        */

        listView2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                bluetoothAdapter.cancelDiscovery();
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                connectToDevice(address);
                dialog.dismiss();
            }
        });

        dialog.findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    private void checkBTPermissions(){
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int permissionCheck = this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            permissionCheck += this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
            if (permissionCheck != 0) {
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
            }
        }
    }

    private void setStatus(String s) {
        status = (TextView) findViewById(R.id.status);
        status.setText(s);
    }

    private void connectToDevice(String deviceAddress) {
        bluetoothAdapter.cancelDiscovery();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        commandController.connect(device);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BLUETOOTH:
                if (resultCode == Activity.RESULT_OK) {
                    commandController = new CommandController(this, handler);
                } else {
                    Toast.makeText(this, "Bluetooth désactivé, vous quittez l'application", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
        } else {
            commandController = new CommandController(this, handler);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (commandController != null) {
            if (commandController.getState() == CommandController.STATE_NONE) {
                commandController.start();
            }
        }
    }

    private final BroadcastReceiver discoveryFinishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    discoveredDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (discoveredDevicesAdapter.getCount() == 0) {
                    discoveredDevicesAdapter.add(getString(R.string.none_found));

                }
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (commandController != null)
            commandController.stop();
//        unregisterReceiver(discoveryFinishReceiver);
    }

    public void inscription(View v){
        nom = findViewById(R.id.saisieNom);
        prenom = findViewById(R.id.saisiePrenom);
        mail = findViewById(R.id.saisieMail);
        nomsaisie=nom.getText().toString();
        prenomsaisie=prenom.getText().toString();
        mailsaisie=mail.getText().toString();
        if(nomsaisie.equals("")||prenomsaisie.equals("")||mailsaisie.equals("")){
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
        }else {
            if(emailAddressValidator(mailsaisie)) {
                commandController.write(mailsaisie);
                commandController.write(prenomsaisie);
                commandController.write(nomsaisie);

                setContentView(R.layout.manette);
            }else{
                Toast.makeText(this, "Veuillez entrer une adresse mail valide", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void boutonA(View v){
        MediaPlayer mp = MediaPlayer.create(this, R.raw.pull);
        mp.start();
        commandController.write("A");
        System.out.println("A");
    }

    public void boutonB(View v){
        MediaPlayer mp = MediaPlayer.create(this, R.raw.pull);
        mp.start();
        commandController.write("B");
        System.out.println("B");
    }

    public void boutonU(View v){
        commandController.write("U");
        System.out.println("U");
    }

    public void boutonD(View v){
        commandController.write("D");
        System.out.println("D");
    }

    public void boutonR(View v){
        commandController.write("R");
        System.out.println("R");
    }

    public void boutonL(View v){
        commandController.write("L");
        System.out.println("L");
    }

    public static boolean emailAddressValidator(String emailId) {
        Pattern pattern = Pattern.compile("\\w+([-+.]\\w+)*" + "\\@"
                + "\\w+([-.]\\w+)*" + "\\." + "\\w+([-.]\\w+)*");

        Matcher matcher = pattern.matcher(emailId);
        if (matcher.matches())
            return true;
        else
            return false;
    }

    public void quitter(View v){
        setContentView(R.layout.quitter);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                System.exit(0);
            }
        }, 2000);

    }

}
