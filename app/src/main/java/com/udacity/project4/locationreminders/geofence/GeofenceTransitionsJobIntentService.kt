package com.udacity.project4.locationreminders.geofence

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.local.IRemindersRepository
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.sendNotification
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import kotlin.coroutines.CoroutineContext

private const val TAG = "GeofenceTransitionsJobIntentService"

class GeofenceTransitionsJobIntentService : JobIntentService(), CoroutineScope {

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

    companion object {
        private const val JOB_ID = 573

        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(
                context,
                GeofenceTransitionsJobIntentService::class.java, JOB_ID,
                intent
            )
        }
    }

    override fun onHandleWork(intent: Intent) {
        Log.i(TAG, "Geofence triggered")

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        geofencingEvent?.let {
            if (geofencingEvent.hasError()) {
                val errorMessage = errorMessage(this.applicationContext, geofencingEvent.errorCode)
                Log.e(TAG, errorMessage)
                return
            }

            if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                Log.v(TAG, this.applicationContext.getString(R.string.geofence_entered))

                geofencingEvent.triggeringGeofences?.let { triggeringGeofences ->
                    sendNotification(triggeringGeofences)
                }
            }
        }
    }

    private fun sendNotification(triggeringGeofences: List<Geofence>) {
        for (triggeredGeofence in triggeringGeofences) {
            triggeredGeofence.requestId.let { entryId ->
                //Get the local repository instance
                val remindersLocalRepository: IRemindersRepository by inject()

                // Interaction to the repository has to be through a coroutine scope
                CoroutineScope(coroutineContext).launch(SupervisorJob()) {
                    //get the reminder with the request id
                    val result = remindersLocalRepository.getReminder(entryId)
                    if (result is Result.Success<ReminderDTO>) {
                        val reminderDTO = result.data
                        //send a notification to the user with the reminder details
                        sendNotification(
                            this@GeofenceTransitionsJobIntentService, ReminderDataItem(
                                reminderDTO.title,
                                reminderDTO.description,
                                reminderDTO.location,
                                reminderDTO.latitude,
                                reminderDTO.longitude,
                                reminderDTO.id
                            )
                        )
                    }
                }
            }
        }
    }
}
