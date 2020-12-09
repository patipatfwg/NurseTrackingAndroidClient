package com.phyathai.NurseTrackingClient.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.phyathai.NurseTrackingClient.adapters.BleDevicesAdapter
import com.phyathai.NurseTrackingClient.models.*
import com.phyathai.NurseTrackingClient.utils.MQTTUtils.MQQTTConstants.subTopic
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.nio.file.Files.size
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MQTTUtils private constructor(private val context: Context) {

    private val DEFAULT_VALUE = "000"
    private val CONFIG_FLAG_INTERVAL_TAG = 4
    private val gson = Gson()
    private var gson2 = GsonBuilder().setPrettyPrinting().create()
    private var mqttAndroidClient: MqttAndroidClient? = null
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("phyathai", 0)

    object MQQTTConstants {
        const val mqttServerHome: String = "tcp://192.168.1.49:1883"
        const val mqttServerFWG: String = "tcp://10.32.11.18:1883"
        const val mqttServerHTTP: String = "tcp://test.mosquitto.org"

        const val mqttServerUri = mqttServerHTTP
        const val publishTopic: String = "Phyathai/Ward1/Client"
        const val subTopic: String = "Phyathai/Ward1/Server"
        const val clientId: String = ""
    }

    init {
        mqttAndroidClient = MqttAndroidClient(context, MQQTTConstants.mqttServerUri,MQQTTConstants.clientId)
        mqttAndroidClient?.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String) {
                Log.d("----MQTT", "connected!")
                subscribeServer()
                if (reconnect) {
                    showToast("reconnected")
                } else {
                    showToast("connected")
                }
            }

            override fun connectionLost(cause: Throwable) {

            }

            @Throws(Exception::class)
            override fun messageArrived(topic: String, message: MqttMessage) {
                UpdateiTAGListVersion(message)
            }

            override fun deliveryComplete(token: IMqttDeliveryToken) {
                Log.d("----MQTT", "data sent!")
                showToast("data sent")
            }
        })

        val mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.isCleanSession = true
