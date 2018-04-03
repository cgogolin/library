package com.cgogolin.library;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import android.util.Log;

import android.app.Activity;

import android.os.Bundle;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;

import android.Manifest.permission;

import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.SearchManager;

import android.support.v4.content.FileProvider;
import android.support.v4.provider.DocumentFile;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;

import android.view.View;
import android.view.View.OnClickListener;
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
import android.webkit.MimeTypeMap;

import android.net.Uri;

import android.os.AsyncTask;

public class Library extends AppCompatActivity implements SearchView.OnQueryTextListener
{
    class LibraryBibtexAdapter extends BibtexAdapter {

        private Context context;
        
        public LibraryBibtexAdapter(Context context, InputStream inputStream) throws java.io.IOException {
            super(inputStream);
            this.context = context;
        }
        @Override
        Uri getUriForActionViewIntent(String path) {
            
            if (android.os.Build.VERSION.SDK_INT < 21) {
                Uri uri = Uri.parse("file://"+path);
                File file = null;
                if (path != null && uri != null) {
                    file = new File(uri.getPath());
                }
                if( uri == null || !file.isFile() ) 
                {
                    Toast.makeText(context, context.getString(R.string.couldnt_find_file)+" "+path+".\n\n"+context.getString(R.string.path_conversion_hint),Toast.LENGTH_LONG).show();
                    return null;
                }
                else
                    return uri;
            }
            else {
                    //New versions of Android want files to be shared through a content:// Uri and not via a file:// Uri
                    //First we convert backslashes to slashes and remove Windows style drive letters and then try to idenitfy the uri corresponding to the path in the bibtex file
                Uri uri = getUriInLibraryFolder(path);
                if(uri != null) 
                {
                    Log.i(getString(R.string.app_name), "got the following uri for this path:"+uri.toString()+" and libraryFolderRootUri="+libraryFolderRootUri);
                    DocumentFile file = DocumentFile.fromSingleUri(context, uri);
                }
                else
                {
                    libraryFolderRootUri = null;
                }
                if(uri == null || libraryFolderRootUri == null) {
                    showSetLibraryFolderRootDialog(path);
                    return null;
                }
                else
                    return uri;
            }
        }
        @Override
        String getModifiedPath(String path) {
            if (android.os.Build.VERSION.SDK_INT < 21) 
                    //Some versions of Android suffer from this very stupid bug:
                    //http://stackoverflow.com/questions/16475317/android-bug-string-substring5-replace-empty-string
                return pathPrefixString + (pathTargetString.equals("") ? path : path.replace(pathTargetString,pathReplacementString));
            else
                    //On newer versions of Android we return the unmodified path as finding and opening files is handled in a completely different way...
                return path;
        }
        @Override
        public void onPreBackgroundOperation() {
            bibtexListView.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            
        }
        @Override
        public void onPostBackgroundOperation() {
            progressBar.setVisibility(View.GONE);
            bibtexListView.setVisibility(View.VISIBLE);
        }
        @Override
        public void onEntryClick(View v) {
            hideKeyboard();
        }
    };
    
    private Context context;
    
    public static final String GLOBAL_SETTINGS = "global settings";
    public static final int LIBRARY_FILE_PICK_REQUEST = 0;
    public static final int WRITE_PERMISSION_REQUEST = 1;
    public static final int SET_LIBRARY_FOLDER_ROOT_REQUEST = 2;
    public static final int SELECT_GROUP_REQUEST = 3;

    private boolean libraryWasPreviouslyInitializedCorrectly = false;
    private String libraryPathString = "/mnt/sdcard/";
    private String pathTargetString = "home/username";
    private String pathReplacementString = "/mnt/sdcard";
    private String pathPrefixString = "";
    private Uri libraryFolderRootUri = null;
    private String uriTargetString = null;
    private String uriReplacementString = null;
    private String uriPrefixString = null;

