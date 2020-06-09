package com.reactnative.googlefit
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessActivities
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.DataType.TYPE_HEART_RATE_BPM
import com.google.android.gms.fitness.data.Session
import com.google.android.gms.fitness.request.SessionInsertRequest
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import java.lang.Exception
import java.util.concurrent.TimeUnit


class GoogleFitManager(private val mReactContext: ReactContext, private val mActivity: Activity) : ActivityEventListener {
    private  var mFitnessOptions = FitnessOptions.builder()
            .addDataType(TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_WORKOUT_EXERCISE, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_WORKOUT_EXERCISE, FitnessOptions.ACCESS_WRITE)
            .build()
    private lateinit var mSignInAccount: GoogleSignInAccount

    fun resetAuthInProgress() {
        if (!isAuthorized) {
            mAuthInProgress = false
        }
    }


    fun getmSignInAccount(): GoogleSignInAccount? {
        return mSignInAccount
    }
        fun saveWorkout(workout: Workout, onSuccess: OnSuccessListener<in Void>, onFailure: OnFailureListener) {
            val session: Session = Session.Builder()
                    .setName(workout.name)
                    .setDescription(workout.description)
                    .setIdentifier(workout.id)
                    .setActivity(FitnessActivities.STRENGTH_TRAINING)
                    .setStartTime(workout.startTime.toLong(), TimeUnit.MILLISECONDS)
                    .setEndTime(workout.endTime.toLong(), TimeUnit.MILLISECONDS)
                    .build()

            val insertRequest = SessionInsertRequest.Builder()
                    .setSession(session)
                    .setSession(session).build()

            Fitness.getSessionsClient(mReactContext, mSignInAccount)
                    .insertSession(insertRequest)
                    .addOnSuccessListener(onSuccess)
                    .addOnFailureListener(onFailure)
        }
    fun authorize() {
        val mReactContext = mReactContext
        mSignInAccount = GoogleSignIn.getAccountForExtension(mActivity, mFitnessOptions)
        val displayName = mSignInAccount.displayName
        if (!isAuthorized) {

            GoogleSignIn.requestPermissions(mActivity, GOOGLE_FIT_PERMISSIONS_REQUEST_CODE, mSignInAccount, mFitnessOptions)
        }

    }

    fun disconnect(context: Context?) {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()
        val googleSignInClient = GoogleSignIn.getClient(context!!, options)
        val gsa = GoogleSignIn.getAccountForScopes(mReactContext, Scope(Scopes.FITNESS_ACTIVITY_READ))
        Fitness.getConfigClient(mReactContext, gsa).disableFit()
        googleSignInClient.signOut()
    }

    val isAuthorized: Boolean
        get() {
            val account = GoogleSignIn.getAccountForExtension(mActivity, mFitnessOptions)
            return GoogleSignIn.hasPermissions(account, mFitnessOptions)
        }

    private fun sendEvent(reactContext: ReactContext,
                          eventName: String,
                          params: WritableMap?) {
        reactContext
                .getJSModule(RCTDeviceEventEmitter::class.java)
                .emit(eventName, params)
    }

    companion object {
        private const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 33
        private var mAuthInProgress = false
        private const val TAG = "RNGoogleFit"
    }

    init {
        mReactContext.addActivityEventListener(this)
    }

    override fun onNewIntent(intent: Intent?) {
    }

    override fun onActivityResult(activity: Activity?, requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
            mAuthInProgress = false

            if (resultCode == Activity.RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                Log.d("FIT", "everything is fine")
            } else if (resultCode == Activity.RESULT_CANCELED) {
                val requestedScopes =  mSignInAccount.requestedScopes
                val grantedScopes = mSignInAccount.grantedScopes
                Log.e(TAG, "Authorization - Cancel")
                val map = Arguments.createMap()
                map.putString("message", "" + "Authorization cancelled")
                sendEvent(mReactContext, "GoogleFitAuthorizeFailure", map)
            }
        }
    }
}