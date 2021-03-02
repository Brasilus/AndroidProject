package fr.esme.esme_map

import android.bluetooth.BluetoothSocket
import android.util.Log

class BluetoothServer (private val activity: MainActivity, private val socket: BluetoothSocket): Thread() {
    private val inputStream = this.socket.inputStream
    private val outputStream = this.socket.outputStream

    override fun run() {
        val buffer = ByteArray(1024)
        var bytes: Int
        try {
            //val available = inputStream.available()
            //val bytes = ByteArray(available)
            //Log.i("server", "Reading")
            //inputStream.read(bytes, 0, available)
            //val text = String(bytes)
            //Log.i("server", "Message received")
            //Log.i("server", text)
            bytes = inputStream.read(buffer)
            val string = String(buffer, 0, bytes)
            Log.i("server", "Message received")
            Log.i("server", string)



            activity.appendText(string)
        } catch (e: Exception) {
            Log.e("client", "Cannot read data", e)
        } finally {
            inputStream.close()
            outputStream.close()
            socket.close()
        }
    }
}