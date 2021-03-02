package fr.esme.esme_map

import com.squareup.picasso.Picasso
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.room.Room
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import fr.esme.esme_map.dao.AppDatabase
import fr.esme.esme_map.interfaces.UserClickInterface
import fr.esme.esme_map.model.POI
import fr.esme.esme_map.model.Position
import fr.esme.esme_map.model.User
//import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

//Bluetooth Adaptater
var devices = ArrayList<BluetoothDevice>()
var devicesMap = HashMap<String, BluetoothDevice>()
var mArrayAdapter: ArrayAdapter<String>? = null
val uuid: UUID = UUID.fromString("8989063a-c9af-463a-b3f1-f21d9b2b827b")
var message = "give data"
var response = false
var global_lat = 0.0
var global_long = 0.0
var client : BluetoothDevice? = null

class MainActivity : AppCompatActivity(), OnMapReadyCallback, UserClickInterface {
    //Bluetooth textview
    private var textView: TextView? = null

    private val TAG = MainActivity::class.qualifiedName
    private lateinit var mMap: GoogleMap
    private lateinit var viewModel: MainActivityViewModel
    private var isFriendShow = true

    private val POI_ACTIVITY = 1
    private val USER_ACTIVITY = 2
    private val BLUETOOTH_ACTIVITY = 3
    private val REQUEST_CODE_DISCOVERABLE_BT = 4
    private lateinit var fusedLocationClient : FusedLocationProviderClient



    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        viewModel.getPOIFromViewModel()
        viewModel.getPositionFromViewModel()

        mMap.setOnMapClickListener {

            val intent = Intent(this, CreatePOIActivity::class.java).apply {
                putExtra("LATLNG", it)
            }

            startActivityForResult(intent, POI_ACTIVITY)


        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == POI_ACTIVITY) {
            var t = data?.getStringExtra("poi")
            var poi = Gson().fromJson<POI>(t, POI::class.java)
            viewModel.savePOI(poi)
            showPOI(Gson().fromJson<POI>(t, POI::class.java))
        }
        if (requestCode == BLUETOOTH_ACTIVITY) {
            if (resultCode == RESULT_CANCELED){
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, BLUETOOTH_ACTIVITY)
            }
            else{
                BluetoothServerController(this).start()
                if (!BluetoothAdapter.getDefaultAdapter().isDiscovering) {
                    val intent = Intent(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                        putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)})
                    startActivityForResult(intent, REQUEST_CODE_DISCOVERABLE_BT)
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContentView(R.layout.activity_main)

        mArrayAdapter = ArrayAdapter(this, R.layout.dialog_select_device)
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(mReceiver, filter) // Don't forget to unregister during onDestroy
        this.textView = findViewById(R.id.textView)

        //button
        findViewById<FloatingActionButton>(R.id.showFriendsButton).setOnClickListener {
            manageUserVisibility()
        }

        //Button
        val button = findViewById<FloatingActionButton>(R.id.scanDevices)
        button.setOnClickListener {view ->
            if (BluetoothAdapter.getDefaultAdapter() == null) {
                Snackbar.make(view, "Bluetooth is disabled", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()

            } else {
                BluetoothServerController(this).start()

                devicesMap = HashMap()
                devices = ArrayList()
                mArrayAdapter!!.clear()

                //val editText = findViewById<EditText>(R.id.editText)
                //message = editText.text.toString()
                message = "give data"
                //editText.text.clear()
                for (device in BluetoothAdapter.getDefaultAdapter().bondedDevices) {
                    devicesMap.put(device.address, device)
                    devices.add(device)
                    // Add the name and address to an array adapter to show in a ListView
                    mArrayAdapter!!.add((if (device.name != null) device.name else "Unknown") + "\n" + device.address + "\nPared")
                }

                // Start discovery process
                if (BluetoothAdapter.getDefaultAdapter().startDiscovery()) {
                    val dialog = SelectDeviceDialog()
                    dialog.show(supportFragmentManager, "select_device")
                }
            }
        }
        //start server
        //BluetoothServerController(this).start()



        //MAP
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //BaseData
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "database-name"
        ).build()