//        mqttConnectOptions.userName = ""
//        mqttConnectOptions.password = ""

        mqttAndroidClient?.connect(mqttConnectOptions, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                val disconnectedBufferOptions = DisconnectedBufferOptions()
                disconnectedBufferOptions.isBufferEnabled = true
                disconnectedBufferOptions.bufferSize = 100
                disconnectedBufferOptions.isPersistBuffer = false
                disconnectedBufferOptions.isDeleteOldestMessages = false
                mqttAndroidClient?.setBufferOpts(disconnectedBufferOptions)
            }

            override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                showToast("connection failure")
                Log.e("----MQTT", "connect failed")

                //
                reconnect()
                //

            }
        })
    }

    companion object {
        private var instance: MQTTUtils? = null
        @Synchronized
        fun getInstance(context: Context): MQTTUtils {
            if (instance == null) {
                instance = MQTTUtils(context)
            }
            return instance as MQTTUtils
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun disconnect() {
        mqttAndroidClient?.disconnect()
    }

    fun reconnect(){
        val mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.isCleanSession = true
        mqttAndroidClient = MqttAndroidClient(context, MQQTTConstants.mqttServerUri,MQQTTConstants.clientId)
        mqttAndroidClient?.connect(mqttConnectOptions, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                val disconnectedBufferOptions = DisconnectedBufferOptions()
                disconnectedBufferOptions.isBufferEnabled = true
                disconnectedBufferOptions.bufferSize = 100
                disconnectedBufferOptions.isPersistBuffer = false
                disconnectedBufferOptions.isDeleteOldestMessages = false
                mqttAndroidClient?.setBufferOpts(disconnectedBufferOptions)
            }

            override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                showToast("connection failure2")
                Log.e("----MQTT", "connect failed2")

                //

                //

            }
        })
    }

    fun publishMessage(message: String) {
        try {
            val publishTopic = MQQTTConstants.publishTopic
            val mqttMessage = MqttMessage()
            mqttMessage.payload = message.toByteArray()
            mqttAndroidClient?.publish(publishTopic, mqttMessage)
            if (mqttAndroidClient?.isConnected == true) {
                //
            } else {
                showToast("mqtt not established")
            }
        } catch (e: Exception) {
            Log.e("----MQTT", "not estd.")
            e.printStackTrace()
        }
    }

    fun subscribeServer() {
        val topic = subTopic
        val qos = 0 // Mention your qos value
        try {
            mqttAndroidClient?.subscribe(topic, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.d("----MQTT","subscribeServer $topic")
                }
                override fun onFailure(
                    asyncActionToken: IMqttToken,
                    exception: Throwable
                ) {
                    Log.d("----MQTT","What $topic")
                }
            })
        } catch (e: MqttException) {
            // Give your subscription failure callback here
        }
    }

    fun BLEDevice(bleDeviceAdapter: BleDevicesAdapter?){
//        Log.d("mqtt Blue",bleDeviceAdapter!!.bleDevices.toString())
        var editor = sharedPreferences.edit()
        var flag_interval_TAG = sharedPreferences.getInt("flag_interval_TAG", 0)
        val FoundiTAG = bleDeviceAdapter!!.bleDevices.size

            val deviceId = sharedPreferences.getString("deviceId", DEFAULT_VALUE)
            val datetime: String =
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            val iTAGListVersionSetting =
                sharedPreferences.getString("iTAGListVersionSetting", DEFAULT_VALUE)
            val androidbox = Androidbox(deviceId, datetime)
            val itag_list: ArrayList<iTAGList> = ArrayList()
            for (BleDevice in bleDeviceAdapter!!.bleDevices) {
                itag_list?.add(
                    iTAGList(
                        BleDevice.address.toString(),
//                        BleDevice.name.toString(),
                        null,
                        null,
                        BleDevice.rssi?.toInt()
                    )
                )
            }
        Log.d("mqtt device",deviceId)
            editor.remove("flag_interval_TAG").commit()
            val itag = iTAG(iTAGListVersionSetting, itag_list)
            val SendPublish = SendPublish(androidbox, itag)
            var JSONSendPublish = gson.toJson(SendPublish)

        if( FoundiTAG==0 ){
            if(flag_interval_TAG!=CONFIG_FLAG_INTERVAL_TAG) {
                val flag_interval_TAG = flag_interval_TAG + 1
                editor.putInt("flag_interval_TAG",flag_interval_TAG).apply()
                editor.commit()
            }else if(flag_interval_TAG==CONFIG_FLAG_INTERVAL_TAG){
                val flag_interval_TAG = 0
                editor.putInt("flag_interval_TAG",flag_interval_TAG).apply()
                editor.commit()
                publishMessage(JSONSendPublish)
            }
        }
        else if( FoundiTAG > 0 ) {
            val flag_interval_TAG = 0
            editor.putInt("flag_interval_TAG",flag_interval_TAG).apply()
            editor.commit()
            publishMessage(JSONSendPublish)
        }

    }

    fun UpdateiTAGListVersion(message: MqttMessage) {
        val res_server = gson.fromJson(message.toString(), SendPublish::class.java)
        val DeviceIdServer = res_server?.androidbox?.device_id
        val DeviceIdClient = sharedPreferences.getString("deviceId",DEFAULT_VALUE)
        if( DeviceIdServer == DeviceIdClient ) {
            var iTAGListVersionServer = res_server?.itag?.version
            val iTAGListVersionClient =
                sharedPreferences.getString("iTAGListVersionSetting", DEFAULT_VALUE)
            if (iTAGListVersionClient != iTAGListVersionServer) {
                val editor = sharedPreferences.edit()

                //
                editor.remove("iTAGListVersionSetting")
                editor.commit()
                editor.putString("iTAGListVersionSetting", iTAGListVersionServer).apply()
                editor.commit()

                //
                editor.remove("iTAGListSetting")
                editor.commit()

                val iTAGListServer = message.toString()
                editor.putString("iTAGListServerSetting", iTAGListServer).apply()
                editor.commit()
            }
        }
    }

    fun FilterDeviceModel(ENV_FILTER: Boolean,bleDevice: BleDevice, bleDeviceAdapter: BleDevicesAdapter?){
        if ( bleDevice != null ) {
            if (bleDeviceAdapter?.isDeviceAlreadyFound(bleDevice) == false) {
                //
                if( ENV_FILTER == false ){
                    bleDeviceAdapter?.bleDevices?.add(bleDevice)
                }else if( ENV_FILTER == true ){
                    val iTAGListServerSetting =
                        sharedPreferences.getString("iTAGListServerSetting", DEFAULT_VALUE)
                    if (iTAGListServerSetting != DEFAULT_VALUE) {
                        val iTAGListServer = gson.fromJson(
                            iTAGListServerSetting,
                            SendPublish::class.java
                        ).itag.itag_list
                        iTAGListServer?.forEach {
                            val MacAddressServer = it.mac_address
                            val MacAddressClient = bleDevice.address
                            if (MacAddressServer == MacAddressClient) {
                                val distance = it.distance!!
                                val rssi = bleDevice.rssi?.toInt()!!
                                if ((rssi > distance)) {
                                    bleDeviceAdapter?.bleDevices?.add(bleDevice)
                                }
                            }
                        }
                    }
                }
                //
            }
            bleDeviceAdapter?.notifyDataSetChanged()
        }
    }

}

