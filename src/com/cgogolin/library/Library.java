package com.cgogolin.library;

import java.io.File;
import java.lang.reflect.Method; //Used below to fix an issue with highliting in webview

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

// import android.webkit.WebView;
// import android.webkit.WebViewClient;
// import android.webkit.WebChromeClient;
import android.webkit.MimeTypeMap;

import android.net.Uri;

import android.os.AsyncTask;

public class Library extends Activity implements OnItemClickListener, SearchView.OnQueryTextListener, SearchView.OnCloseListener
{
    Context context;
    
    public static final String GLOBAL_SETTINGS = "global settings";
    private String libraryUrlString = "/mnt/sdcard/";
    private String pathTargetString = "home/username";
    private String pathReplacementString = "/mnt/sdcard";
    private String pathPrefixString = "";
    
//    private EditText editText;
    private String oldQueryText = "";
    private ListView bibtexListView = null;
    private BibtexAdapter bibtexAdapter = null;
    private ProgressBar progressBar  = null;
    private SearchView searchView = null;
    private boolean applyFilterTaskRunning = false;
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) //Inflates the options menu
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

            // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
//        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconified(false);
            //Respond to a click on the X 
        searchView.setOnCloseListener(this); //Implemented in: public void onClose(View view)
        searchView.setOnQueryTextListener(this); //Implemented in: public boolean onQueryTextChange(String query) and public boolean onQueryTextSubmit(String query)
        return true;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) //Handel clicks in the options menu 
    {
        switch (item.getItemId()) 
        {
                // case android.R.id.home:
                //     return true;
            case R.id.menu_set_library_path:
                setLibraryPath();
                return true;
            case R.id.menu_set_path_conversion:
                setTargetAndReplacementStrings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    

    @Override
    public boolean onClose()
    {
//        oldQueryText = "";
//        searchView.setQuery("",false);
//        resetFilter();
        return false;
    }    

    
    @Override
    public boolean onQueryTextChange(String query) { //This is a hacky way to determine when the user has reset the text field with the X button 
        if ( query.length() == 0 && oldQueryText.length() > 1 && bibtexAdapter != null && bibtexAdapter.getStatus() == BibtexAdapter.STATUS_OK ) {
            resetFilter();
        }
        oldQueryText = query;
        return false;
    }

    
    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    
    @Override
    public void onItemClick(AdapterView parent, View view, int position, long id) //Handle clicks on items in the ListView
    {
            //Focus the listView and close the keyboard
        parent.requestFocus();
        hideKeyboard();
        
            //Create the PopupMenu
        PopupMenu popupMenu = new PopupMenu(context, view);
        Menu menu = popupMenu.getMenu();
        popupMenu.getMenuInflater().inflate(R.menu.bibtex_context_menu, popupMenu.getMenu()); //API level 11 compati equivalent to popupMenu.inflate(R.menu.bibtex_context_menu);
        
            //Read from the Files list from the BibtexEntry
        List<String> associatedFilesList = bibtexAdapter.getFiles(position);
        if (associatedFilesList != null)
        {
            for (String file : associatedFilesList)
            {
                    //Modify the base of the url
                final String url = pathPrefixString + file.replace(pathTargetString,pathReplacementString);
                if ( url == null || url.equals("") ) continue;
                
                    //Add an item to the menu and

                // Uri testUri = Uri.parse(url);
                // if(!(new File(testUri.getPath())).isFile()) Toast.makeText(getApplicationContext(),"could not find file: "+url,Toast.LENGTH_SHORT).show();                    
                menu.add(getString(R.string.open)+": "+url).setOnMenuItemClickListener( new OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item)
                            {
//                                if( (new File(url)).exists())
//                                if( (new File(url)).isFile())
                                Uri uri = Uri.parse(url);
                                if(uri != null && (new File(uri.getPath())).isFile()) 
                                {
                                        //Determine mime type
                                    MimeTypeMap map = MimeTypeMap.getSingleton();
                                    
                                    //String extension = map.getFileExtensionFromUrl(url);
                                    String extension ="";
                                    if (url.lastIndexOf(".") != -1) extension = url.substring((url.lastIndexOf(".") + 1), url.length());
                                    
                                    String type = map.getMimeTypeFromExtension(extension);
                                    
                                        //Start application to open the file
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setDataAndType(uri, type);
//                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    try 
                                    {
                                        startActivity(intent);
                                    }
                                    catch (ActivityNotFoundException e) 
                                    {
                                        Toast.makeText(getApplicationContext(),getString(R.string.no_application_to_view_files_of_type)+" "+type+".",Toast.LENGTH_SHORT).show();
                                    }
                                }
                                else
                                {
                                    Toast.makeText(getApplicationContext(),getString(R.string.couldnt_find_file)+" "+url+".\n\n"+getString(R.string.path_conversion_hint),Toast.LENGTH_LONG).show();
                                    
                                }
                                return true;
                            }
                    });
            }
        }
        
        
            //Read from the URLs list from the BibtexEntry
        List<String> associatedUrlList = bibtexAdapter.getUrls(position);
        if (associatedUrlList != null)
        {
            for (final String url : associatedUrlList)
            {
                if ( url == null || url.equals("") ) continue;
            
                menu.add(getString(R.string.url)+": "+url).setOnMenuItemClickListener( new OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item)
                            {
                            
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(url));
//                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                try 
                                {
                                    startActivity(intent);
                                }
                                catch (ActivityNotFoundException e) 
                                {
                                    Toast.makeText(getApplicationContext(),getString(R.string.error_opening_webbrowser),Toast.LENGTH_SHORT).show();
                                }
                                return true;
                            }
                    });
            }
        }
    

            //Read from the DOIs list from the BibtexEntry
        List<String> associatedDoiList = bibtexAdapter.getDois(position);
        if (associatedDoiList != null)
        {
            for (final String doi : associatedDoiList)
            {
                if ( doi == null || doi.equals("") ) continue;
                        
                menu.add(getString(R.string.doi)+": "+doi).setOnMenuItemClickListener( new OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item)
                            {
                            
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(doi));
//                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                try 
                                {
                                    startActivity(intent);
                                }
                                catch (ActivityNotFoundException e) 
                                {
                                    Toast.makeText(getApplicationContext(),getString(R.string.error_opening_webbrowser),Toast.LENGTH_SHORT).show();
                                }
                                return true;
                            }
                    });
            }
        }

                    //Add a share button
        final String entryString = bibtexAdapter.getEntryAsString(position);
        menu.add(getString(R.string.share)).setOnMenuItemClickListener( new OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item)
                    {
                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.setType("plain/text");
                        shareIntent.setType("*/*");
                        shareIntent.putExtra(Intent.EXTRA_TEXT, entryString);
                        try 
                        {
                            startActivity(shareIntent);
                        }
                        catch (ActivityNotFoundException e) 
                        {
                            Toast.makeText(getApplicationContext(),getString(R.string.error_starting_share_intent),Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    }
            });
        
        if (popupMenu.getMenu().size() > 0 ) popupMenu.show();
    }

    
    @Override
    public void onBackPressed() //Handles clicks on the back button 
    {
        if (searchView.getQuery().toString().equals("")) finish();
        resetFilter();
    }


    @Override
    public void onNewIntent(Intent intent) { //Is called when a search is performed
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {   
            if (bibtexAdapter != null)
            {
                if(bibtexAdapter.getStatus() == BibtexAdapter.STATUS_OK)
                {
                        //Apply the filter now
                    applyFilter(intent.getStringExtra(SearchManager.QUERY));
                        //Focus the listView and close the keyboard
                    bibtexListView.requestFocus();
                    hideKeyboard();
                }
                else
                {
                        //Set the initial filter so that filtering is done in the constructor of the BibtexAdapter after loading the entries 
                    bibtexAdapter.setInitialFilter(intent.getStringExtra(SearchManager.QUERY));
                }
            }
        }   
    }

    
    @Override
    public void onCreate(Bundle savedInstanceState) //Main
    {
        context = Library.this;
            
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bibtexlist);
        
        loadGlobalSettings(); //Load seetings (uses default if not set)

        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        
        prepareBibtexListView();
        bibtexAdapter = (BibtexAdapter) getLastNonConfigurationInstance(); //Try to retrive the saved state of the BibtexAdapter from before a state change
        prepareBibtexAdapter(); //Does nothing if retriving was successfull
    }

    
    // @Override
    // protected void onResume() //When we resume
    // {   
    //     super.onResume();
    // }
    
        
    @Override
    protected void onStop() //Before we stop
    {
        super.onStop();
        
            //Write settings
        SharedPreferences globalSettings = getSharedPreferences(GLOBAL_SETTINGS, MODE_PRIVATE);
        SharedPreferences.Editor globalSettingsEditor = globalSettings.edit();
        globalSettingsEditor.putString("bibtexUrlString", libraryUrlString);
        globalSettingsEditor.putString("pathTargetString", pathTargetString);
        globalSettingsEditor.putString("pathReplacementString", pathReplacementString);
        globalSettingsEditor.putString("pathPrefixString", pathPrefixString);
        globalSettingsEditor.commit();
    }


    @Override
    public Object onRetainNonConfigurationInstance() //Saves the state of the BibtexAdapter before a state change
    {
        return bibtexAdapter;
    }
    
    
    public void setLibraryPath() //Set the bibtex library path from user input
    {
        final LinearLayout editTextLayout = new LinearLayout(context);
        editTextLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        editTextLayout.setOrientation(1);
        editTextLayout.setPadding(16, 0, 16, 0);
        final EditText input = new EditText(this);
        input.setRawInputType(0x00000011); // 0x00000011=textUri
        input.setSingleLine();
        input.setText(libraryUrlString);
        editTextLayout.addView(input);
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.menu_set_library_path))
            .setMessage(getString(R.string.please_enter_path_of_bibtex_library))
            .setView(editTextLayout)
            .setPositiveButton(getString(R.string.save), new DialogInterface.OnClickListener() 
                {
                    public void onClick(DialogInterface dialog, int whichButton) 
                        {
                            String newLibraryUrlString = input.getText().toString();
                            SharedPreferences globalSettings = getSharedPreferences(GLOBAL_SETTINGS, MODE_PRIVATE);
                            SharedPreferences.Editor globalSettingsEditor = globalSettings.edit();
                            globalSettingsEditor.putString("bibtexUrlString", newLibraryUrlString);
                            globalSettingsEditor.commit();
                            libraryUrlString = newLibraryUrlString;
                                //loadGlobalSettings();
                            prepareBibtexAdapter();
                        }
                })
            .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() 
                    {public void onClick(DialogInterface dialog, int whichButton) {}}) //Do nothing
            .show();
    }

    
    public void setTargetAndReplacementStrings() //Set the target and repacement strings from user input
    {
        final LinearLayout editTextLayout = new LinearLayout(context);
        editTextLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        editTextLayout.setOrientation(1);
        editTextLayout.setPadding(16, 0, 16, 0);
        final EditText input1 = new EditText(this);
        final EditText input2 = new EditText(this);
        final EditText input3 = new EditText(this);
        input1.setRawInputType(0x00000011); // 0x00000011=textUri
        input2.setRawInputType(0x00000011); // 0x00000011=textUri
        input3.setRawInputType(0x00000011); // 0x00000011=textUri
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
                    {public void onClick(DialogInterface dialog, int whichButton) {}}) //Do nothing
            .show();
    }


    private void loadGlobalSettings() //Load seetings (uses default if not set)
    {
        SharedPreferences globalSettings = getSharedPreferences(GLOBAL_SETTINGS, MODE_PRIVATE);
        libraryUrlString = globalSettings.getString("bibtexUrlString", libraryUrlString);
        pathTargetString = globalSettings.getString("pathTargetString", pathTargetString);
        pathReplacementString = globalSettings.getString("pathReplacementString", pathReplacementString);
        pathPrefixString = globalSettings.getString("pathPrefixString", pathPrefixString);
    }

    
    private void prepareBibtexListView() //Prepares the bibtexListView
    {
            //Create the ListView
        bibtexListView = (ListView) findViewById(R.id.bibtex_list_view);
        
            //Handle clicks on items in the ListView
        bibtexListView.setOnItemClickListener(this); //Implemented in: public void onItemClick(AdapterView parent, View view, int position, long id)
    }


    private void prepareBibtexAdapter() //Prepares the bibtexAdapter
    {
            //Create the Adapter that will fill the list with content
        new PrepareBibtexAdapterTask().execute(libraryUrlString);
    }

    
    private class PrepareBibtexAdapterTask extends AsyncTask<String, Void, BibtexAdapter> { //Manages asynchronous execution of the creation of the BibtexAdapter and updates the UI before and once finished

        protected void onPreExecute()
        {
            bibtexListView.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        }
        
        protected BibtexAdapter doInBackground(String... libraryUrlString) {
                //If the BibtexAdapter is already initialized do nothing
            if (bibtexAdapter == null || bibtexAdapter.getStatus() != BibtexAdapter.STATUS_OK)
                bibtexAdapter = new BibtexAdapter(context, libraryUrlString[0]);
                //If it is now correctly initialized apply the filter
            if(bibtexAdapter != null && bibtexAdapter.getStatus() == BibtexAdapter.STATUS_OK && searchView != null)
                bibtexAdapter.applyFilter(searchView.getQuery().toString());
            return bibtexAdapter;
        }
        
        protected void onPostExecute(BibtexAdapter bibtexAdapter) {
            progressBar.setVisibility(View.GONE);
            if(bibtexAdapter == null || bibtexAdapter.getStatus() != BibtexAdapter.STATUS_OK)
            {
                    //If the Adapter was not initialized correctly complain
                if(bibtexAdapter == null)
                    Toast.makeText(context, "BibtexAdapter not initialized", Toast.LENGTH_LONG).show();
                else
                    switch (bibtexAdapter.getStatus())
                    {
                        case BibtexAdapter.STATUS_NOT_INITIALIZED:
                            Toast.makeText(context, getString(R.string.adapter_not_initialized), Toast.LENGTH_LONG).show();
                            break;
                        case BibtexAdapter.STATUS_FILE_NOT_FOUND:
                            Toast.makeText(context, getString(R.string.unable_to_find_file)+" "+bibtexAdapter.getLibraryUrlString()+".", Toast.LENGTH_LONG).show();
                            break;
                        case BibtexAdapter.STATUS_IO_EXCEPTION:
                            Toast.makeText(context, getString(R.string.io_exception_while_reading)+" "+bibtexAdapter.getLibraryUrlString()+".", Toast.LENGTH_LONG).show();
                            break;
                        case BibtexAdapter.STATUS_IO_EXCEPTION_WHILE_CLOSING:
                            Toast.makeText(context, getString(R.string.io_exception_while_closing)+" "+bibtexAdapter.getLibraryUrlString()+".", Toast.LENGTH_LONG).show();
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
                new PrepareBibtexAdapterForFilteringTask().execute();
            }
        }
    }


    private class PrepareBibtexAdapterForFilteringTask extends AsyncTask<Void, Void, Void> { //Manages asynchronous execution of the caching that speeds up search operations
        
        protected Void doInBackground(Void... na) {
            bibtexAdapter.prepareForFiltering();
            return null;
        }

        // protected void onPostExecute(Void na) {
        //     Toast.makeText(context, "Caching done", Toast.LENGTH_LONG).show();
        // }
    }
    

    private void applyFilter(String searchString) //Applies the search filter
    {
            //Check if there is already a filter beeing applied 
        if (applyFilterTaskRunning) return;
        applyFilterTaskRunning = true;
            //Apply the filter
        new ApplyFilterTask().execute(searchString);
    }

    
    private class ApplyFilterTask extends AsyncTask<String,Void,String> //Manages asynchronous execution of the filter process and updates the UI before and once finished - somehow AsyncTask<String,Void,Void> with return null; didn't work...
    {
        protected void onPreExecute()
        {
            bibtexListView.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        }
            
        protected String doInBackground(String... searchString) {
            bibtexAdapter.applyFilter(searchString[0]);
            return searchString[0];
        }
            
        protected void onPostExecute(String searchString) {
            bibtexAdapter.notifyDataSetChanged();
            progressBar.setVisibility(View.GONE);
            bibtexListView.setVisibility(View.VISIBLE);
            applyFilterTaskRunning = false;
        }        
    }


    private boolean resetFilter() //Resets the search filter
    {
        if (bibtexAdapter == null) return false;
        boolean wasReset = bibtexAdapter.resetFilter();
        if (wasReset) {
            bibtexAdapter.notifyDataSetChanged();
            searchView.setQuery("",false);
        }
        return wasReset;
    }


    private void hideKeyboard()
    {
        InputMethodManager inputMethodManager = (InputMethodManager)  getSystemService(INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }
    
}
