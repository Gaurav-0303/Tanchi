package com.example.signuplogin

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.widget.Button
import android.widget.ImageView
import android.widget.MediaController
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.w3c.dom.Text
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*


class CustomVideoView : VideoView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        super.onDraw(canvas)
    }
}


class MainActivity : AppCompatActivity() {

    private lateinit var connectButton: Button
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothDevice: BluetoothDevice
    private lateinit var bluetoothSocket: BluetoothSocket
    private lateinit var inputStream: InputStream
    private lateinit var receivedDataTextView: TextView
    private lateinit var vv: ImageView
    private lateinit var tvObject: TextView

    private val bluetoothPermissionRequestCode = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectButton = findViewById(R.id.connectButton)
        vv = findViewById<ImageView>(R.id.vv)
        tvObject = findViewById(R.id.tvObject)

        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), bluetoothPermissionRequestCode)

        connectButton.setOnClickListener {
            connectToBluetoothDevice()
        }
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    }

    private fun checkBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PackageManager.PERMISSION_GRANTED == checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            true
        }
    }

    class IncomingDataHandler(private val textView: TextView, private val vv: ImageView, packageName: String, private val tvObject: TextView) : Handler() {
        var d: String = ""
        var t: String = ""
        var h: String = ""
        var v: Int = -1
        var packageName = packageName

        override fun handleMessage(msg: Message) {
            val data = msg.obj as String
            if(data.contains("-5")){
                v = 1
            }
            if(data.contains("-4")){
                v = 0
            }
            if (data.contains("Distance")) {
                d = data
            }
            if (data.contains("celsius")) {
                t = data
            }
            if (data.contains("Humidity")) {
                h = data
            }
            textView.text = "$d\n$t\n$h\n"
            if(v == 1){
                vv.setImageResource(R.drawable.correct)
                tvObject.text = "OBJECT DETECTED"
            }
            if(v == 0){
                vv.setImageResource(R.drawable.wrong)
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

    private fun connectToBluetoothDevice() {
        val deviceAddress = "00:22:04:00:C3:4B"
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress)
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)

        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                val receivedDataTextView: TextView = findViewById(R.id.receivedDataTextView)

                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                bluetoothSocket.connect()

                // You can now read/write data from/to the Bluetooth device using the BluetoothSocket
                Toast.makeText(this, "Connected to device HC-05", Toast.LENGTH_SHORT).show()

                try {
                    val inputStream = bluetoothSocket.inputStream
                    val handler = IncomingDataHandler(receivedDataTextView,vv,packageName,tvObject)
                    val thread = ConnectedThread(handler, receivedDataTextView, inputStream)
                    thread.start()

                    val buffer = ByteArray(1024)
                    var numBytes: Int

                    numBytes = inputStream.read(buffer)
                    if (numBytes != -1) {
                        val receivedData = String(buffer, 0, numBytes)
                        // Process the received data as needed
                        // Update UI or perform other operations
                    }

                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Failed to connect to Bluetooth device", Toast.LENGTH_SHORT).show()
                }
                return
            }

        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to connect to Bluetooth device", Toast.LENGTH_SHORT).show()
        }
    }
}