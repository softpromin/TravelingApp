package prak.travelerapp;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.joda.time.DateTime;

import prak.travelerapp.PlaceApi.PlacePickerFragment;
import prak.travelerapp.TripDatabase.TripDBAdapter;
import prak.travelerapp.TripDatabase.model.TravelType;
import prak.travelerapp.TripDatabase.model.Trip;
import prak.travelerapp.TripDatabase.model.TripItems;
import prak.travelerapp.TripDatabase.model.Tupel;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    private MenueApdapter menueApdapter;
    private DrawerLayout drawerLayout;
    private ListView listView;
    private String[] menue_links;
    private TripDBAdapter tripDBAdapter;
    private Trip active_trip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        Utils.overrideFont(getApplicationContext(), "SERIF", "fonts/Avenir-Book.ttf");
        prepareViews();

        active_trip = checkActiveTrip();

        menueApdapter = new MenueApdapter(this);
        updateMenueRemainingItems(active_trip);
        listView.setAdapter(menueApdapter);
        listView.setOnItemClickListener(this);

        listView.performItemClick(listView.getChildAt(1), 1, listView.getItemIdAtPosition(1));

        //testTripDB();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    public Trip checkActiveTrip(){
        TripDBAdapter tripDBAdapter = new TripDBAdapter(this);
        tripDBAdapter.open();
        active_trip = tripDBAdapter.getActiveTrip();


        if (active_trip != null) {
            boolean tripIsOver = active_trip.getEnddate().isBeforeNow();
            if(tripIsOver){
                Toast.makeText(this, "Aktive Reise is beendet", Toast.LENGTH_LONG).show();
                tripDBAdapter.setAllTripsInactive();
                return null;
            }
        }
        return active_trip;
    }

    private void prepareViews() {
        menue_links = getResources().getStringArray(R.array.menue_links);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        listView = (ListView) findViewById(R.id.drawerList);

        // Prepare youre views
    }

    public void openDrawer(){
        drawerLayout.openDrawer(listView);
    }

    public void closeDrawer(){
        drawerLayout.closeDrawer(listView);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(LOG_TAG,"Selected: " + menue_links[position] + " at " + position);
        Fragment fragment;
        switch(position) {
            case 1:
                if (active_trip != null) {
                    fragment = new LandingFragment();
                    setUpFragment(fragment);
                } else {
                    fragment = new StartFragment();
                    setUpFragment(fragment);
                }
                break;
            case 2:
                if (active_trip != null) {
                    fragment = new ItemViewFragment();
                    setUpFragment(fragment);
                } else {
                    Toast.makeText(this, "Du besitzt keine Aktive Reise", Toast.LENGTH_SHORT).show();
                }
                break;
            case 3:
                fragment = new TripHistoryFragment();
                setUpFragment(fragment);
                break;
            case 4:
                fragment = new SettingsFragment();
                setUpFragment(fragment);
                break;
            case 5:
                fragment = new PlacePickerFragment();
                setUpFragment(fragment);
                break;
        }
        // Highlight the selected items, update the title, and close the drawer
        listView.setItemChecked(position, true);
        closeDrawer();
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 1){
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    public void setUpFragment(Fragment fragment) {
        FragmentManager fragmentManager = getFragmentManager();

        String frag_name = fragment.getClass().getSimpleName();
        if (frag_name.equals("NewTripFragment")){
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        } else {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        boolean isFragmentInStack = isFragmentInBackstack(fragmentManager,fragment.getClass().getSimpleName());

        if (isFragmentInStack){
            //Log.d("Main", "Load Fragment with Tag " + fragment.getClass().getSimpleName() + " from Backstack");
            fragmentManager.popBackStackImmediate(fragment.getClass().getSimpleName(), 0);
        } else {
            //Log.d("Main", "Load Fragment" + fragment.getClass().getSimpleName() + " not from Backstack" );
            fragmentTransaction.replace(R.id.mainContent, fragment);
            fragmentTransaction.addToBackStack(fragment.getClass().getSimpleName());
            fragmentTransaction.commit();
        }

    }

    public static boolean isFragmentInBackstack(final FragmentManager fragmentManager, final String fragmentTagName) {
        for (int entry = 0; entry < fragmentManager.getBackStackEntryCount(); entry++) {
            if (fragmentTagName.equals(fragmentManager.getBackStackEntryAt(entry).getName())) {
                return true;
            }
        }
        return false;
    }

    public void testTripDB(){

        String s = "(3,0);(4,0)";
        TripItems items = new TripItems(s);
        DateTime startDate = new DateTime(2016,01,12,0,0);
        DateTime endDate = new DateTime(2016,01,15,0,0);


        tripDBAdapter = new TripDBAdapter(this);
        tripDBAdapter.open();

        tripDBAdapter.insertTrip(items, "Berlin", "DE" ,startDate,endDate, TravelType.WANDERN, TravelType.SKIFAHREN, false);
        Trip activeTrip = tripDBAdapter.getActiveTrip();
        System.out.println(activeTrip.getCity());
    }

    public Trip getActive_trip() {
        return active_trip;
    }

    public void menueClick(int pos) {
        if (listView != null){
            listView.performItemClick(listView.getChildAt(pos), pos, listView.getItemIdAtPosition(pos));
        }
    }


    //Update die anzahl der verbleibenden Items im Menü
    public void updateMenueRemainingItems(Trip trip){
        int remainingItems = 0;
        for (Tupel t : trip.getTripItems().getItems()) {
            if (t.getY() == 0) {
                remainingItems++;
            }
        }
        menueApdapter.setRemainingItems(remainingItems);
    }

}