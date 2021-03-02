package fr.esme.esme_map

import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class SelectDeviceDialog: DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(this.activity)
        builder.setTitle("Ask data to :")
        builder.setAdapter(mArrayAdapter) { _, which: Int ->
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
            BluetoothClient(devices[which]).start()
        }

        return builder.create()
    }

    /*override fun onCancel(dialog: DialogInterface?) {
        super.onCancel(dialog)

    }*/

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
    }
}