package com.example.fiftysix;

import static android.content.ContentValues.TAG;



import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.squareup.picasso.Picasso;


import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jp.wasabeef.picasso.transformations.CropCircleTransformation;

public class AttendeeMainActivity extends AppCompatActivity {

    private ImageButton profile_button;
    private ImageButton notification_button;
    private ImageButton qrcode_button;
    private ImageButton home_button;

    private FirebaseFirestore db;
    private CollectionReference attEventRef;
    private CollectionReference eventRef;
    private ViewFlipper viewFlipper;
    private Attendee attendee;
    private String eventCheckinID;
    private RecyclerView recyclerViewMyEvents;
    private RecyclerView recyclerViewAllEvents;
    private ArrayList<Event> myEventDataList;
    private ArrayList<Event> allEventDataList;
    private ViewFlipper viewflipper;
    private RecyclerView recyclerView;
    private CollectionReference imageRef;

    // Openening camera
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private Uri cameraImageUri = null; // To store the camera image URI
    private Profile posterHandler;
    private Uri selectedImageUri = null;
    private static final int REQUEST_CAMERA_PERMISSION = 201;

    // Buttons on home pages
    private ImageButton attendeeAddEventButton;
    private ImageButton attendeeProfileButton;
    private ImageButton attendeeNotificationButton;
    private ImageButton attendeeHomeButton;
    private Button browseAllEventsButton;
    private Button browseMyEvents;

    // Buttons on Add event page
    private Button addEventScanCheckinButton;
    private ImageButton addEventBackImageButton;
    private Button browseEventsButton;

    //Profile edit page
    private ImageButton profileImage;
    private Button profileSave;
    private Button profileBack;
    private EditText profileName;
    private EditText profileEmail;
    private EditText profilePhoneNumber;
    private EditText profileBio;
    private Button profileRemovePic;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("TAG", "onClick:   not working ");
        setContentView(R.layout.attendee_flipper);


        // Please add ALL FUTURE LAYOUTS to view flipper. this way we can switch layouts without switching activities and the app will run faster.
        viewFlipper = findViewById(R.id.attendeeFlipper);
        Context context = getApplicationContext();
        db = FirebaseFirestore.getInstance();



        setButtons();
        setEditText();
        attendee = new Attendee(context);









        // The user is stored as an organizer and should be returned to main page
       // if (attendee.getUserType()=="organizer"){
            //TODO: doesn't work but shows general idea, need to query the data to check user type from firebase
            //finish();

        //}

        myEventDataList = new ArrayList<>();
        allEventDataList = new ArrayList<>();
        //myEventDataList.add(new Event("temp location", "temp location", "temp Date", "temp details", 11, 100));

        recyclerViewMyEvents = findViewById(R.id.attendeeHomeRecyclerView);
        recyclerViewAllEvents = findViewById(R.id.attendeeHomeRecyclerViewAllEvents);
        //setRecyclerView();


        //db.collection('users').doc()



        Map<String,Object> emptyHash = new HashMap<>();
        emptyHash.put("test", "test");
        db = FirebaseFirestore.getInstance();
        attEventRef = db.collection("Users").document(attendee.getDeviceId()).collection("upcomingEvents");
        eventRef = db.collection("Events");
        imageRef = db.collection("Images");

        AttendeeMyEventAdapter attendeeMyEventAdapter = new AttendeeMyEventAdapter(myEventDataList);
        recyclerViewMyEvents.setAdapter(attendeeMyEventAdapter);
        recyclerViewMyEvents.setHasFixedSize(false);


        AttendeeMyEventAdapter attendeeAllEventAdapter = new AttendeeMyEventAdapter(allEventDataList);
        recyclerViewAllEvents.setAdapter(attendeeAllEventAdapter);
        recyclerViewAllEvents.setHasFixedSize(false);




        //________________________________________UpdatesHomePageData________________________________________




