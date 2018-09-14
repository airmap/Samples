package com.parrot.sdksample.activity;

import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.Toast;

import com.airmap.airmapsdk.AirMapException;
import com.airmap.airmapsdk.models.Coordinate;
import com.airmap.airmapsdk.models.flight.AirMapFlight;
import com.airmap.airmapsdk.networking.callbacks.AirMapCallback;
import com.airmap.airmapsdk.networking.callbacks.SuccessCallback;
import com.airmap.airmapsdk.networking.services.AirMap;

import java.util.Date;
import java.util.UUID;

import timber.log.Timber;

public abstract class AirMapFlightActivity extends AppCompatActivity {

    private AirMapFlight flight;

    protected void onStartingTakeoff() {
        if (flight == null || !flight.isActive()) {
            createFlight();
        }
    }

    protected void onLanding() {
        if (flight != null && flight.isActive()) {
            endFlight();
        }
    }

    protected void onTelemetryLocationUpdate(double lat, double lng, float altitude) {
        AirMap.getTelemetryService().sendPositionMessage(flight.getFlightId(), lat, lng, altitude, altitude, 0);
    }

    protected void onTelemetrySpeedUpdate(float velocityX, float velocityY, float velocityZ) {
        AirMap.getTelemetryService().sendSpeedMessage(flight.getFlightId(), velocityX, velocityY, velocityZ);
    }

    protected void onTelemetryAttidueUpdate(float yaw, float pitch, float roll) {
        AirMap.getTelemetryService().sendAttitudeMessage(flight.getFlightId(),yaw, pitch, roll);
    }

    /**
     *  Creates flight on AirMap
     */
    private void createFlight() {
        // user must be authenticated prior to submitting a flight
        if (TextUtils.isEmpty(AirMap.getAuthToken())) {

            // authentication via anonymous login
            AirMap.performAnonymousLogin(getUserId(), new SuccessCallback<Void>() {
                @Override
                protected void onSuccess(Void response) {
                    createFlight();
                }
            });
            return;
        }

        flight = new AirMapFlight();
        Coordinate takeoffCoordinate = new Coordinate(34.0195, -118.4912);
        flight.setCoordinate(takeoffCoordinate);
        flight.setBuffer(100); // 100 meters
        flight.setNotify(true);
        flight.setPublic(true);

        Date now = new Date();
        flight.setStartsAt(now);

        Date in4Hours = new Date(now.getTime() + (4 * 60 * 60 * 1000));
        flight.setEndsAt(in4Hours);

        AirMap.createFlight(flight, new AirMapCallback<AirMapFlight>() {
            @Override
            protected void onSuccess(AirMapFlight response) {
                flight = response;

                Timber.v("Submitting flight success");
                if (isActive()) {
                    toast("Flight Submitted to AirMap!");
                }
            }

            @Override
            protected void onError(AirMapException e) {
                Timber.e(e,"Submitting flight failure");
            }
        });
    }

    /**
     *  Ends flight on AirMap
     */
    private void endFlight() {
        AirMap.endFlight(flight.getFlightId(), new SuccessCallback<AirMapFlight>() {
            @Override
            protected void onSuccess(AirMapFlight response) {
                Timber.v("Ended flight %s", response.getFlightId());
                flight.setEndsAt(response.getEndsAt());
                toast("Flight Ended on AirMap");
            }
        });
    }

    /**
     *  Return a unique identifier for your pilot
     *  e.g. pilot's email address, phone number or just random UUID
     */
    private String getUserId() {
        return UUID.randomUUID().toString();
    }

    private void toast(String text) {
        Toast.makeText(AirMapFlightActivity.this, text, Toast.LENGTH_SHORT).show();
    }

    private boolean isActive() {
        return !isDestroyed() && !isFinishing();
    }
}
