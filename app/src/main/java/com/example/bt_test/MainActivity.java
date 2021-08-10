package com.example.bt_test;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import static android.R.layout.*;
import android.widget.VerticalSeekBar;

import org.w3c.dom.Text;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;

    BluetoothAdapter bluetoothAdapter;

    ArrayList<String> pairedDeviceArrayList;

    ListView listViewPairedDevice;
    FrameLayout ButPanel;

    private UUID myUUID;
    ArrayAdapter<String> pairedDeviceAdapter;

    ThreadConnectBTdevice myThreadConnectBTdevice;
    ThreadConnected myThreadConnected;


    private byte[] comBuffer = new byte[5];
    private int comSize = 0;
    byte valueA = 0;
    byte valueB = 0;
    boolean barsEnabled = true;

    public VerticalSeekBar barA, barB, barFull;
    public TextView logView;

    boolean useAscii = false; //использовать ascii символы для чисел (для отладки)
    byte minNum = 0;
    byte maxNum = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        barA = (VerticalSeekBar)findViewById(R.id.vertical_Seekbar);
        barB = (VerticalSeekBar)findViewById(R.id.vertical_Seekbar2);
        barFull = (VerticalSeekBar)findViewById(R.id.vertical_SeekbarFull);
        logView = (TextView)findViewById(R.id.logView);

        if (useAscii) { //для отладки
            minNum = '0';
            maxNum = '9';
        }

        listViewPairedDevice = (ListView)findViewById(R.id.pairedlist);

        ButPanel = (FrameLayout) findViewById(R.id.ButPanel);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)){
            Toast.makeText(this, "BLUETOOTH NOT support", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        final String UUID_STRING_WELL_KNOWN_SPP = "00001101-0000-1000-8000-00805F9B34FB";
        myUUID = UUID.fromString(UUID_STRING_WELL_KNOWN_SPP);

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this hardware platform", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

//        String stInfo = bluetoothAdapter.getName() + " " + bluetoothAdapter.getAddress();
//        Toast.makeText(getApplicationContext(), String.format("Это устройство: %s", stInfo), Toast.LENGTH_LONG).show();

    } // END onCreate


    @Override
    protected void onStart() { // Запрос на включение Bluetooth
        super.onStart();

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        setup();
    }

    private void setup() { // Создание списка сопряжённых Bluetooth-устройств
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) { // Если есть сопряжённые устройства

            pairedDeviceArrayList = new ArrayList<>();

            for (BluetoothDevice device : pairedDevices) { // Добавляем сопряжённые устройства - Имя + MAC-адресс
                pairedDeviceArrayList.add(device.getName() + "\n" + device.getAddress());
            }

            pairedDeviceAdapter = new ArrayAdapter<>(this, simple_list_item_1, pairedDeviceArrayList);
            listViewPairedDevice.setAdapter(pairedDeviceAdapter);

            listViewPairedDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() { // Клик по нужному устройству

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    listViewPairedDevice.setVisibility(View.GONE); // После клика скрываем список

                    String  itemValue = (String) listViewPairedDevice.getItemAtPosition(position);
                    String MAC = itemValue.substring(itemValue.length() - 17); // Вычленяем MAC-адрес

                    BluetoothDevice device2 = bluetoothAdapter.getRemoteDevice(MAC);

                    myThreadConnectBTdevice = new ThreadConnectBTdevice(device2);
                    myThreadConnectBTdevice.start();  // Запускаем поток для подключения Bluetooth
                }
            });
        }
    }

    @Override
    protected void onDestroy() { // Закрытие приложения
        super.onDestroy();
        if(myThreadConnectBTdevice!=null) myThreadConnectBTdevice.cancel();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) { // Если разрешили включить Bluetooth, тогда void setup()

            if (resultCode == Activity.RESULT_OK) {
                setup();
            } else { // Если не разрешили, тогда закрываем приложение

                Toast.makeText(this, "BlueTooth не включён", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }


    private class ThreadConnectBTdevice extends Thread { // Поток для коннекта с Bluetooth

        private BluetoothSocket bluetoothSocket = null;

        private ThreadConnectBTdevice(BluetoothDevice device) {

            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID);
            }

            catch (IOException e) {
                e.printStackTrace();
            }
        }


        @Override
        public void run() { // Коннект

            boolean success = false;

            try {
                bluetoothSocket.connect();
                success = true;
            }

            catch (IOException e) {
                e.printStackTrace();

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Нет коннекта, проверьте Bluetooth-устройство с которым хотите соединиться!", Toast.LENGTH_LONG).show();
                        listViewPairedDevice.setVisibility(View.VISIBLE);
                    }
                });

                try {
                    bluetoothSocket.close();
                }

                catch (IOException e1) {

                    e1.printStackTrace();
                }
            }

            if(success) {  // Если законнектились, тогда открываем панель с кнопками и запускаем поток приёма и отправки данных

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        ButPanel.setVisibility(View.VISIBLE); // открываем панель с кнопками
                    }
                });

                myThreadConnected = new ThreadConnected(bluetoothSocket);
                myThreadConnected.start(); // запуск потока приёма и отправки данных
            }
        }


        public void cancel() {

            Toast.makeText(getApplicationContext(), "Close - BluetoothSocket", Toast.LENGTH_LONG).show();

            try {
                bluetoothSocket.close();
            }

            catch (IOException e) {
                e.printStackTrace();
            }
        }

    } // END ThreadConnectBTdevice:



    private class ThreadConnected extends Thread {    // Поток - приём и отправка данных

        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;

        private String sbprint;

        public ThreadConnected(BluetoothSocket socket) {

            InputStream in = null;
            OutputStream out = null;

            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
            }

            catch (IOException e) {
                e.printStackTrace();
            }

            connectedInputStream = in;
            connectedOutputStream = out;

            sbprint = "";
            DisableBars();
            //приветственная команда
            byte[] bytesToSend = "R0E0Q".getBytes();
            write(bytesToSend, 5);

            //читаем на случай мусора
            try {
                int avBytes = connectedInputStream.available();
                if (avBytes > 0) {
                    connectedInputStream.skip(avBytes);
                }
            } catch (IOException e) {

            }

            //это listener для слайдеров, будет получать управление когда мы их двигаем
            SeekBar.OnSeekBarChangeListener barCallback = new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {

                    //в этот метод попадаем когда поменялся прогресс на любом слайдере
                    byte barAProgress = (byte)barA.getProgress();
                    byte barBProgress = (byte)barB.getProgress();

                    //отправляем команду только если значение поменялось
                    if (barsEnabled && (barAProgress != valueA || barBProgress != valueB)) {
                        valueA = barAProgress;
                        valueB = barBProgress;
                        byte[] resultCom = new byte[5];
                        resultCom[0] = 'A';
                        resultCom[1] = barAProgress;
                        resultCom[2] = 'B';
                        resultCom[3] = barBProgress;
                        resultCom[4] = 'Q';
                        //отправляем команду
                        write(resultCom, 5);
                    } else {
//                        sbprint = "skipped" + System.getProperty("line.separator") + sbprint;
//
//                        runOnUiThread(new Runnable() { // Вывод данных
//
//                            @Override
//                            public void run() {
//                            Toast.makeText(MainActivity.this, sbprint, Toast.LENGTH_LONG).show();
////                                logView.setText(sbprint);
//                            }
//                        });
                    }

                }
            };
            barA.setOnSeekBarChangeListener(barCallback);
            barB.setOnSeekBarChangeListener(barCallback);


            //это listener для общего слайдера
            SeekBar.OnSeekBarChangeListener barFullCallback = new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                    int val = barFull.getProgress();
                    valueA = (byte)val;
                    barA.setProgress(val);
                    valueB = (byte)val;
                    barB.setProgress(val);

                    byte[] resultCom = new byte[5];
                    resultCom[0] = 'A';
                    resultCom[1] = valueA;
                    resultCom[2] = 'B';
                    resultCom[3] = valueB;
                    resultCom[4] = 'Q';
                    //отправляем команду
                    write(resultCom, 5);

                }
            };
            barFull.setOnSeekBarChangeListener(barFullCallback);
        }

        private boolean CheckComPos(byte b, int pos) {
            switch (pos) {
                case 0:
                    return b == 'A';
                case 1:
                case 3:
                    return b >= minNum && b <= maxNum;
                case 2:
                    return b == 'B';
                case 4:
                    return b == 'Q';
            }
            //невалидный символ для собираемой команды
            return false;
        }

        //Проверяет валидность команды. Не проверяет границы массива!
        private boolean CheckCom(byte[] buffer) {
            return buffer[0] == 'A' && buffer[1] >= minNum && buffer[1] <= maxNum
                    && buffer[2] == 'B' && buffer[3] >= minNum && buffer[3] <= maxNum
                    && buffer[4] == 'Q';
        }

        //Выполняет команду
        private void ExecuteCom(byte[] com) {
            //формат: AxByQ
            valueA = com[1];
            valueB = com[3];
            if (useAscii) { //для отладки
                valueA = (byte) Character.getNumericValue(com[1]);
                valueB = (byte) Character.getNumericValue(com[3]);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    barA.setProgress((int)valueA);
                    barB.setProgress((int)valueB);
                    EnableBars();
                }
            });
            comSize = 0;
        }

        private void DisableBars() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    barsEnabled = false;
                    barA.setEnabled(false);
                    barB.setEnabled(false);
                    barFull.setEnabled(false);
                }
            });
        }

        private void EnableBars() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    barA.setEnabled(true);
                    barB.setEnabled(true);
                    barFull.setEnabled(true);
                    barsEnabled = true;
                }
            });
        }

        private void read() throws IOException {
            //получаем данные
            byte[] buffer = new byte[512];
            int bytes = connectedInputStream.read(buffer);
            if (bytes == 5 && CheckCom(buffer)) {
                comBuffer = buffer;
                DisableBars();
                ExecuteCom(comBuffer);
            } else {
                //на случай если данные пришли по кускам, будем собирать команду в несколько заходов
                for (int i = 0; i < bytes; i++) {
                    if (buffer[i] == 'A') {
                        comBuffer[comSize] = buffer[i];
                        //получили A, значит начало команды, сбрасываем всё что набрали
                        comSize = 1;
                        //и блочим управление
                        DisableBars();
                    } else if (CheckComPos(buffer[i], comSize)) {
                        //пока команда сходится с форматом, пишем
                        comBuffer[comSize] = buffer[i];
                        //увеличили размер собранной команды
                        comSize ++;
                        //проверяем, не набрали ли команду целиком
                        if (comSize == 5) {
                            //команда из 5 символов собралась
                            if (CheckCom(comBuffer)) {
                                ExecuteCom(comBuffer);
                            } else {
                                //Неверный формат! По идее такого не должно быть.
                                String strIncom = new String(comBuffer, 0, 5);
                                sbprint = "wrong com: " + strIncom + System.getProperty("line.separator") + sbprint;
                                //Toast.makeText(MainActivity.this, "Wrong com! " + strIncom, Toast.LENGTH_LONG).show();
                                EnableBars();
                            }
                            //сбросим сохранённый размер буфера
                            comSize = 0;
                        }
                    } else {
                        //команда не сходится, сбрасываем и возвращаем управление
                        comSize = 0;
                        sbprint = "wrong com, reset buffer" + System.getProperty("line.separator") + sbprint;
                        EnableBars();
                    }
                }
            }
            //дальше для лога
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes; i++) {
                if (buffer[i] >= 0 && buffer[i] <= 10) {
                    sb.append(Integer.toString(buffer[i]));
                } else {
                    sb.append(new String(buffer, i, 1));
                }
            }
            sbprint = "r:" + sb.toString() + System.getProperty("line.separator") + sbprint;

            runOnUiThread(new Runnable() { // Вывод данных

                @Override
                public void run() {
//                            Toast.makeText(MainActivity.this, sbprint, Toast.LENGTH_LONG).show();
                    logView.setText(sbprint);
                }
            });
        }

        @Override
        public void run() { // Приём данных

            while (true) {
                try {
                    read();
                } catch (IOException e) {
                    break;
                }
            }
        }


        public void write(byte[] buffer, int len) {
            try {
                connectedOutputStream.write(buffer, 0, len);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < len; i++) {
                if (buffer[i] >= 0 && buffer[i] <= 10) {
                    sb.append(Integer.toString(buffer[i]));
                } else {
                    sb.append(new String(buffer, i, 1));
                }
            }
            //дальше для лога
            sbprint = "w:" + sb.toString() + System.getProperty("line.separator") + sbprint;
            runOnUiThread(new Runnable() { // Вывод данных

                @Override
                public void run() {
//                            Toast.makeText(MainActivity.this, sbprint, Toast.LENGTH_LONG).show();
                    logView.setText(sbprint);
                }
            });
        }

    }

    public void onClickReconnect(View v) {
        if(myThreadConnectBTdevice!=null) myThreadConnectBTdevice.cancel();
        listViewPairedDevice.setVisibility(View.VISIBLE);
        ButPanel.setVisibility(View.GONE);
    }
} // END