    private AsyncTask<String, Void, Void> prepareBibtexAdapterTask = null;
    private AsyncTask<String, Void, Boolean> analyseLibraryFolderRootTask = null;
    
    BibtexAdapter.SortMode sortMode = BibtexAdapter.SortMode.None;
    String filter = "";
    String group = "";
    private Menu menu = null;

    private String oldQueryText = "";
    private String savedQueryText = null;
    private ListView bibtexListView = null;
    private LibraryBibtexAdapter bibtexAdapter = null;
    private ProgressBar progressBar  = null;
    private android.support.v7.widget.SearchView searchView = null;
    private AlertDialog setLibraryPathDialog = null;
    private AlertDialog setTargetAndReplacementStringsDialog = null;
    private AlertDialog setLibraryFolderRootUriDialog = null;
    private AlertDialog analysingLibraryFolderRootDialog = null;
    private String pathOfFileTrigeredSetLibraryFolderRootDialog = null;
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) //Inflates the options menu
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

            // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager)getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchMenuItem = menu.findItem(R.id.menu_search);
        searchView = (android.support.v7.widget.SearchView)MenuItemCompat.getActionView(searchMenuItem);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconified(false);

        searchView.setOnCloseListener( new SearchView.OnCloseListener() {
                @Override
                public boolean onClose() {
                    searchView.setIconified(false);//prevent collapsing
                    return true;
                }
            });
        searchView.setOnQueryTextListener(this); //Implemented in: public boolean onQueryTextChange(String query) and public boolean onQueryTextSubmit(String query)
//        searchView.setMaxWidth(Integer.MAX_VALUE);//Makes the overflow menu button disappear on API 23
        if(savedQueryText!=null) 
        {
            searchView.setQuery(savedQueryText, true);
            savedQueryText = null;
        }
        
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
            case Title:
                SelectedSortMenuItem = menu.findItem(R.id.menu_sort_by_title);
                break;
        }
        if(SelectedSortMenuItem!=null)
            SelectedSortMenuItem.setChecked(true);

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            MenuItem pathConversionMenuItem = menu.findItem(R.id.menu_set_path_conversion);
            if(pathConversionMenuItem != null) 
                pathConversionMenuItem.setVisible(false);
        }

        this.menu = menu;

        return true;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) //Handel clicks in the options menu 
    {
        switch (item.getItemId()) 
        {
            case R.id.menu_set_library_path:
                showSetLibraryPathDialog();
                return true;
            case R.id.menu_set_path_conversion:
                showSetTargetAndReplacementStringsDialog();
                return true;
            case R.id.menu_sort_by_none:
                sortMode = BibtexAdapter.SortMode.None;
                sortInBackground(sortMode);
                break;
            case R.id.menu_sort_by_date:
                sortMode = BibtexAdapter.SortMode.Date;
                sortInBackground(sortMode);
                break;
            case R.id.menu_sort_by_author:
                sortMode = BibtexAdapter.SortMode.Author;
                sortInBackground(sortMode);
                break;
            case R.id.menu_sort_by_journal:
                sortMode = BibtexAdapter.SortMode.Journal;
                sortInBackground(sortMode);
                break;
            case R.id.menu_sort_by_title:
                sortMode = BibtexAdapter.SortMode.Title;
                sortInBackground(sortMode);
                break;
            case R.id.menu_groups:
                Intent intent = new Intent(this, GroupsActivity.class);
                intent.putExtra("group list", new ArrayList(bibtexAdapter.getGroups()));
                startActivityForResult(intent, SELECT_GROUP_REQUEST);
            default:
                return super.onOptionsItemSelected(item);
        }
        invalidateOptionsMenu();
        return true;
    }
    
    
    @Override
    public boolean onQueryTextChange(String query) { //This is a hacky way to determine when the user has reset the text field with the X button 
        if ( query.length() == 0 && oldQueryText.length() > 1) {
            resetFilter();
        }
        oldQueryText = query;
        return true;
    }

    
    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }
    
    
    // @Override
    // public void onBackPressed() //Handles clicks on the back button 
    // {
    //     if (!searchView.getQuery().toString().equals(""))
    //         searchView.setQuery("", true);
    //     else
    //         super.onBackPressed();
    // }


    @Override
    public void onNewIntent(Intent intent) { //Is called when a search is performed
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            filter = intent.getStringExtra(SearchManager.QUERY);
            filterAndSortInBackground(filter, sortMode, group);
        }
            //Unocus the searchView and close the keyboard
        if(searchView != null)
            searchView.clearFocus();
        hideKeyboard();
    }

    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("");
        actionBar.setIcon(null);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        
        setContentView(R.layout.bibtexlist);
        context = this;
        loadGlobalSettings(); //Load seetings (uses default if not set)
