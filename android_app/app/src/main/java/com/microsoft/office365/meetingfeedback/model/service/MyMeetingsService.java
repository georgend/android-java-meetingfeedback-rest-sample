/*
 *  Copyright (c) Microsoft. All rights reserved. Licensed under the MIT license. See full license at the bottom of this file.
 */
package com.microsoft.office365.meetingfeedback.model.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.microsoft.office365.meetingfeedback.ConnectActivity;
import com.microsoft.office365.meetingfeedback.MeetingFeedbackApplication;
import com.microsoft.office365.meetingfeedback.model.Constants;
import com.microsoft.office365.meetingfeedback.model.DataStore;
import com.microsoft.office365.meetingfeedback.model.webservice.RatingServiceManager;
import com.microsoft.office365.meetingfeedback.util.SharedPrefsUtil;
import com.microsoft.services.odata.impl.ADALDependencyResolver;

import java.util.Map;

import javax.inject.Inject;

import dagger.ObjectGraph;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MyMeetingsService extends IntentService {

    public static final String MY_MEETINGS_SERVICE = "MyMeetingsService";
    private static final String TAG = "MyMeetingsService";
    public static final String EVENT_ID = "EVENT_ID";
    private ObjectGraph applicationGraph;
    public static final int MEETING_REQUEST_CODE = 1;

    @Inject
    DataStore mDataStore;
    @Inject
    RatingServiceManager mRatingServiceManager;
    @Inject
    SharedPrefsUtil mSharedPrefsUtil;
    @Inject
    ADALDependencyResolver mADALDependencyResolver;

    private String mUsername;
    private Map<String, Double> mSavedMeetingResults;
    private NotificationManager mNotificationManager;
    private ADALDependencyResolver mResolver;

    public MyMeetingsService() {
        super(MY_MEETINGS_SERVICE);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MeetingFeedbackApplication application = (MeetingFeedbackApplication) getApplication();
        applicationGraph = application.getApplicationGraph();
        applicationGraph.inject(this);
        mUsername = mSharedPrefsUtil.getSavedUsername();
        mSavedMeetingResults = mSharedPrefsUtil.getSavedMeetingResults();
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Polling for new Meeting Ratings...");
        mRatingServiceManager.loadMyMeetings(mUsername, new Callback<MyMeetingsResponse>() {
            @Override
            public void success(MyMeetingsResponse meetingResponse, Response response) {
                Log.d(TAG, "success!");
                //todo: compare the shared preferences version with the new version
                Map<String, Double> newMeetingResponse = meetingResponse.toMap();
                for (String id : newMeetingResponse.keySet()) {
                    Double savedCountForMeeting = mSavedMeetingResults.get(id);
                    Double newCountForMeeting = newMeetingResponse.get(id);
                    //if old meeting response didnt have the key
                    if (!mSavedMeetingResults.containsKey(id)) {
                        Log.d(TAG, "RATING COUNT CHANGED! Send a notification for " + id + "!");
                        sendNotificationForEvent(id);
                    }
                    if (savedCountForMeeting != null && newCountForMeeting != null
                            && !savedCountForMeeting.equals(newCountForMeeting)) {
                        Log.d(TAG, "RATING COUNT CHANGED! Send a notification for " + id + " !");
                        sendNotificationForEvent(id);
                    }
                }
                mDataStore.setMyMeetings(newMeetingResponse);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "An error occured", error);
            }
        });
    }

    private void sendNotificationForEvent(String id) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
                .setTicker("New Rating Received!")
                .setAutoCancel(true)
                .setContentTitle("New Rating Received!")
                .setContentText("Your meeting has received a new rating. Click to view");
        Intent intent = new Intent(this, ConnectActivity.class);
        intent.putExtra(EVENT_ID, id);

        mADALDependencyResolver.setResourceId(Constants.DISCOVERY_RESOURCE_ID);

        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder.setContentIntent(pIntent);
        mNotificationManager.notify(0, builder.build());
    }

}

// *********************************************************
//
// O365-Android-MeetingFeedback, https://github.com/OfficeDev/O365-Android-MeetingFeedback
//
// Copyright (c) Microsoft Corporation
// All rights reserved.
//
// MIT License:
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
// *********************************************************