        // Adds events from database to the attendee home screen. Will only show events the attendee has signed up for.
        attEventRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot querySnapshots,
                                @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.e("Firestore", error.toString());
                    return;
                }
                if (querySnapshots != null) {
                    myEventDataList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshots) {
                        String eventID = doc.getId();
                        eventRef.document(eventID).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                            @Override
                            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                                if (error != null) {
                                    Log.e("Firestore", error.toString());
                                    return;
                                }
                                if (querySnapshots != null) {
                                    String eventName = value.getString("eventName");
                                    Integer inAttendeeLimit = value.getLong("attendeeLimit").intValue();
                                    Integer inAttendeeCount = value.getLong("attendeeCount").intValue();
                                    String imageUrl = value.getString("posterURL");
                                    String inDate = value.getString("date");
                                    String location = value.getString("location");
                                    String details = value.getString("details");
                                    String posterID = value.getString("posterID");
                                    db.collection("Images").document(posterID).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                                        @Override
                                        public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                                            if (value != null) {
                                                String imageUrl = value.getString("image");
                                                myEventDataList.add(new Event(eventName, location, inDate, details, inAttendeeCount, inAttendeeLimit, imageUrl));
                                                attendeeMyEventAdapter.notifyDataSetChanged();
                                            }
                                        }
                                    });


                                }
                            }
                        });
                        Log.d("Firestore", "hello");
                    }
                }
            }
        });


            // Displays all events
            eventRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                    if (value != null) {
                        allEventDataList.clear();

                        for (QueryDocumentSnapshot doc : value) {
                            if(doc.getId() != "temp") {


                                String eventName = doc.getString("eventName");
                                Integer inAttendeeLimit = doc.getLong("attendeeLimit").intValue();
                                Integer inAttendeeCount = doc.getLong("attendeeCount").intValue();
                                String imageUrl = doc.getString("posterURL");
                                String inDate = doc.getString("date");
                                String location = doc.getString("location");
                                String details = doc.getString("details");
                                String posterID = doc.getString("posterID");

                                db.collection("Images").document(posterID).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                                    @Override
                                    public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {

                                        if (value != null) {

                                            String imageUrl = value.getString("image");
                                            allEventDataList.add(new Event(eventName, location, inDate, details, inAttendeeCount, inAttendeeLimit, imageUrl));
                                            attendeeAllEventAdapter.notifyDataSetChanged();
                                        }
                                    }
                                });
                            }
                        }
                    }

                }
            });






        //________________________________________HomePage_______________________________________

        attendeeProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profileView(v);
            }
        });



        browseAllEventsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                allEventsView(v);
            }
        });

        browseMyEvents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: For Botsian
                myEventsView(v);
            }
        });




        attendeeAddEventButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addEventsView(v); // Opens add event page

            }
        });



        attendeeNotificationButton.setOnClickListener(v -> {
            //startActivity(new Intent(AttendeeMainActivity.this, Notification.class));
        });


        //______________________________________Sign in to event Page_______________________________________

        addEventBackImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myEventsView(v);
            }
        });


        addEventScanCheckinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanCode();
                myEventsView(v);
            }
        });




        //________________________________________UpdatesProfileInfo________________________________________



        // Displays all events
        db.collection("Users").document(attendee.getDeviceId()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (value != null){

                        String imageURL = value.getString("profileImageURL");
                        String name = value.getString("name");
                        String email = value.getString("email");
                        String phone = value.getString("phoneNumber");
                        String bio = value.getString("bio");

                        profileName.setText(name);
                        profileEmail.setText(email);
                        profilePhoneNumber.setText(phone);
                        profileBio.setText(bio);

                        Picasso.get()
                                .load(imageURL)
                                .fit().transform(new CropCircleTransformation())
                                .into(profileImage);

                }
            }
        });













        //________________________________________Profile________________________________________

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (result && cameraImageUri != null) {
                        selectedImageUri = cameraImageUri;
                        // Now the selectedImageUri contains the URI of the captured image
                    }
                }
        );


        profileSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String name = String.valueOf(profileName.getText());
                String email = String.valueOf(profileEmail.getText());
                String phone = String.valueOf(profilePhoneNumber.getText());
                String bio = String.valueOf(profileBio.getText());


                Profile profile = new Profile(attendee.getDeviceId());
                profile.editProfile(attendee.getDeviceId() , name, email, phone, bio);

                if (selectedImageUri != null) {

                    posterHandler.uploadImageAndStoreReference(selectedImageUri, new Profile.ProfileUploadCallback() {
                        @Override
                        public void onUploadSuccess(String imageUrl) {

                            if (imageUrl != null) {

                                // Stores image URL
                                posterHandler.storeImageInUser(imageUrl);
                                Log.d(TAG, "IMAGE URL PROFILE = " + imageUrl);

                                Picasso.get()
                                        .load(imageUrl)
                                        .fit().transform(new CropCircleTransformation())
                                        .into(profileImage);
                                //organizerEventAdapter.notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onUploadFailure(Exception e) {
                            Log.d(TAG, "IMAGE URL PROFILE = Failed");

                        }


                    });
                }


                Picasso.get()
                        .load(profile.getProfileURL())
                        .fit().transform(new CropCircleTransformation())
                        .into(profileImage);


                // Profile profile = new profile(ID, Name, Email, phone, home)
            }
        });


        posterHandler = new Profile(attendee.getDeviceId());
        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageSourceDialog();


            }
        });

        profileBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myEventsView(v);
            }
        });

        profileRemovePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.collection("Users").document(attendee.getDeviceId()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        String name  = documentSnapshot.getString("name");
                        db.collection("Users").document(attendee.getDeviceId()).update("profileImageURL", "https://ui-avatars.com/api/?rounded=true&name="+ name +"&background=random&size=512");
                    }
                });
            }
        });
    }









    //________________________________________Methods________________________________________


    private void myEventsView(View v){
        viewFlipper.setDisplayedChild(0);
    }

    private void allEventsView(View v){
        viewFlipper.setDisplayedChild(1);
    }

    private void addEventsView(View v){
        viewFlipper.setDisplayedChild(2);
    }

    private void profileView(View v){
        viewFlipper.setDisplayedChild(3);
    }


    private void previousView(View v){
        viewFlipper.showPrevious();
    }

    private void nextView(View v){
        viewFlipper.showNext();
    }



    private void showImageSourceDialog() {
        CharSequence[] items = {"Upload from Gallery", "Upload from Camera"};
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Upload Profile Photo");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @SuppressLint("QueryPermissionsNeeded")
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: // Upload from Gallery
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        galleryLauncher.launch(intent);
                        startActivity(intent);
                        break;
                    case 1: // Upload from Camera
                        Log.d(TAG, "Attempting to launch camera.");
                        if (ContextCompat.checkSelfPermission(AttendeeMainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            Log.d(TAG, "RequestCameraPermissionCalled");
                            requestCameraPermission();
                        } else {
                            Log.d(TAG, "Permission is already granted");
                            openCamera();
                        }
                        break;
                }
            }
        });
        builder.show();
    }


    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(AttendeeMainActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, open camera
                openCamera();
            } else {
                // Permission was denied
                Toast.makeText(this, "Camera permission is required to use the camera", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Error occurred while creating the file", ex);
                return;
            }
            Uri photoURI = FileProvider.getUriForFile(AttendeeMainActivity.this, "com.example.fiftysix.fileProvider", photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            cameraLauncher.launch(photoURI);
        } else {
            Log.d(TAG, "No app can handle the camera intent.");
        }
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName,".jpg",storageDir);
        cameraImageUri = Uri.fromFile(image);
        return image;
    }





    private void setButtons(){
        // Home page buttons
        attendeeAddEventButton = (ImageButton) findViewById(R.id.buttonAttendeeSignInEvent);
        attendeeProfileButton = (ImageButton) findViewById(R.id.buttonAttendeeProfile);
        attendeeNotificationButton = (ImageButton) findViewById(R.id.buttonAttendeeNotificationBell);
        attendeeHomeButton = (ImageButton) findViewById(R.id.buttonAttendeeHome);
        browseAllEventsButton = (Button) findViewById(R.id.browseAllEvents);
        browseMyEvents = (Button) findViewById(R.id.browseMyEvents);

        // Add event page buttons
        addEventScanCheckinButton = (Button) findViewById(R.id.buttonAttendeeCheckinWithQR);
        browseEventsButton = (Button) findViewById(R.id.buttonAttendeeBrowseEvent);
        addEventBackImageButton = (ImageButton) findViewById(R.id.buttonAttendeeBackSignUp);



        // Profile tab buttons
        profileBack = (Button) findViewById(R.id.profile_back);
        profileSave = (Button) findViewById(R.id.profile_save);
        profileImage = (ImageButton) findViewById(R.id.profile_image);
        profileRemovePic = (Button) findViewById(R.id.profile_remove_pic);

    }


    private void setEditText() {
        profileName = (EditText) findViewById(R.id.profile_name);
        profileEmail = (EditText) findViewById(R.id.profile_email);
        profilePhoneNumber = (EditText) findViewById(R.id.profile_phone);
        profileBio = (EditText) findViewById(R.id.profile_home);
    }



    // "youtube - Implement Barcode QR Scanner in Android studio barcode reader | Cambo Tutorial" - youtube channel = Cambo Tutorial
    private void scanCode(){
        ScanOptions options = new ScanOptions();
        options.setPrompt("Volume up to turn on flash");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class);
        barLauncher.launch(options);
    }
    ActivityResultLauncher<ScanOptions> barLauncher = registerForActivityResult(new ScanContract(), result->{

        if(result.getContents() != null){
            AlertDialog.Builder builder = new AlertDialog.Builder(AttendeeMainActivity.this);
            builder.setTitle("Checked In");
            builder.setMessage(result.getContents());
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    eventCheckinID = result.getContents().toString();
                    attendee.checkInToEvent(eventCheckinID);

                    dialog.dismiss();
                }
            }).show();
        }
    });
}