        /*//BLUETOOTH
        if (m_bluetoothAdapter == null) {
            Log.d(TAG, "Device don't support Bluetooth")
        }*/
        // Check if Bluetooth  is enabled
        if (BluetoothAdapter.getDefaultAdapter()?.isEnabled == false) {
            // Ask Bt activation
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, BLUETOOTH_ACTIVITY)
        }
        else{
            BluetoothServerController(this).start()
            if (!BluetoothAdapter.getDefaultAdapter().isDiscovering) {
                val intent = Intent(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                    putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)})
                startActivityForResult(intent, REQUEST_CODE_DISCOVERABLE_BT)
            }
        }





        viewModel = MainActivityViewModel(db)

        viewModel.poisLiveData.observe(this, { listPOIs ->
            showPOIs(listPOIs)
        })

        viewModel.myPositionLiveData.observe(this, { position ->
            showMyPosition(position)
        })


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
       ) {

           this.requestPermissions(
               arrayOf<String>(
                   Manifest.permission.ACCESS_FINE_LOCATION,
                   Manifest.permission.ACCESS_COARSE_LOCATION
               ), 1
           )
       }

        val locationRequest = LocationRequest.create()?.apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        var locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations){
                    showMyPosition(Position(location.latitude, location.longitude))
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }
    /*
    // Discover Function
    private fun discoverDevices(){
        if (bluetoothAdapter!!.isDiscovering) {
            // Bluetooth is already in mode discovery mode, we cancel to restart it again
            bluetoothAdapter!!.cancelDiscovery()
        }
        val bool = bluetoothAdapter?.startDiscovery()
        Log.i("", bool.toString())
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(mReceiver, filter)

        unregisterReceiver(mReceiver)
    }*/



    //TODO show POI
    fun showPOIs(POIs: List<POI>) {
        POIs.forEach {
            val poiPos = LatLng(it.position.latitude, it.position.longitude)
            mMap.addMarker(MarkerOptions().position(poiPos).title(it.name))
        }
    }

    fun showPOI(poi: POI) {
        mMap.addMarker(
            MarkerOptions().position(
                LatLng(
                    poi.position.latitude,
                    poi.position.longitude
                )
            ).title(poi.name)
        )
    }

    //TODO show MyPosition
    fun showMyPosition(position: Position) {
        val myPos = LatLng(position.latitude, position.longitude)

        global_lat = position.latitude
        global_long = position.longitude


        val circleOptions = CircleOptions()
        circleOptions.center(myPos)
        circleOptions.radius(80.0)
        circleOptions.strokeColor(Color.WHITE)
        circleOptions.fillColor(Color.BLACK)
        circleOptions.strokeWidth(6f)

        if(this::myPositionCircle.isInitialized) {
            myPositionCircle.remove()
        }
        myPositionCircle =  mMap.addCircle(circleOptions)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPos, 14f))
    }

    fun showFriendPosition(floatlat : Float, floatlong : Float) {
        val friendPos = LatLng(floatlat.toDouble(), floatlong.toDouble())

        val circleOptions = CircleOptions()
        circleOptions.center(friendPos)
        circleOptions.radius(80.0)
        circleOptions.strokeColor(Color.WHITE)
        circleOptions.fillColor(Color.RED)
        circleOptions.strokeWidth(6f)

        myFriendCircle =  mMap.addCircle(circleOptions)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(friendPos, 14f))
    }

    lateinit var myFriendCircle : Circle
    lateinit var myPositionCircle : Circle

    //TODO show Travel

    //TODO show USer
    fun manageUserVisibility() {

        if (isFriendShow) {
            isFriendShow = false
            findViewById<ListView>(R.id.friendsListRecyclerview).visibility = View.INVISIBLE
        } else {
            isFriendShow = true

            var friends = viewModel.getUsers()

            val adapter = FriendsAdapter(this, ArrayList(friends))
            findViewById<ListView>(R.id.friendsListRecyclerview).adapter = adapter


            findViewById<ListView>(R.id.friendsListRecyclerview).visibility = View.VISIBLE
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")

    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
    }

    var myPosition : Location? = null

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")

    }


    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")

    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()

        mMap.clear()
    }

    override fun OnUserClick(user: User) {

        Log.d("ADAPTER", user.username)

        val intent = Intent(this, UserActivity::class.java).apply {
            putExtra("USER", Gson().toJson(user))
        }

        startActivityForResult(intent, USER_ACTIVITY)



    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun appendText(text: String) {
        runOnUiThread {
            Log.i("server","displaying")
            this.textView?.text = this.textView?.text.toString() +"\n" + text
            if (text.toString() == "give data"){
                response = true
                Log.i("server","getting position : "+global_lat+" "+global_long)
                Log.i("server","getting client : "+client)
                var device = client
                if (device != null) {
                    BluetoothClient(device).start()
                }
            }
            else{
                //TODO show friend on map
                val strs = text.toString().split(" ").toTypedArray()
                showFriendPosition(strs.get(0).toFloat(), strs.get(1).toFloat())
            }
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND == action) {
                // Get the BluetoothDevice object from the Intent
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val pairedDevice = devicesMap[device?.address]
                if (pairedDevice == null) {
                    var index = -1
                    for (i in devices.indices) {
                        val tmp = devices[i]
                        if (device != null) {
                            if (tmp.address == device.address) {
                                index = i
                                break
                            }
                        }
                    }

                    if (index > -1) {
                        if (device != null) {
                            if (device.name != null) {
                                mArrayAdapter?.insert((if (device.name != null) device.name else "Unknown") + "\n" + device.address, index)
                            }
                        }
                    } else {
                        if (device != null) {
                            devices.add(device)
                        }
                        // 	Add the name and address to an array adapter to show in a ListView
                        if (device != null) {
                            mArrayAdapter?.add((if (device.name != null) device.name else "Unknown") + "\n" + device.address)
                        }
                    }
                }
            }
        }
    }
}