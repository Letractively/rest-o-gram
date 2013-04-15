package com.tau;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.tau.client.RestogramClient;
import com.tau.location.LocationTracker;
import com.tau.tasks.*;


public class MyActivity extends Activity implements ITaskObserver {

    private LocationTracker locationTracker;

    /**
     * Called when the activity is first created.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        locationTracker = new LocationTracker(this);

        setContentView(R.layout.main);

        clear();
        init();

        watchLocation();
    }

    @Override
    protected void onStop() {
        super.onStop();
        locationTracker.stopTracking();
    }

    private void watchLocation() {
        if (locationTracker.canGetLocation()) {
            Log.d("Your Location", "latitude:" + locationTracker.getLatitude() + ", longitude: " + locationTracker.getLongitude());
        }
        else
            showSettingsAlert();
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        if (requestCode == 1650)
        {
            if (locationTracker.canGetLocation()) {
                Log.d("Your Location", "latitude:" + locationTracker.getLatitude() + ", longitude: " + locationTracker.getLongitude());
            }
        }
    }

    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        // Setting Dialog Title
        alertDialog.setTitle("Location Services Settings");

        // Setting Dialog Message
        alertDialog
                .setMessage("Location services are not enabled. Would you like to enable these services?");

        final MyActivity main = this;

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(
                                Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        main.startActivityForResult(intent, 1650);
                    }
                });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        // Showing Alert Message
        alertDialog.show();
    }

    public void onGetLocationClicked(View view)
    {
        if (locationTracker.isLocationValid())
        {
            updateLocation(locationTracker.getLatitude(), locationTracker.getLongitude());
        }
    }

    public void onGoClicked(View view)
    {
        clear();

        double latitude, longitude, radius;

        EditText et1 = (EditText)findViewById(R.id.editTextLat);
        EditText et2 = (EditText)findViewById(R.id.editTextLon);

        try
        {
            latitude = Double.parseDouble(et1.getText().toString());
            longitude = Double.parseDouble(et2.getText().toString());
        }
        catch(NumberFormatException e)
        {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        SeekBar sb1 = (SeekBar) findViewById(R.id.seekBarRadius);
        radius = sb1.getProgress();

        updateText("Searching...");

        RestogramClient.getInstance().getNearby(latitude, longitude, radius, this);
        Log.d("Get Nearby Task", "lat: " + latitude + ", long: " + longitude + ", radius: " + radius);
    }

    public void onNextClicked(View view)
    {
        if(currPhotos == null)
            return;

        currPhotoIndex = (currPhotoIndex + 1) % currPhotos.length;

        ImageView image = (ImageView)findViewById(R.id.imageView1);
        String imageUrl = currPhotos[currPhotoIndex].getStandardResolution();
        DownloadImageTask task = new DownloadImageTask(image);
        task.execute(imageUrl);
    }

    public void onFinished(RestogramVenue[] venues)
    {
        if(venues == null || venues.length == 0)
        {
            // TODO: report error
            updateText("No Restaurant Found");
            return;
        }

        RestogramVenue venue = venues[0]; // TODO: fix
        String venueID = venue.getId();

        updateText(venue.getName());

        // Get info
        RestogramClient.getInstance().getInfo(venueID, this);

        // Get photos
        RestogramClient.getInstance().getPhotos(venueID, this);
    }

    public void onFinished(RestogramVenue venue)
    {
        if(venue == null)
        {
            // TODO: report error
            return;
        }

        String imageUrl = venue.getImageUrl();

        ImageView venueImage = (ImageView)findViewById(R.id.imageViewVenue);
        DownloadImageTask task = new DownloadImageTask(venueImage);
        task.execute(imageUrl);
    }

    public void onFinished(RestogramPhoto[] photos)
    {
        // TODO: implementation
        if(photos == null)
        {
            // TODO: report error
            return;
        }

        Button button = (Button)findViewById(R.id.buttonNext);
        button.setClickable(true);

        updatePhotos(photos);
    }

    private void init()
    {
        try
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

            StrictMode.setThreadPolicy(policy);

            final TextView tvRadius = (TextView)findViewById(R.id.textViewRadius);
            SeekBar sbRadius = (SeekBar)findViewById(R.id.seekBarRadius);
            final int stepSize = 10;

            sbRadius.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener()
            {
                public void onProgressChanged(SeekBar seekBar, int progress,
                                              boolean fromUser)
                {
                    // TODO Auto-generated method stub

                    progress = ((int)Math.round(progress/stepSize))*stepSize;
                    seekBar.setProgress(progress);

                    tvRadius.setText("Radius: " + progress);
                }

                public void onStartTrackingTouch(SeekBar seekBar)
                {
                    // TODO Auto-generated method stub
                }

                public void onStopTrackingTouch(SeekBar seekBar)
                {
                    // TODO Auto-generated method stub
                }
            });

        }
        catch (Exception e)
        {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void updatePhotos(RestogramPhoto[] photos)
    {
        currPhotos = photos;
        currPhotoIndex = 0;

        ImageView image = (ImageView)findViewById(R.id.imageView1);
        String imageUrl = photos[currPhotoIndex].getStandardResolution();
        DownloadImageTask task = new DownloadImageTask(image);
        task.execute(imageUrl);
    }

    private void clear()
    {
        currPhotos = null;
        currPhotoIndex = -1;

        updateText("");

        ImageView venueImage = (ImageView)findViewById(R.id.imageViewVenue);
        venueImage.setImageResource(android.R.color.transparent);

        ImageView image = (ImageView)findViewById(R.id.imageView1);
        image.setImageResource(android.R.color.transparent);

        Button button = (Button)findViewById(R.id.buttonNext);
        button.setClickable(false);
    }

    private void updateText(String text)
    {
        TextView tv = (TextView)findViewById(R.id.textView);
        tv.setText(text);
        tv.invalidate();
    }

    private void updateLocation(double lat, double lon)
    {
        EditText et1 = (EditText)findViewById(R.id.editTextLat);
        EditText et2 = (EditText)findViewById(R.id.editTextLon);

        et1.setText(Double.toString(lat));
        et2.setText(Double.toString(lon));
    }

    private RestogramPhoto[] currPhotos;
    private int currPhotoIndex;
}
