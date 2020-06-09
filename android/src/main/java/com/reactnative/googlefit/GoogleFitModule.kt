/**
 * Copyright (c) 2017-present, Stanislav Doskalenko - doskalenko.s@gmail.com
 * All rights reserved.
 *
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * Based on Asim Malik android source code, copyright (c) 2015
 */
package com.reactnative.googlefit

import android.content.pm.PackageManager
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.uimanager.IllegalViewOperationException
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener

class GoogleFitModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), LifecycleEventListener {
    private val mReactContext: ReactContext
    private var mGoogleFitManager: GoogleFitManager? = null
    private val GOOGLE_FIT_APP_URI = "com.google.android.apps.fitness"
    override fun getName(): String {
        return REACT_MODULE
    }

    override fun initialize() {
        super.initialize()
        reactApplicationContext.addLifecycleEventListener(this)
    }

    override fun onHostResume() {
        if (mGoogleFitManager != null) {
            mGoogleFitManager!!.resetAuthInProgress()
        }
    }

    override fun onHostPause() {}
    override fun onHostDestroy() {
        // todo disconnect from Google Fit
    }

    @ReactMethod
    fun authorize(options: ReadableMap) {
        val activity = currentActivity
        if (mGoogleFitManager == null) {
            mGoogleFitManager = GoogleFitManager(mReactContext, activity!!)
        }
        if (mGoogleFitManager!!.isAuthorized) {
            return
        }
        mGoogleFitManager!!.authorize()
    }

    @ReactMethod
    fun isAuthorized(promise: Promise) {
        var isAuthorized = false
        if (mGoogleFitManager != null && mGoogleFitManager!!.isAuthorized) {
            isAuthorized = true
        }
        val map = Arguments.createMap()
        map.putBoolean("isAuthorized", isAuthorized)
        promise.resolve(map)
    }

    @ReactMethod
    fun disconnect(promise: Promise) {
        try {
            if (mGoogleFitManager != null) {
                mGoogleFitManager!!.disconnect(currentActivity)
            }
            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject(e)
        }
    }

    @ReactMethod
    fun isAvailable(errorCallback: Callback, successCallback: Callback) { // true if GoogleFit installed
        try {
            successCallback.invoke(isAvailableCheck)
        } catch (e: IllegalViewOperationException) {
            errorCallback.invoke(e.message)
        }
    }

    @ReactMethod
    fun isEnabled(errorCallback: Callback, successCallback: Callback) { // true if permission granted
        try {
            successCallback.invoke(isEnabledCheck)
        } catch (e: IllegalViewOperationException) {
            errorCallback.invoke(e.message)
        }
    }

    @ReactMethod
    fun openFit() {
        val pm = mReactContext.packageManager
        try {
            val launchIntent = pm.getLaunchIntentForPackage(GOOGLE_FIT_APP_URI)
            mReactContext.startActivity(launchIntent)
        } catch (e: Exception) {
            Log.i(REACT_MODULE, e.toString())
        }
    }


    @ReactMethod
    fun saveWorkout(workoutData: WritableMap, promise: Promise) {
        mGoogleFitManager?.saveWorkout(Workout(workoutData), OnSuccessListener {
            result ->
            promise.resolve(result)
        }, OnFailureListener {
            exception ->
            promise.reject(exception)
        })
    }

    private val isAvailableCheck: Boolean
        get() {
            val pm = mReactContext.packageManager
            return try {
                pm.getPackageInfo(GOOGLE_FIT_APP_URI, PackageManager.GET_ACTIVITIES)
                true
            } catch (e: Exception) {
                Log.i(REACT_MODULE, e.toString())
                false
            }
        }

    private val isEnabledCheck: Boolean
         get() {
            if (mGoogleFitManager == null) {
                mGoogleFitManager = GoogleFitManager(mReactContext, currentActivity!!)
            }
            return mGoogleFitManager!!.isAuthorized
        }

    /*
    @ReactMethod
    fun getHydrationSamples(startDate: Double,
                            endDate: Double,
                            errorCallback: Callback,
                            successCallback: Callback) {
        try {
            successCallback.invoke(mGoogleFitManager.hydrationHistory.getHistory(startDate.toLong(), endDate.toLong()))
        } catch (e: IllegalViewOperationException) {
            errorCallback.invoke(e.message)
        }
    }
    */

    companion object {
        private const val REACT_MODULE = "RNGoogleFit"
    }

    init {
        mReactContext = reactContext
    }
}