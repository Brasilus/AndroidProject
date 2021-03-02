package fr.esme.esme_map

import android.bluetooth.BluetoothDevice
import android.util.Log

class BluetoothClient (device: BluetoothDevice): Thread() {
    private val socket = device.createRfcommSocketToServiceRecord(uuid)

    override fun run() {
        Log.i("client", "Connecting")
        this.socket.connect()

        Log.i("client", "Sending")
        val outputStream = this.socket.outputStream
        val inputStream = this.socket.inputStream
        try {
            if(response == true){
                val data = global_lat.toString()+" "+ global_long.toString()
                outputStream.write(data.toByteArray())
            }
            else {
                outputStream.write(message.toByteArray())
            }
            response = false
            outputStream.flush()
            Log.i("client", "Sent")
        } catch(e: Exception) {
            Log.e("client", "Cannot send", e)
        } finally {
            outputStream.close()
            inputStream.close()
            this.socket.close()
        }
    }
}