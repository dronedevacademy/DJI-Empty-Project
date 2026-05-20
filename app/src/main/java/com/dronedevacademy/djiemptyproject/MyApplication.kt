package com.dronedevacademy.djiemptyproject

import android.app.Application
import android.content.Context
import android.util.Log
import com.cySdkyc.clx.Helper
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent
import dji.v5.manager.SDKManager
import dji.v5.manager.interfaces.SDKManagerCallback

class MyApplication : Application() {

    companion object {
        private const val TAG = "MyApplication"
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Helper.install(this)
    }

    override fun onCreate() {
        super.onCreate()

        SDKManager.getInstance().init(this, object : SDKManagerCallback {
            override fun onRegisterSuccess() {
                Log.i(TAG, "onRegisterSuccess: ")
            }

            override fun onRegisterFailure(error: IDJIError?) {
                Log.i(TAG, "onRegisterFailure: ")
            }

            override fun onProductDisconnect(productId: Int) {
                Log.i(TAG, "onProductDisconnect: ")
            }

            override fun onProductConnect(productId: Int) {
                Log.i(TAG, "onProductConnect: ")
            }

            override fun onProductChanged(productId: Int) {
                Log.i(TAG, "onProductChanged: ")
            }

            override fun onInitProcess(
                event: DJISDKInitEvent?,
                totalProcess: Int
            ) {
                Log.i(TAG, "onInitProcess: ")
                if (event == DJISDKInitEvent.INITIALIZE_COMPLETE){
                    SDKManager.getInstance().registerApp()
                }
            }

            override fun onDatabaseDownloadProgress(current: Long, total: Long) {
                Log.i(TAG, "onDatabaseDownloadProgress: total $total")
            }

        })

    }
}