package com.example.signuplogin

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.io.InputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private val REQUEST_ENABLE_BT = 0
    private lateinit var connectButton: Button
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothDevice: BluetoothDevice
    private lateinit var bluetoothSocket: BluetoothSocket
    private lateinit var vv: ImageView
    private lateinit var tvObject: TextView

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectButton = findViewById(R.id.connectButton)
        vv = findViewById<ImageView>(R.id.vv)
        tvObject = findViewById(R.id.tvObject)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if(bluetoothAdapter?.isEnabled == false){
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        connectButton.setOnClickListener {
            connectToBluetoothDevice()
        }

    }

    class IncomingDataHandler(private val textView: TextView, private val vv: ImageView, packageName: String, private val tvObject: TextView) : Handler() {
        private var d: String = ""
        private var t: String = ""
        private var h: String = ""
        private var v: Int = -1

        override fun handleMessage(msg: Message) {
            val data = msg.obj as String
            if(data.contains("-5")){
                v = 1
            }
            if(data.contains("-4")){
                v = 0
            }
            if (data.contains("Distance")) {
                if(!data.contains("celsius") && !data.contains("Humidity") && !data.contains("-4") && !data.contains("-5"))
                    d = data
            }
            if (data.contains("celsius")) {
                if(!data.contains("Distance") && !data.contains("Humidity") && !data.contains("-4") && !data.contains("-5"))
                    t = data
            }
            if (data.contains("Humidity")) {
                if(!data.contains("celsius") && !data.contains("Distance") && !data.contains("-4") && !data.contains("-5"))
                    h = data
            }

            textView.text = "$d\n$t\n$h\n"
            if(v == 1){
                vv.setImageResource(R.drawable.green_correct)
                tvObject.text = "OBJECT DETECTED"
            }
            if(v == 0){
                vv.setImageResource(R.drawable.red_wrong)
                tvObject.text = "OBJECT NOT DETECTED"
            }
        }
    }

    class ConnectedThread(private val handler: Handler, private val textView: TextView, private val inputStream: InputStream) : Thread() {
        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int

            while (true) {
                try {
                    bytes = inputStream.read(buffer)
                    val receivedData = String(buffer, 0, bytes)

                    // Create a message with the received data
                    val message = handler.obtainMessage()
                    message.obj = receivedData

                    // Send the message to the handler to update the UI
                    handler.sendMessage(message)

                } catch (e: IOException) {
                    e.printStackTrace()
                    break
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToBluetoothDevice() {
        val deviceAddress = "00:22:04:00:C3:4B"
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress)
        bluetoothAdapter.getRemoteDevice(deviceAddress)

        try {
            val receivedDataTextView: TextView = findViewById(R.id.receivedDataTextView)

            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
            bluetoothSocket.connect()

            runOnUiThread {
                Toast.makeText(this, "Connected to device HC-05", Toast.LENGTH_SHORT).show()
            }

            try {
                val inputStream = bluetoothSocket.inputStream
                val handler = IncomingDataHandler(receivedDataTextView,vv,packageName,tvObject)
                val thread = ConnectedThread(handler, receivedDataTextView, inputStream)
                thread.start()

                val buffer = ByteArray(1024)

                val numBytes: Int = inputStream.read(buffer)
                if (numBytes != -1) {
                    val receivedData = String(buffer, 0, numBytes)
                }

            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to connect to Bluetooth device", Toast.LENGTH_SHORT).show()
            }
            return
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to connect to Bluetooth device", Toast.LENGTH_SHORT).show()
        }
    }
}