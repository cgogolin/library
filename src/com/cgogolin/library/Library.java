//Can be built and tested using:
// 
// ant debug && ~/src/android/android-sdk-linux/platform-tools/adb uninstall com.cgogolin.library &&  ~/src/android/android-sdk-linux/platform-tools/adb install bin/Library-debug.apk
//
//
package com.cgogolin.library;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ActivityNotFoundException;

import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.SearchManager;

import android.view.View;
import android.view.Window;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.inputmethod.InputMethodManager;

import android.text.InputType;

import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.PopupMenu;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SearchView;
import android.webkit.MimeTypeMap;

import android.net.Uri;

import android.os.AsyncTask;

public class Library extends Activity implements SearchView.OnQueryTextListener
{
    Context context;
    
    public static final String GLOBAL_SETTINGS = "global settings";
    private String libraryPathString = "/mnt/sdcard/";
    private String pathTargetString = "home/username";
    private String pathReplacementString = "/mnt/sdcard";
    private String pathPrefixString = "";
    BibtexAdapter.SortMode sortMode = BibtexAdapter.SortMode.None;
    
    private String oldQueryText = "";
    private ListView bibtexListView = null;
    private BibtexAdapter bibtexAdapter = null;
    private ProgressBar progressBar  = null;
    private SearchView searchView = null;
    private boolean applyFilterTaskRunning = false;
    private String scheduledFilteringString = null;
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) //Inflates the options menu
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

            // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager)getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchMenuItem = menu.findItem(R.id.menu_search);
        searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
//        searchView.setIconifiedByDefault(true);
        searchView.setIconified(false);

        searchView.setOnCloseListener( new SearchView.OnCloseListener() {
                @Override
                public boolean onClose() {
                    searchView.setIconified(false);//prevent collapsing
                    return true;
                }
            });
        

        searchView.setOnQueryTextListener(this); //Implemented in: public boolean onQueryTextChange(String query) and public boolean onQueryTextSubmit(String query)
        searchView.setMaxWidth(Integer.MAX_VALUE);
        

        MenuItem SelectedSortMenuItem = null;
        switch(sortMode){
            case None:
                SelectedSortMenuItem = menu.findItem(R.id.menu_sort_by_none);
                break;
            case Date:
                SelectedSortMenuItem = menu.findItem(R.id.menu_sort_by_date);
                break;
            case Author:
                SelectedSortMenuItem = menu.findItem(R.id.menu_sort_by_author);
                break;
            case Journal:
                SelectedSortMenuItem = menu.findItem(R.id.menu_sort_by_journal);
                break;
        }
        if(SelectedSortMenuItem!=null)
