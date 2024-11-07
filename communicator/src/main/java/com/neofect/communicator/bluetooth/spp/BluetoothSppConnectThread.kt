/*
 * Copyright 2014-2015 Neofect Co., Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.neofect.communicator.bluetooth.spp

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.neofect.communicator.ConnectionType
import java.io.IOException
import java.util.UUID

/**
 * @author neo.kim@neofect.com
 */
internal class BluetoothSppConnectThread(private val connection: BluetoothSppConnection) :
    Thread("BluetoothSppConnectThread") {
    override fun run() {
        // Always cancel discovery because it will slow down a connection.
        // As recommended in http://developer.android.com/guide/topics/connectivity/bluetooth.html#ConnectingAsAClient
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery()

        val device = connection.bluetoothDevice


        // Get a BluetoothSocket for a connection with the given BluetoothDevice.
        val socket: BluetoothSocket? = try {
            // UUID for SPP connection
            val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

            when (connection.connectionType) {
                ConnectionType.BLUETOOTH_SPP -> {
                    device.createRfcommSocketToServiceRecord(sppUuid)
                }

                ConnectionType.BLUETOOTH_SPP_INSECURE -> {
                    device.createInsecureRfcommSocketToServiceRecord(sppUuid)
                }

                else -> {
                    connection.onFailedToConnect(RuntimeException("Unknown bluetooth connection type! '" + connection.connectionType + "'"))
                    null
                }
            }
        } catch (e: IOException) {
            connection.onFailedToConnect(RuntimeException("Failed to create bluetooth socket!", e))
            null
        }
        if (socket == null) {
            return
        }


        // Make a connection to the BluetoothSocket
        try {
            // This is a blocking call and will only return on a successful connection or an exception.
            socket.connect()
        } catch (e: IOException) {
            try {
                socket.close()
            } catch (e1: IOException) {
                Log.e(LOG_TAG, "", e1)
            }
            connection.onFailedToConnect(
                RuntimeException(
                    "Failed to connect to device '" + connection.descriptionWithAddress + "'",
                    e
                )
            )
            return
        }

        connection.onSucceededToConnect(socket)
    }

    companion object {
        private val LOG_TAG: String = BluetoothSppConnectThread::class.java.simpleName
    }
}