//        bibtexAdapter = (LibraryBibtexAdapter) getLastNonConfigurationInstance(); //retreving doesn't work as the on...BackgroundOpertaion() methods lose their references to the Views
        
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        
        if(savedInstanceState != null)
        {
            savedQueryText = savedInstanceState.getString("SearchQueryText", savedQueryText);
        }
    }

    
    @Override
    protected void onResume()
    {   
        super.onResume();
        
        prepareBibtexListView();
        prepareBibtexAdapter();
    }
    
        
    @Override
    protected void onStop()
    {
        super.onStop();
        
            //Write settings
        SharedPreferences globalSettings = getSharedPreferences(GLOBAL_SETTINGS, MODE_PRIVATE);
        SharedPreferences.Editor globalSettingsEditor = globalSettings.edit();
        globalSettingsEditor.putBoolean("libraryPreviouslyInitialized", libraryWasPreviouslyInitializedCorrectly);
        globalSettingsEditor.putString("bibtexUrlString", libraryPathString);
        globalSettingsEditor.putString("pathTargetString", pathTargetString);
        globalSettingsEditor.putString("pathReplacementString", pathReplacementString);
        globalSettingsEditor.putString("pathPrefixString", pathPrefixString);
        globalSettingsEditor.putString("uriTargetString", uriTargetString);
        globalSettingsEditor.putString("uriReplacementString", uriReplacementString);
        globalSettingsEditor.putString("uriPrefixString", uriPrefixString);
        globalSettingsEditor.putString("sortMode", sortMode.toString());
        globalSettingsEditor.putString("bibtexFolderRootUri", libraryFolderRootUri != null ? libraryFolderRootUri.toString() : "null");
        globalSettingsEditor.commit();
    }


    // @Override
    // public Object onRetainNonConfigurationInstance() //retainig doesn't work as the on...BackgroundOpertaion() methods lose their references to the Views
    // {
    //     return bibtexAdapter;
    // }
    

    @Override
    protected void onSaveInstanceState(Bundle outState) { //Called when the app is destroyed by the system and in various other cases
        super.onSaveInstanceState(outState);

        String searchQueryText = "";
        if(searchView!=null) 
        {
            searchQueryText = searchView.getQuery().toString();
            outState.putString("SearchQueryText", searchQueryText);
        }
    }
    
    
    public void showSetLibraryPathDialog() //Open a dialoge to set the bibtex library path from user input
    {
        if(setLibraryPathDialog != null && setLibraryPathDialog.isShowing())
            return;
        
        final LinearLayout editTextLayout = new LinearLayout(context);
        editTextLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        editTextLayout.setOrientation(1);
        editTextLayout.setPadding(16, 0, 16, 0);
        final EditText input = new EditText(this);
        input.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setText(libraryPathString);
        editTextLayout.addView(input);
        String message = getString(R.string.please_enter_path_of_bibtex_library);
        if(bibtexAdapter == null && libraryWasPreviouslyInitializedCorrectly)
            message = getString(R.string.adapter_failed_to_intialized)+"\n\n"+message;
		if (android.os.Build.VERSION.SDK_INT >= 19){
                /*On newer versions of Android offer to use the file system picker to chose the bibtex library file*/
            final Button button = new Button(context);
            button.setText(getString(R.string.pick_bibtex_library));
            button.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v)
                        {
                            if (android.os.Build.VERSION.SDK_INT >= 21)
                                setLibraryFolderRootUri(null);
                            
                            Intent openDocumentIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                            openDocumentIntent.addCategory(Intent.CATEGORY_OPENABLE);
                            openDocumentIntent.setType("*/*");
                            openDocumentIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                            startActivityForResult(openDocumentIntent, LIBRARY_FILE_PICK_REQUEST);
                        }
                });
            editTextLayout.addView(button);
        }    
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder
            .setTitle(getString(R.string.menu_set_library_path))
            .setMessage(message)
            .setView(editTextLayout)
            .setPositiveButton(getString(R.string.save), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) 
                        {
                            if (android.os.Build.VERSION.SDK_INT >= 21)
                                setLibraryFolderRootUri(null);
                            setLibraryPathDialog = null;
                            setLibraryPath(input.getText().toString().trim());
                            bibtexAdapter = null;
                            prepareBibtexAdapter();
                        }
                })
            .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                    setLibraryPathDialog = null;
                    if(bibtexAdapter == null && prepareBibtexAdapterTask == null)
                        finish();
                }})
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if(bibtexAdapter == null && prepareBibtexAdapterTask == null)
                            finish();
                    }
                });
        setLibraryPathDialog = alertDialogBuilder.show();
    }

    
    public void showSetTargetAndReplacementStringsDialog() //Open a dialoge to set the target and repacement strings from user input
    {
        if(setTargetAndReplacementStringsDialog != null && setTargetAndReplacementStringsDialog.isShowing())
            return;
        
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
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this)
            .setTitle(getString(R.string.menu_set_path_conversion))
            .setMessage(getString(R.string.menu_set_path_conversion_help))
            .setView(editTextLayout)
            .setPositiveButton(getString(R.string.save), new DialogInterface.OnClickListener() 
                {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
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
                        
                        setTargetAndReplacementStringsDialog = null;
                    }
                })
            .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() 
                {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        setTargetAndReplacementStringsDialog = null;
                    }
                });
        
        setTargetAndReplacementStringsDialog = alertDialogBuilder.show();
    }


    public void showSetLibraryFolderRootDialog(final String path)
    {
        if(setLibraryFolderRootUriDialog != null && setLibraryFolderRootUriDialog.isShowing())
            return;

        String message = getString(R.string.dialog_set_library_root_message);
        if(!pathTargetString.equals("obsolete due to update"))
        {
            message = getString(R.string.dialog_set_library_root_message_addition_on_upgrade)+"\n\n"+message;
            pathTargetString = "obsolete due to update";
        }
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_set_library_root_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.dialog_set_library_root_select), new DialogInterface.OnClickListener() 
                {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) 
                        {
                            pathOfFileTrigeredSetLibraryFolderRootDialog = path;
                            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                            startActivityForResult(intent, SET_LIBRARY_FOLDER_ROOT_REQUEST);
                        }
                })
            .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButto) {}
                });
        setLibraryFolderRootUriDialog = alertDialogBuilder.show();
    }
    
    // private void rememberPathOfFileTrigeredSetLibraryFolderRootDialog(String path)
    // {
    //     pathOfFileTrigeredSetLibraryFolderRootDialog = path;
    // }

    private void analyseLibraryFolderRoot(final Uri treeUri)
    {
        if(pathOfFileTrigeredSetLibraryFolderRootDialog == null)
            throw new RuntimeException("pathOfFileTrigeredSetLibraryFolderRootDialog was null, this should not have happened");
        
        if(analysingLibraryFolderRootDialog != null && analysingLibraryFolderRootDialog.isShowing())
            return;
            
        analyseLibraryFolderRootTask = new AsyncTask<String, Void, Boolean>() {
                @Override
                protected void onPreExecute() {
                    
                }
                @Override
                protected Boolean doInBackground(String... path0) {
                    String path = convertToLinuxLikePath(path0[0]);
                    DocumentFile libraryFolderRootDir = DocumentFile.fromTreeUri(context, treeUri);
                    DocumentFile currentDir = libraryFolderRootDir;
                    DocumentFile file = null;
                    String relativePath = "";
                    for (String pathSegment : path.split("/"))
                    {
                        
                        file = currentDir.findFile(pathSegment);
                        if(file != null)
                        {
                                //Log.i(getString(R.string.app_name), "found "+pathSegment);
                            currentDir = file;
                            if(!relativePath.equals(""))
                                relativePath += "/";
                            relativePath += pathSegment;
                        }
                        else
                        {
                                //Log.i(getString(R.string.app_name), "couldn't find "+pathSegment+" in "+currentDir.getUri().toString());
                            relativePath = "";
                        }
                    }
                    if(file != null) 
                    {
                        String fileUriString = file.getUri().toString();
                        uriTargetString = path.substring(0,path.lastIndexOf(relativePath));
                        uriReplacementString = "";
//                        uriPrefixString = (relativePath.equals("") ? fileUriString : fileUriString.replaceLast(Uri.encode(relativePath), ""));
                        uriPrefixString = fileUriString.substring(0,fileUriString.lastIndexOf(Uri.encode(relativePath)));
                        return true;
                    }
                    else
                        return false;
                }
                @Override
                protected void onPostExecute(Boolean succees) {
                    if(succees)
                    {
                        final String path = pathOfFileTrigeredSetLibraryFolderRootDialog;
                        setLibraryFolderRootUri(treeUri);
                        SharedPreferences globalSettings = getSharedPreferences(GLOBAL_SETTINGS, MODE_PRIVATE);
                        SharedPreferences.Editor globalSettingsEditor = globalSettings.edit();
                        globalSettingsEditor.putString("uriTargetString", uriTargetString);
                        globalSettingsEditor.putString("uriReplacementString", uriReplacementString);
                        globalSettingsEditor.putString("uriPrefixString", uriPrefixString);
                        if(analysingLibraryFolderRootDialog != null)
                        {
                                //analysingLibraryFolderRootDialog.cancel();
                            analysingLibraryFolderRootDialog.setMessage(String.format(getString(R.string.dialog_analyse_library_root_message_success), pathOfFileTrigeredSetLibraryFolderRootDialog, libraryFolderRootUri.toString(), getUriInLibraryFolder(pathOfFileTrigeredSetLibraryFolderRootDialog).toString() ));
                            analysingLibraryFolderRootDialog.setTitle(getString(R.string.dialog_analyse_library_root_title_success));
                            analysingLibraryFolderRootDialog.getButton( android.content.DialogInterface.BUTTON_NEGATIVE).setText(R.string.open);
                            analysingLibraryFolderRootDialog.setButton( android.content.DialogInterface.BUTTON_NEGATIVE, getString(R.string.open), new DialogInterface.OnClickListener() 
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whichButton)
                                        {
                                            if(bibtexAdapter != null)
                                                bibtexAdapter.openExternally(context, getUriInLibraryFolder(path));
                                        }
                                });
                            analysingLibraryFolderRootDialog = null;
                        }
                    }
                    else
                    {
                        setLibraryFolderRootUri(null);
                        if(analysingLibraryFolderRootDialog != null)
                        {
                                //analysingLibraryFolderRootDialog.cancel();
                            analysingLibraryFolderRootDialog.setMessage(String.format(getString(R.string.dialog_analyse_library_root_message_failed), pathOfFileTrigeredSetLibraryFolderRootDialog, treeUri.toString()));
                            analysingLibraryFolderRootDialog.setTitle(getString(R.string.dialog_analyse_library_root_title_failed));
                            analysingLibraryFolderRootDialog = null;
                        }
                    }
                    pathOfFileTrigeredSetLibraryFolderRootDialog = null;
                }
                // @Override
                // protected void onPostExecute(Boolean succees) {
                    
                // }
            };

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_analyse_library_root_title))
            .setMessage(getString(R.string.dialog_analyse_library_root_message))
            .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() 
                {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if(analyseLibraryFolderRootTask != null) 
                        {
                            analyseLibraryFolderRootTask.cancel(false);
                            analyseLibraryFolderRootTask = null;
                        }
                    }
                })
            .setOnCancelListener(new DialogInterface.OnCancelListener()
                {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if(analyseLibraryFolderRootTask != null) 
                        {
                            analyseLibraryFolderRootTask.cancel(false);
                            analyseLibraryFolderRootTask = null;
                        }
                    }
                });
        analysingLibraryFolderRootDialog = alertDialogBuilder.show();
        analyseLibraryFolderRootTask.execute(pathOfFileTrigeredSetLibraryFolderRootDialog);
    }

    
    Uri getUriInLibraryFolder(String path)
    {
        path = convertToLinuxLikePath(path);
            //Some versions of Android suffer from this very stupid bug:
            //http://stackoverflow.com/questions/16475317/android-bug-string-substring5-replace-empty-string
        if(uriPrefixString == null || path == null)
            return null;
        else
            return Uri.parse(uriPrefixString + Uri.encode((uriTargetString == null || uriTargetString.equals("")) ? path : path.replaceFirst(Pattern.quote(uriTargetString), uriReplacementString)));
    }

    
    private void loadGlobalSettings()
    {
        SharedPreferences globalSettings = getSharedPreferences(GLOBAL_SETTINGS, MODE_PRIVATE);
        libraryWasPreviouslyInitializedCorrectly = globalSettings.getBoolean("libraryPreviouslyInitialized", libraryWasPreviouslyInitializedCorrectly);
        libraryPathString = globalSettings.getString("bibtexUrlString", libraryPathString);
        pathTargetString = globalSettings.getString("pathTargetString", pathTargetString);
        pathReplacementString = globalSettings.getString("pathReplacementString", pathReplacementString);
        pathPrefixString = globalSettings.getString("pathPrefixString", pathPrefixString);
        uriTargetString = globalSettings.getString("uriTargetString", uriTargetString);
        uriReplacementString = globalSettings.getString("uriReplacementString", uriReplacementString);
        uriPrefixString = globalSettings.getString("uriPrefixString", uriPrefixString);
        sortMode = BibtexAdapter.SortMode.valueOf(globalSettings.getString("sortMode", "None"));
        String libraryFolderRootUriString = globalSettings.getString("bibtexFolderRootUri", libraryFolderRootUri != null ? libraryFolderRootUri.toString() : "");
        if(libraryFolderRootUriString == null || libraryFolderRootUriString.equals("null"))
            libraryFolderRootUri = null;
        else
            libraryFolderRootUri = Uri.parse(libraryFolderRootUriString);
    }

    
    private void prepareBibtexListView()
    {
        bibtexListView = (ListView) findViewById(R.id.bibtex_list_view);
    }


    private void prepareBibtexAdapter()
    {
            //If we already have an adapter, throw it away if if the file has changed since we last opend it
        if(bibtexAdapter != null)
        {
            SharedPreferences globalSettings = getSharedPreferences(GLOBAL_SETTINGS, MODE_PRIVATE);
            long lastModifyDate = globalSettings.getLong("libraryFileLastModifyDate", 0);
            if(libraryPathString != null)
            {
                Uri libraryUri = Uri.parse(libraryPathString);
                if(libraryUri != null)
                {
                    File libraryFile = new File(Uri.decode(libraryUri.getEncodedPath()));
                    if(libraryFile != null)
                    {
                        SharedPreferences.Editor globalSettingsEditor = globalSettings.edit();
                        globalSettingsEditor.putLong("libraryFileLastModifyDate", libraryFile.lastModified());
                        globalSettingsEditor.apply();
                        if(libraryFile.lastModified() != lastModifyDate)
                            bibtexAdapter = null;
                    }
                }
            }
        }
        
        if(prepareBibtexAdapterTask != null)
            prepareBibtexAdapterTask.cancel(true);
        
        prepareBibtexAdapterTask = new AsyncTask<String, Void, Void>() {
            @Override
            protected void onPreExecute()
            {
                if(bibtexListView != null)
                    bibtexListView.setVisibility(View.GONE);
                if(progressBar != null)
                    progressBar.setVisibility(View.VISIBLE);
            }
            @Override
            protected Void doInBackground(String... libraryPathString) {
                if (bibtexAdapter == null)
                {
                    InputStream inputStream = null;
                    try
                    {
                        Uri libraryUri = Uri.parse(libraryPathString[0]);
                        File libraryFile = new File(Uri.decode(libraryUri.getEncodedPath()));
                        
                        if(libraryFile != null && libraryFile.isFile())
                        {
                            inputStream = new FileInputStream(libraryFile);
                        }
                        if(inputStream == null && libraryUri.toString().startsWith("content://"))
                        {
                            inputStream = context.getContentResolver().openInputStream(libraryUri);
                        }
                        
                        bibtexAdapter = new LibraryBibtexAdapter(context, inputStream);
                    }
                    catch(Exception e)
                    {
                        Log.e(getString(R.string.app_name), getString(R.string.exception_while_loading_library)+e.getMessage(), e);
                        bibtexAdapter = null;
                    }
                    finally{
                        prepareBibtexAdapterTask = null;
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (java.io.IOException e) {
                                    //Nothing we can do
                            }
                        }
                    }
                }
                return null;
            }
            @Override
            protected void onPostExecute(Void v) {
                if(bibtexAdapter != null){
                    libraryWasPreviouslyInitializedCorrectly = true;
                        //Bind the Adapter to the UI and update
                    bibtexListView.setAdapter(bibtexAdapter);
                    bibtexAdapter.notifyDataSetChanged();
                    bibtexAdapter.onPostBackgroundOperation();

                    filterAndSortInBackground(filter, sortMode, group);
                    bibtexAdapter.prepareForFiltering();

                    if(bibtexAdapter.getGroups().isEmpty() && menu != null){
                        menu.findItem(R.id.menu_groups).setVisible(false);
                    }
                    if(!bibtexAdapter.getGroups().isEmpty() && menu != null){
                        menu.findItem(R.id.menu_groups).setVisible(true);
                    }
                }
                else
                {
                    showSetLibraryPathDialog();
                }
            }
        };
        prepareBibtexAdapterTask.execute(libraryPathString);
    } 
        
    private void resetFilter() {
        if(bibtexAdapter!=null)
            bibtexAdapter.filterAndSortInBackground("", sortMode, group);
        filter = "";
    }
    
    private void sortInBackground(BibtexAdapter.SortMode sortMode) 
    {
        if(bibtexAdapter!=null)
            bibtexAdapter.sortInBackground(sortMode);
    }

    private void filterAndSortInBackground(String filter, BibtexAdapter.SortMode sortMode, String group)
    {
        if(bibtexAdapter!=null) 
        {
            bibtexAdapter.filterAndSortInBackground(filter, sortMode, group);
        }
    }
    
    private void hideKeyboard() 
    {
        InputMethodManager inputMethodManager = (InputMethodManager)  getSystemService(INPUT_METHOD_SERVICE);
        if(inputMethodManager != null && getCurrentFocus() != null)
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case LIBRARY_FILE_PICK_REQUEST:
                if(resultCode == Activity.RESULT_OK)
                {
                    if (intent != null && intent.getData() != null) {
                        if(setLibraryPathDialog!=null)
                        {
                            setLibraryPathDialog.dismiss();
                            setLibraryPathDialog = null;
                        }
                        setLibraryPath(intent.getData().toString());

                        if (android.os.Build.VERSION.SDK_INT >= 19)
                        {
                            try
                            {
                                getContentResolver().takePersistableUriPermission(intent.getData(), Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            }
                            catch(Exception e)
                            {
                                    //Nothing we can do if we don't get the permission
                            }
                        }
                        
                        bibtexAdapter = null;
                        prepareBibtexAdapter();
                    }
                    if (android.os.Build.VERSION.SDK_INT >= 21 && (android.support.v4.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || android.support.v4.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) )
                    {
                        requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_PERMISSION_REQUEST);
                    }
                }
                break;
            case SET_LIBRARY_FOLDER_ROOT_REQUEST:
                if(resultCode==Activity.RESULT_OK) 
                {
                    Uri treeUri=intent.getData();
                    grantUriPermission(getPackageName(), treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);//Not sure this is necessary
                    getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION );
                    analyseLibraryFolderRoot(treeUri);
                }
                break;
            case SELECT_GROUP_REQUEST:
                if(resultCode==Activity.RESULT_OK)
                {
                    group = intent.getStringExtra("group");
                    filterAndSortInBackground(filter, sortMode, group);
                    TextView group_titlebar = (TextView) findViewById(R.id.group_titlebar);

                    if (bibtexAdapter.getGroups().contains(group)) {
                        group_titlebar.setText(group);
                        group_titlebar.setVisibility(View.VISIBLE);
                    } else {
                        group_titlebar.setText("");
                        group_titlebar.setVisibility(View.GONE);
                    }
                }
        }
    }

    void setLibraryPath(String newLibraryPathString) {
        SharedPreferences globalSettings = getSharedPreferences(GLOBAL_SETTINGS, MODE_PRIVATE);
        SharedPreferences.Editor globalSettingsEditor = globalSettings.edit();
        globalSettingsEditor.putString("bibtexUrlString", newLibraryPathString);
        globalSettingsEditor.commit();
        libraryPathString = newLibraryPathString;
    }


    void setLibraryFolderRootUri(Uri newLibraryFolderRootUri) {
        SharedPreferences globalSettings = getSharedPreferences(GLOBAL_SETTINGS, MODE_PRIVATE);
        SharedPreferences.Editor globalSettingsEditor = globalSettings.edit();
        globalSettingsEditor.putString("bibtexFolderRootUri", newLibraryFolderRootUri != null ? newLibraryFolderRootUri.toString() : "null" );
        globalSettingsEditor.commit();
        libraryFolderRootUri = newLibraryFolderRootUri;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == WRITE_PERMISSION_REQUEST) {
                /*We just resume irrespective of whether the permission was
                 * granted and then handle cases where we can not access a
                 * file on a per case basis.
                 * Addendum: We should be able to simply resume here, but
                 * due to a bug in Android we have to kill the current process
                 * because we only actually get the permission after the app
                 * is restarted from scratch.
                 * */
            //onResume();
            Boolean anyResultPositive = false;
            for (int result : grantResults)
                if(result ==  android.content.pm.PackageManager.PERMISSION_GRANTED ) {
                    anyResultPositive = true;
                    break;
                }
            
            if(anyResultPositive) 
            {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                AlertDialog alert = alertDialogBuilder.create();
                alert.setTitle(R.string.dialog_newpermissions_title);
                alert.setMessage(getResources().getString(R.string.dialog_newpermissions_message));
                alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dialog_newpermissions_ok),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                });
                alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        public void onDismiss(DialogInterface dialog) {
                            android.os.Process.killProcess(android.os.Process.myPid());
                        }
                    });
                alert.show();
            }
        }
    }
    
    String convertToLinuxLikePath(String path) {
        path = path.replace("\\","/");
        if(path.indexOf(":")>=0)
            path = path.substring(path.indexOf(":")+1);
        return path;
    }
}