//            SelectedSortMenuItem.setIcon(R.drawable.ic_done_white_24dp);
            SelectedSortMenuItem.setChecked(true);
        
        return true;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) //Handel clicks in the options menu 
    {
        switch (item.getItemId()) 
        {
            case R.id.menu_set_library_path:
                setLibraryPath();
                return true;
            case R.id.menu_set_path_conversion:
                setTargetAndReplacementStrings();
                return true;
            case R.id.menu_sort_by_none:
                sortMode = BibtexAdapter.SortMode.None;
                sort(sortMode);
                break;
            case R.id.menu_sort_by_date:
                sortMode = BibtexAdapter.SortMode.Date;
                sort(sortMode);
                break;
            case R.id.menu_sort_by_author:
                sortMode = BibtexAdapter.SortMode.Author;
                sort(sortMode);
                break;
            case R.id.menu_sort_by_journal:
                sortMode = BibtexAdapter.SortMode.Journal;
                sort(sortMode);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        invalidateOptionsMenu();
        return true;
    }
    
    
    @Override
    public boolean onQueryTextChange(String query) { //This is a hacky way to determine when the user has reset the text field with the X button 
        if ( query.length() == 0 && oldQueryText.length() > 1 && bibtexAdapter != null && bibtexAdapter.getStatus() == BibtexAdapter.STATUS_OK ) {
            resetFilter();
        }
        oldQueryText = query;
        return true;//prevents from collapsing
    }

    
    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }
    
    
    @Override
    public void onBackPressed() //Handles clicks on the back button 
    {
        if (searchView.getQuery().toString().equals(""))
            super.onBackPressed();
        resetFilter();
    }


    @Override
    public void onNewIntent(Intent intent) { //Is called when a search is performed
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {   
            if (bibtexAdapter != null && bibtexAdapter.getStatus() == BibtexAdapter.STATUS_OK)
            {
                    //Apply the filter now
                applyFilter(intent.getStringExtra(SearchManager.QUERY));
            }
            else
            {
                    //Schedule a filtering that will be performed in the PrepareBibtexAdapterTask
                scheduledFilteringString = intent.getStringExtra(SearchManager.QUERY);
            }
                //Focus the listView and close the keyboard
            bibtexListView.requestFocus();
            hideKeyboard();
        }   
    }

    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        context = this;
            
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bibtexlist);
        
        loadGlobalSettings(); //Load seetings (uses default if not set)

        ActionBar actionBar = getActionBar();
        actionBar.setTitle("");
        actionBar.setIcon(null);
        actionBar.setDisplayShowTitleEnabled(false);
        
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        
        prepareBibtexListView();
        bibtexAdapter = (BibtexAdapter) getLastNonConfigurationInstance(); //Try to retrive the saved state of the BibtexAdapter from before a state change
        prepareBibtexAdapter();
    }

    
    // @Override
    // protected void onResume()
    // {   
    //     super.onResume();
    // }
    
        
    @Override
    protected void onStop()
    {
        super.onStop();
        
            //Write settings
        SharedPreferences globalSettings = getSharedPreferences(GLOBAL_SETTINGS, MODE_PRIVATE);
        SharedPreferences.Editor globalSettingsEditor = globalSettings.edit();
        globalSettingsEditor.putString("bibtexUrlString", libraryPathString);
        globalSettingsEditor.putString("pathTargetString", pathTargetString);
        globalSettingsEditor.putString("pathReplacementString", pathReplacementString);
        globalSettingsEditor.putString("pathPrefixString", pathPrefixString);
        globalSettingsEditor.putString("sortMode", sortMode.toString());
        globalSettingsEditor.commit();
    }


    @Override
    public Object onRetainNonConfigurationInstance() //Saves the state of the BibtexAdapter before a state change
    {
        return bibtexAdapter;
    }
    
    
    public void setLibraryPath() //Open a dialoge to set the bibtex library path from user input
    {
        final LinearLayout editTextLayout = new LinearLayout(context);
        editTextLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        editTextLayout.setOrientation(1);
        editTextLayout.setPadding(16, 0, 16, 0);
        final EditText input = new EditText(this);
        input.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setSingleLine();
        input.setText(libraryPathString);
        editTextLayout.addView(input);
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.menu_set_library_path))
            .setMessage(getString(R.string.please_enter_path_of_bibtex_library))
            .setView(editTextLayout)
            .setPositiveButton(getString(R.string.save), new DialogInterface.OnClickListener() 
                {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) 
                        {
                            String newLibraryPathString = input.getText().toString();
                            SharedPreferences globalSettings = getSharedPreferences(GLOBAL_SETTINGS, MODE_PRIVATE);
                            SharedPreferences.Editor globalSettingsEditor = globalSettings.edit();
                            globalSettingsEditor.putString("bibtexUrlString", newLibraryPathString);
                            globalSettingsEditor.commit();
                            libraryPathString = newLibraryPathString;
                            bibtexAdapter = null;
                            prepareBibtexAdapter();
                        }
                })
            .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() 
                    {public void onClick(DialogInterface dialog, int whichButton) {}}) //Do nothing
            .show();
    }

    
    public void setTargetAndReplacementStrings() //Open a dialoge to set the target and repacement strings from user input
    {
        final LinearLayout editTextLayout = new LinearLayout(context);
        editTextLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        editTextLayout.setOrientation(1);
        editTextLayout.setPadding(16, 0, 16, 0);
        final EditText input1 = new EditText(this);
        final EditText input2 = new EditText(this);
        final EditText input3 = new EditText(this);
        input1.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input2.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input3.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input1.setSingleLine();
        input2.setSingleLine();
        input3.setSingleLine();
        input1.setText(pathTargetString);
        input2.setText(pathReplacementString);
        input3.setText(pathPrefixString);
        final TextView view1 = new TextView(this);
        view1.setText(getString(R.string.target)+":");
        final TextView view2 = new TextView(this);
        view2.setText(getString(R.string.replacement)+":");
        final TextView view3 = new TextView(this);
        view3.setText(getString(R.string.prefix)+":");
        editTextLayout.addView(view1);
        editTextLayout.addView(input1);
        editTextLayout.addView(view2);
        editTextLayout.addView(input2);
        editTextLayout.addView(view3);
        editTextLayout.addView(input3);        
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.menu_set_path_conversion))
            .setMessage(getString(R.string.menu_set_path_conversion_help))
            .setView(editTextLayout)
            .setPositiveButton(getString(R.string.save), new DialogInterface.OnClickListener() 
                {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) 
                        {
                            String newPathTargetString = input1.getText().toString().trim();
                            String newPathReplacementString = input2.getText().toString().trim();
                            String newpathPrefixString = input3.getText().toString().trim(); 
                            SharedPreferences globalSettings = getSharedPreferences(GLOBAL_SETTINGS, MODE_PRIVATE);
                            SharedPreferences.Editor globalSettingsEditor = globalSettings.edit();
                            globalSettingsEditor.putString("pathTargetString", newPathTargetString);
                            globalSettingsEditor.putString("pathReplacementString", newPathReplacementString);
                            globalSettingsEditor.putString("pathPrefixString", newpathPrefixString);
                            globalSettingsEditor.commit();
                            pathTargetString = newPathTargetString;
                            pathReplacementString = newPathReplacementString;
                            pathPrefixString = newpathPrefixString;
                                //loadGlobalSettings();
                        }
                })
            .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() 
                {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {}
                }) //Do nothing
            .show();
    }


    private void loadGlobalSettings() //Load seetings (uses default if not set)
    {
        SharedPreferences globalSettings = getSharedPreferences(GLOBAL_SETTINGS, MODE_PRIVATE);
        libraryPathString = globalSettings.getString("bibtexUrlString", libraryPathString);
        pathTargetString = globalSettings.getString("pathTargetString", pathTargetString);
        pathReplacementString = globalSettings.getString("pathReplacementString", pathReplacementString);
        pathPrefixString = globalSettings.getString("pathPrefixString", pathPrefixString);
        sortMode = BibtexAdapter.SortMode.valueOf(globalSettings.getString("sortMode", "None"));
    }

    
    private void prepareBibtexListView() //Prepares the bibtexListView
    {
            //Create the ListView
        bibtexListView = (ListView) findViewById(R.id.bibtex_list_view);
    }


    private void prepareBibtexAdapter() //Prepares the bibtexAdapter asynchronously 
    {
            //Create the Adapter that will fill the list with content
        AsyncTask<String, Void, BibtexAdapter> PrepareBibtexAdapterTask = new AsyncTask<String, Void, BibtexAdapter>() { //Manages asynchronous execution of the creation of the BibtexAdapter and updates the UI before and once finished
            @Override
            protected void onPreExecute()
            {
                if(bibtexListView != null)
                    bibtexListView.setVisibility(View.GONE);
                if(progressBar != null)
                    progressBar.setVisibility(View.VISIBLE);
            }
            @Override
            protected BibtexAdapter doInBackground(String... libraryPathString) {
                    //If the BibtexAdapter is already correctly initialized do nothing
                if (bibtexAdapter == null || bibtexAdapter.getStatus() != BibtexAdapter.STATUS_OK)
                {
                    Uri libraryUri = Uri.parse(libraryPathString[0]);
                    File libraryFile = new File(Uri.decode(libraryUri.getEncodedPath()));
                    InputStream inputStream = null;
                    try
                    {
                        if(libraryFile != null && libraryFile.isFile())
                        {
                            inputStream = new FileInputStream(libraryFile);
                        }
                        if(inputStream == null && libraryUri.toString().startsWith("content://"))
                        {
                            inputStream = context.getContentResolver().openInputStream(libraryUri);
                        }
                    }
                    catch(Exception e)
                    {
                            //Nothing to do here as BibtexAdapter will anyway complain if inputStream=null
                    }
                    bibtexAdapter = new BibtexAdapter(inputStream) {
                            @Override
                            String getModifiedPath(String path) {
                                    //Some versions of Android suffer from this very stupid bug:
                                    //http://stackoverflow.com/questions/16475317/android-bug-string-substring5-replace-empty-string
                                return pathPrefixString + (pathTargetString.equals("") ? path : path.replace(pathTargetString,pathReplacementString));
                            }
                        };
                }
                    //If it is now correctly initialized apply the filter
                if(scheduledFilteringString != null && bibtexAdapter != null && bibtexAdapter.getStatus() == BibtexAdapter.STATUS_OK)
                {
                    bibtexAdapter.applyFilter(scheduledFilteringString);
                    scheduledFilteringString = null;
                }
                bibtexAdapter.sort(sortMode);
                return bibtexAdapter;
            }
            @Override
            protected void onPostExecute(BibtexAdapter bibtexAdapter) {
                progressBar.setVisibility(View.GONE);
                if(bibtexAdapter == null || bibtexAdapter.getStatus() != BibtexAdapter.STATUS_OK)
                {
                        //If the Adapter was not initialized correctly complain
                    if(bibtexAdapter == null)
                        Toast.makeText(context, context.getString(R.string.adapter_null), Toast.LENGTH_LONG).show();
                    else
                        switch (bibtexAdapter.getStatus())
                        {
                            case BibtexAdapter.STATUS_NOT_INITIALIZED:
                                Toast.makeText(context, getString(R.string.adapter_not_initialized), Toast.LENGTH_LONG).show();
                                break;
                            case BibtexAdapter.STATUS_IO_EXCEPTION:
                                Toast.makeText(context, getString(R.string.io_exception_while_reading)+" "+libraryPathString+".", Toast.LENGTH_LONG).show();
                                break;
                            case BibtexAdapter.STATUS_IO_EXCEPTION_WHILE_CLOSING:
                                Toast.makeText(context, getString(R.string.io_exception_while_closing)+" "+libraryPathString+".", Toast.LENGTH_LONG).show();
                                break;
                        }
                    setLibraryPath();
                }
                else
                {
                        //Bind the Adapter to the UI and update
                    bibtexAdapter.notifyDataSetChanged();
                    bibtexListView.setAdapter(bibtexAdapter);
                    bibtexListView.setVisibility(View.VISIBLE);
                    
                        //Do some caching to speed up searches
                    AsyncTask<BibtexAdapter, Void, Void> PrepareBibtexAdapterForFilteringTask = new AsyncTask<BibtexAdapter, Void, Void>() { //Manages asynchronous execution of the caching that speeds up search operations
                        @Override
                        protected Void doInBackground(BibtexAdapter... bibtexAdapter) {
                            bibtexAdapter[0].prepareForFiltering();
                            return null;
                        }
                    };
                    PrepareBibtexAdapterForFilteringTask.execute(bibtexAdapter);
                }
            }
        };
        PrepareBibtexAdapterTask.execute(libraryPathString);
    } 
        
    

    private void applyFilter(String searchString) //Applies the search filter
    {
            //Check if there is already a filter beeing applied 
        if (applyFilterTaskRunning) return;
        applyFilterTaskRunning = true;
            //Apply the filter
        AsyncTask<String,Void,String> applyFilterTask = new AsyncTask<String,Void,String>() {//Manages asynchronous execution of the filter process and updates the UI before and once finished - somehow AsyncTask<String,Void,Void> with return null doesn't work, so I went for AsyncTask<String,Void,String>
            @Override
            protected void onPreExecute()
            {
                bibtexListView.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
            }
            @Override
            protected String doInBackground(String... searchString) {
                if(bibtexAdapter != null)
                    bibtexAdapter.applyFilter(searchString[0]);
                return searchString[0];
            }
            @Override
            protected void onPostExecute(String searchString) {
                if(bibtexAdapter != null)
                    bibtexAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                bibtexListView.setVisibility(View.VISIBLE);
                applyFilterTaskRunning = false;
            }        
        };
        applyFilterTask.execute(searchString);
    }
    

    private void resetFilter() //Resets the search filter
    {
        applyFilter("");
    }


    private void sort(BibtexAdapter.SortMode sortMode) 
    {
        if(bibtexAdapter==null)
            return;
        
        AsyncTask<BibtexAdapter.SortMode,Void,BibtexAdapter.SortMode> sortTask = new AsyncTask<BibtexAdapter.SortMode,Void,BibtexAdapter.SortMode>() {//Manages asynchronous execution of the sort process and updates the UI before and once finished - somehow AsyncTask<String,Void,Void> with return null doesn't work, so I went for AsyncTask<BibtexAdapter.SortMode,Void,BibtexAdapter.SortMode>
            @Override
            protected void onPreExecute()
            {
                bibtexListView.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
            }
            @Override
            protected BibtexAdapter.SortMode doInBackground(BibtexAdapter.SortMode... sortMode) {
                if(bibtexAdapter != null)
                    bibtexAdapter.sort(sortMode[0]);
                return sortMode[0];
            }
            @Override
            protected void onPostExecute(BibtexAdapter.SortMode sortMode) {
                if(bibtexAdapter != null)
                    bibtexAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                bibtexListView.setVisibility(View.VISIBLE);
            }        
        };
        sortTask.execute(sortMode);
    }

    
    private void hideKeyboard() 
    {
        InputMethodManager inputMethodManager = (InputMethodManager)  getSystemService(INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }
    
}
