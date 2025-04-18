package com.example.androidproject;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.location.Address;
import android.location.Geocoder;
import java.util.List;
import java.util.Locale;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;



/**
 * Creates the functionality to create a post
 */
public class CreatePostActivity extends AppCompatActivity {
    LocationHelper locationHelper;
    Button moodDropdown;
    EditText reasonText;
    RadioButton aloneButton;
    RadioButton crowdButton;
    RadioButton groupButton;
    Button confirmButton;
    RadioButton pairButton;
    Button cancelButton;
    RadioButton privateButton;
    RadioButton publicButton;
    LinearLayout imageButton;
    LinearLayout locationButton;
    MoodSelectionAdapter dropdownAdapter;
    ListView dropdownList;
    CardView imagePreviewCard;
    ImageView imagePreview;
    String chosenMood;
    String chosenSituation;
    Uri chosenImage;
    Boolean dropdownStatus;
    Boolean imageStatus = Boolean.FALSE;
    CreatePostActivity current;
    String user;
    Location moodLocation;
    Boolean locationState = Boolean.FALSE;
    TextView locationStateText;
    private RadioButton currentlySelectedButton = null;


    private CardView locationPreviewCard;
    private TextView locationAddressText;

    /**
     * Handles the main loop of the program
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Sets current
        current = this;
        // Gets given data
        Bundle dataGiven = getIntent().getExtras();
        // If there is data gets the user
        if (dataGiven != null) {
            user = (String) dataGiven.get("currentUser");
        } else {
            // If no data was given sets the user to a test value
            user = "testUser";
        }
        super.onCreate(savedInstanceState);
        // Sets content view
        setContentView(R.layout.activity_add_mood);


        // Gets the nav bar
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.nav_bar_container, NavBarFragment.newInstance(user))
                    .commit();
        }
        // Finds all of the views
        moodDropdown = findViewById(R.id.AddMoodSelectMood);
        reasonText = findViewById(R.id.addReason);
        aloneButton = findViewById(R.id.AddradioAlone);
        crowdButton = findViewById(R.id.AddradioCrowd);
        groupButton = findViewById(R.id.AddradioGroup);
        pairButton = findViewById(R.id.AddradioPair);
        dropdownList = findViewById(R.id.add_mood_select_mood_list);
        confirmButton = findViewById(R.id.add_done_button);
        cancelButton = findViewById(R.id.add_cancel_button);
        imageButton = findViewById(R.id.AddImagebutton);
        imagePreviewCard = findViewById(R.id.imagePreviewCardView);
        imagePreview = findViewById(R.id.moodImageView);
        privateButton = findViewById(R.id.addPrivate);
        publicButton = findViewById(R.id.addPublic);
        locationButton = findViewById(R.id.add_location_button);
        locationStateText  = findViewById(R.id.location_state_text);
        locationPreviewCard = findViewById(R.id.locationPreviewCardView);
        locationAddressText = findViewById(R.id.locationTextView);
        //Sets drop down status to false to start
        dropdownStatus = Boolean.FALSE;
        //Taken from https://developer.android.com/training/basics/intents/result
        //Authored by Google Developers
        //Taken by Dalton Low
        //Taken on March 3, 2025
        ActivityResultLauncher<String> launcher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    /**
                     *  Creates a method that once the user has selected the image runs sets chosen image to it
                     * @param uri
                     *      image location selected
                     */
                    @Override
                    public void onActivityResult(Uri uri) {
                        // End of citation
                        chosenImage = uri;

                        if (!isImageSizeValid(chosenImage)) {
                            chosenImage = null;
                            return;
                        }

                        imagePreviewCard.setVisibility(View.VISIBLE);
                        imagePreview.setImageURI(chosenImage);
                        if (uri != null) {
                            findViewById(R.id.add_image_text).setVisibility(View.INVISIBLE);
                            findViewById(R.id.remove_image_text).setVisibility(View.VISIBLE);
                            imageStatus = Boolean.TRUE;
                        }
                    }
                });
        //Adds all the moods for the dropdowns
        ArrayList<String> moodList = new ArrayList<>();
        moodList.add("Anger");
        moodList.add("Confusion");
        moodList.add("Disgust");
        moodList.add("Fear");
        moodList.add("Happiness");
        moodList.add("Sadness");
        moodList.add("Shame");
        moodList.add("Surprise");
        // Sets the adapter for the list
        dropdownAdapter = new MoodSelectionAdapter(this, moodList);
        dropdownList.setAdapter(dropdownAdapter);

        //Sets the moodDropdown button action
        moodDropdown.setOnClickListener(new View.OnClickListener() {
            /**
             * Creates a button that allows the drop down feature for the mood list
             * @param v The view that was clicked.
             */
            @Override
            public void onClick(View v) {
                // If the drop down is open close it
                if (dropdownStatus) {
                    dropdownList.setVisibility(View.GONE);
                } else {
                    // If the drop down is closed make it visible
                    dropdownList.setVisibility(View.VISIBLE);
                    dropdownList.bringToFront();
                }
                dropdownStatus = !dropdownStatus;
            }
        });
        // Sets the option to choose an item
        dropdownList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            /**
             * Allows the user to interact with a single drop down list item to select it
             * @param parent The AdapterView where the click happened.
             * @param view The view within the AdapterView that was clicked (this
             *            will be a view provided by the adapter)
             * @param position The position of the view in the adapter.
             * @param id The row id of the item that was clicked.
             */
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                chosenMood = moodList.get(position);
                dropdownStatus = Boolean.FALSE;
                dropdownList.setVisibility(View.GONE);
                moodDropdown.setText(chosenMood);
            }
        });
        // Sets the alone option button
        aloneButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Creates an option to choose the alone option
             * @param v The view of alone button.
             */
            @Override
            public void onClick(View v) {
                if (currentlySelectedButton == aloneButton) {
                    aloneButton.setChecked(false);
                    currentlySelectedButton = null;
                    chosenSituation = null;
                } else {
                    if (currentlySelectedButton != null) {
                        currentlySelectedButton.setChecked(false);
                    }
                    aloneButton.setChecked(true);
                    currentlySelectedButton = aloneButton;
                    chosenSituation = "Alone";
                }
            }
        });
        // Sets the pair option button
        pairButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Creates an option to choose the alone option
             * @param v The view of alone button.
             */
            @Override
            public void onClick(View v) {
                if (currentlySelectedButton == pairButton) {
                    pairButton.setChecked(false);
                    currentlySelectedButton = null;
                    chosenSituation = null;
                } else {
                    if (currentlySelectedButton != null) {
                        currentlySelectedButton.setChecked(false);
                    }
                    pairButton.setChecked(true);
                    currentlySelectedButton = pairButton;
                    chosenSituation = "Pair";
                }
            }
        });
        // Sets the crowd option button
        crowdButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Creates an option to chose
             * @param v The view of crowd button.
             */
            @Override
            public void onClick(View v) {
                if (currentlySelectedButton == crowdButton) {
                    crowdButton.setChecked(false);
                    currentlySelectedButton = null;
                    chosenSituation = null;
                } else {
                    if (currentlySelectedButton != null) {
                        currentlySelectedButton.setChecked(false);
                    }
                    crowdButton.setChecked(true);
                    currentlySelectedButton = crowdButton;
                    chosenSituation = "Crowd";
                }
            }
        });
        // Sets the group option button
        groupButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Creates an option to select group feature
             * @param v The view of group button.
             */
            @Override
            public void onClick(View v) {
                if (currentlySelectedButton == groupButton) {
                    groupButton.setChecked(false);
                    currentlySelectedButton = null;
                    chosenSituation = null;
                } else {
                    if (currentlySelectedButton != null) {
                        currentlySelectedButton.setChecked(false);
                    }
                    groupButton.setChecked(true);
                    currentlySelectedButton = groupButton;
                    chosenSituation = "Group";
                }
            }
        });
        //Sets the image button on click listener
        imageButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Creates an option to select images
             * @param v The view of the image button.
             */
            @Override
            public void onClick(View v) {
                // Runs if an image hasn't already been selected
                if (!imageStatus) {
                    // Creates the flags that we need
                    launcher.launch("image/*");
                    Log.e("TEST", "adasdasd");
                    Log.e("TEST", "JOASDADadasdasd");
                } else {
                    // Sets the status of if there is an image to false
                    imageStatus = Boolean.FALSE;
                    // Stops showing the image preview
                    imagePreviewCard.setVisibility(View.INVISIBLE);
                    imagePreview.setImageDrawable(null);
                    // Removes the image
                    chosenImage = null;
                    // Displays text saying to add image
                    findViewById(R.id.add_image_text).setVisibility(View.VISIBLE);
                    findViewById(R.id.remove_image_text).setVisibility(View.INVISIBLE);
                }
            }
        });

        locationButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Allows a user to attach a location to the mood
             * @param v The view that was clicked.
             */
            @Override
            public void onClick(View v) {

                if (locationState == Boolean.FALSE) {
                    LocationPermissionFragment locationFragment = new LocationPermissionFragment();
                    locationFragment.show(getSupportFragmentManager(), "LocationPermissionFragment"); // Shows fragment

                    // Once permission is granted inside the fragment, trigger tracking
                    locationFragment.setOnPermissionGrantedListener(() -> {
                        Log.d("CreatePostActivity", "Starting location tracking...");
                        locationFragment.startTrackingLocation();

                    locationFragment.getLastKnownLocation(new LocationPermissionFragment.OnLocationReceivedListener() {
                        @Override
                        public void onLocationReceived(Location location) {
                            moodLocation = location;
                            // Set sea level so that there is no invocation error
                            moodLocation.setMslAltitudeAccuracyMeters(0);
                            moodLocation.setMslAltitudeMeters(0);
                            locationStateText.setText("Remove Location");
                            locationState = Boolean.TRUE;
                            displayLocationAddress(location);
                        }

                        @Override
                        public void onLocationFailure(String errorMessage) {
                            Log.e("CreatePostActivity", "Failed to get location: " + errorMessage);
                        }
                    });});
                } else {
                    moodLocation = null;
                    locationStateText.setText("Add Location");
                    locationState = Boolean.FALSE;
                    locationPreviewCard.setVisibility(View.GONE);
                }
            }
        });

        // Sets the confirm on click listener
        confirmButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Finsihes the mood while checking all requirements
             * @param v The confirm button
             */
            @Override
            public void onClick(View v) {
                // Gives an error message if trying to enter without setting mood
                if (chosenMood == null) {
                    CharSequence sequence = "Cannot Continue Without Mood.";
                    Toast.makeText(current,sequence, Toast.LENGTH_SHORT).show();
                } else {
                    // Creates the new mood state
                    MoodState newMood = new MoodState(chosenMood);
                    // Adds chosen situation if it was specified
                    if (chosenSituation != null) {
                        newMood.setSituation(chosenSituation);
                    }
                    // Adds reason if it was specified
                    if (reasonText.getText().length() != 0) {
                        newMood.setReason(reasonText.getText().toString());
                    }
                    // Sets the location
                    if (moodLocation != null) {
                        newMood.setLocation(moodLocation);
                    }
                    // Sets the post to public if public is chosen
                    if (publicButton.isChecked()) {
                        newMood.setVisibility(Boolean.TRUE);
                    } else if (privateButton.isChecked()){
                        // Sets the post to private
                        newMood.setVisibility(Boolean.FALSE);
                    }
                    // Sets the username
                    newMood.setUser(user);
                    // Adds mood to database
                    Database.getInstance().addMood(newMood);
                    // Adds images to database
                    if (chosenImage != null) {
                        Database.getInstance().addImage(chosenImage, "images/" + newMood.getId(), getContentResolver());
                        CollectionReference moodCol = FirebaseFirestore.getInstance().collection("Moods");
                        moodCol.document(newMood.getId()).update("image", Uri.parse("images/" + newMood.getId()));
                    }
                    finish();
                }
            }
        });
        // Sets the cancel button functionality
        cancelButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Cancels the add mood screen
             * @param v The view that was clicked.
             *      cancel button
             */
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    /**
     * Checks if an image is too large for the database
     * @param chosenImage
     *      path to image in phone
     * @return
     *      boolean of if it is too big
     */
    private boolean isImageSizeValid(Uri chosenImage) {
        try {
            // Opens the image input stream
            InputStream inputStream = getContentResolver().openInputStream(chosenImage);
            int imageSize = inputStream.available();
            inputStream.close();
            // Checks if the image is too large
            if (imageSize >= 65536) {
                Toast.makeText(this, "Image size exceeds 64KB limit. Please select a smaller image.",
                        Toast.LENGTH_LONG).show();
                return false;
            }
            return true;
        } catch (IOException e) {
            // Shows an error if checking image size goes wrong
            Toast.makeText(this, "Error checking image size: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * Displays the address of the location after selecting it
     * @param location
     */
    private void displayLocationAddress(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            // Attempts to get the address from the location
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(), location.getLongitude(), 1);

            // If it finds a valid address store the information
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                String thoroughfare = address.getThoroughfare();
                String locality = address.getLocality();
                String adminArea = address.getAdminArea();
                String countryName = address.getCountryName();

                if (thoroughfare != null) {
                    sb.append(thoroughfare);
                }
                if (locality != null) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(locality);
                }
                if (adminArea != null) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(adminArea);
                }
                if (countryName != null) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(countryName);
                }
                // Sets the text of the address
                locationAddressText.setText(sb.toString());
            } else {
                // If no address found, display coordinates
                locationAddressText.setText(String.format(Locale.getDefault(),
                        "Location: %.6f, %.6f", location.getLatitude(), location.getLongitude()));
            }

            // Show the location preview
            locationPreviewCard.setVisibility(View.VISIBLE);

        } catch (IOException e) {
            // In case of error, display coordinates
            locationAddressText.setText(String.format(Locale.getDefault(),
                    "Location: %.6f, %.6f", location.getLatitude(), location.getLongitude()));
            locationPreviewCard.setVisibility(View.VISIBLE);
        }
    }
}
