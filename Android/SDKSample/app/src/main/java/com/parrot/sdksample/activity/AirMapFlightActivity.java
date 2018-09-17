package com.parrot.sdksample.activity;

import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.Toast;

import com.airmap.airmapsdk.AirMapException;
import com.airmap.airmapsdk.models.Coordinate;
import com.airmap.airmapsdk.models.aircraft.AirMapAircraft;
import com.airmap.airmapsdk.models.aircraft.AirMapAircraftModel;
import com.airmap.airmapsdk.models.flight.AirMapFlight;
import com.airmap.airmapsdk.networking.callbacks.AirMapCallback;
import com.airmap.airmapsdk.networking.callbacks.SuccessCallback;
import com.airmap.airmapsdk.networking.services.AirMap;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import timber.log.Timber;

public abstract class AirMapFlightActivity extends AppCompatActivity {

    private static final String PARROT_MANUFACTURER_ID = "6f25a640-60d9-4b98-8602-67094b7a8914";

    private AirMapFlight flight;
    private AirMapAircraft aircraft;

    private double currentLat;
    private double currentLng;
    private double currentAlt;

    protected void onStartingTakeoff() {
        if (flight == null || !flight.isActive()) {
            if (currentLat != 0 && currentLng != 0) {
                createFlight(currentLat, currentLng);
            }
        }
    }

    protected void onLanding() {
        if (flight != null && flight.isActive()) {
            endFlight();
        }
    }

    protected void onPositionChanged(double lat, double lng, float altitude) {
        if (flight == null || flight.getFlightId() == null) {
            currentLat = lat;
            currentLng = lng;
            currentAlt = altitude;
        } else {
            AirMap.getTelemetryService().sendPositionMessage(flight.getFlightId(), lat, lng, altitude, altitude, 0);
        }
    }

    //TODO: use the ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_SPEEDCHANGED events to send telemetry
    protected void onSpeedUpdate(float velocityX, float velocityY, float velocityZ) {
        if (flight == null || flight.getFlightId() == null) {
            return;
        }
        AirMap.getTelemetryService().sendSpeedMessage(flight.getFlightId(), velocityX, velocityY, velocityZ);
    }

    //TODO: use the ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_ATTITUDECHANGED events to send attitude
    protected void onAttidueUpdate(float yaw, float pitch, float roll) {
        if (flight == null || flight.getFlightId() == null) {
            return;
        }
        AirMap.getTelemetryService().sendAttitudeMessage(flight.getFlightId(),yaw, pitch, roll);
    }

    /**
     *  Creates flight on AirMap
     */
    private void createFlight(final double lat, final double lng) {
        // user must be authenticated prior to submitting a flight
        if (TextUtils.isEmpty(AirMap.getAuthToken())) {
            // authentication via anonymous login
            AirMap.performAnonymousLogin(getUserId(), new SuccessCallback<Void>() {
                @Override
                protected void onSuccess(Void response) {
                    createFlight(lat, lng);
                }
            });
            return;
        }

        flight = new AirMapFlight();
        Coordinate takeoffCoordinate = new Coordinate(lat, lng);
        flight.setCoordinate(takeoffCoordinate);
        flight.setBuffer(1000); // 1000 meters
        flight.setMaxAltitude(100); // 100 meters
        flight.setNotify(true); // notify airports that accept digital notice
        flight.setPublic(true); // show in the AirMap app

        Date now = new Date(System.currentTimeMillis() + (60 * 1000));
        flight.setStartsAt(now);

        Date in2Hours = new Date(now.getTime() + (2 * 60 * 60 * 1000));
        flight.setEndsAt(in2Hours);

        // create Parrot aircraft for this flight
        if (aircraft != null) {
            AirMap.createAircraft(aircraft, new SuccessCallback<AirMapAircraft>() {
                @Override
                protected void onSuccess(AirMapAircraft response) {
                    if (response != null) {
                        flight.setAircraft(response);
                    }
                    submitFlight();
                }
            });
        } else {
            submitFlight();
        }
    }

    private void submitFlight() {
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
                toast("Failed to create flight");
                Timber.e(e, "Submitting flight failure");
            }
        });
    }

    /**
     *  Create an aircraft for this Parrot aircraft and add to this flight
     */
    protected void createAircraft() {
        AirMap.getModels("6f25a640-60d9-4b98-8602-67094b7a8914", new SuccessCallback<List<AirMapAircraftModel>>() {
            @Override
            protected void onSuccess(List<AirMapAircraftModel> response) {
                if (response == null || response.isEmpty()) {
                    return;
                }

                for (AirMapAircraftModel model : response) {
                    if (!model.getName().toLowerCase().equals(getParrotAircraftName().toLowerCase())) {
                        continue;
                    }

                    aircraft = new AirMapAircraft();
                    aircraft.setNickname(getParrotAircraftName());
                    aircraft.setModel(model);
                    break;
                }
            }
        });
    }

    public abstract String getParrotAircraftName();

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

    protected void toast(String text) {
        Toast.makeText(AirMapFlightActivity.this, text, Toast.LENGTH_SHORT).show();
    }

    /**
     *  @return true if activity is still alive and in the foreground
     */
    private boolean isActive() {
        return !isDestroyed() && !isFinishing();
    }
}
