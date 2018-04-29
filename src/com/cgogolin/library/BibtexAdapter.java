package com.cgogolin.library;

import java.lang.Character;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.OutputStream;

import android.content.Context;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager;

import android.net.Uri;

import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.View.MeasureSpec;


import android.view.View.OnClickListener;

import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

//import android.animation.LayoutTransition;
//import android.animation.ObjectAnimator;
//import android.animation.AnimatorSet;

import android.os.AsyncTask;

import android.webkit.MimeTypeMap;

import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Transformation;
import java.util.HashMap;
import java.util.Set;

import static java.util.Arrays.fill;

public class BibtexAdapter extends BaseAdapter {
    
    public enum SortMode {None, Date, Author, Journal, Title}
    
    private ArrayList<BibtexEntry> bibtexEntryList;
    private ArrayList<BibtexEntry> displayedBibtexEntryList;
    private ArrayList<BibtexEntry> bibtexGroupEntryList;
    private HashMap<String,ArrayList<BibtexEntry>> groupMap;
    private String filter = null;
    
    SortMode sortedAccordingTo = SortMode.None;
    String filteredAccodingTo = "";
    SortMode sortingAccordingTo = null;
    String filteringAccodingTo = null;
    String selectedGroup = "";
    String selectingGroup = null;

    // State of the row that needs to show separator
    private static final int SECTIONED_STATE = 1;
    // State of the row that need not show separator
    private static final int REGULAR_STATE = 2;
    // Cache row states based on positions
    private int[] mRowStates;
    private Comparator<BibtexEntry> separatorComparator = null;

    AsyncTask<Object,Void,Void> applyFilterTask;
    AsyncTask<BibtexAdapter.SortMode,Void,Void> sortTask;

    public BibtexAdapter(InputStream inputStream) throws java.io.IOException
    {
        bibtexEntryList = BibtexParser.parse(inputStream);
        bibtexGroupEntryList = bibtexEntryList;
            //Copy all entries to the filtered list
        displayedBibtexEntryList = new ArrayList<BibtexEntry>();
        displayedBibtexEntryList.addAll(bibtexEntryList);
        groupMap = new HashMap<String,ArrayList<BibtexEntry>>();
        for ( BibtexEntry entry : bibtexEntryList )
        { //populate groups hashmap
            List<String> entryGroupList = entry.getGroups();
            if ( entryGroupList != null ){
               for ( String groupName : entryGroupList ){
                   addToGroup(groupName, entry);
               }
            }
        }
        mRowStates = new int[getCount()];
    }

    public void onPreBackgroundOperation() {}
    public void onPostBackgroundOperation() {}
    public void onBackgroundOperationCanceled() {}
    public void onEntryClick(View v) {}
    public Set<String> getGroups(){ return groupMap.keySet(); }
    public void addToGroup(String groupName, BibtexEntry entry)
    {
        if (!groupMap.containsKey(groupName)) {
            groupMap.put(groupName, new ArrayList<BibtexEntry>());
        }
            groupMap.get(groupName).add(entry);
    }

    public synchronized void filterAndSortInBackground(String filter, SortMode sortMode, String group) {

        if (filter == null || (filteringAccodingTo != null && filteringAccodingTo.equals(filter) && sortingAccordingTo != null && sortMode != null && sortingAccordingTo.equals(sortMode) && selectingGroup != null && group != null && selectingGroup.equals(group)) )
            return;

        if(applyFilterTask!=null)
        {
            applyFilterTask.cancel(true);
        }
            
        applyFilterTask = new AsyncTask<Object,Void,Void>() {
                @Override
                protected void onPreExecute() {
                        onPreBackgroundOperation();
                    }
                @Override
                protected Void doInBackground(Object... params) {
                    selectingGroup = (String) params[2];
                    if(!selectedGroup.equals(selectingGroup))
                    {
                        selectGroup(selectingGroup);
                    }
                    selectingGroup = null;
                    //must set filteringAccordingTo after selecting group
                    filteringAccodingTo = (String)params[0];
                    sortingAccordingTo = (SortMode)params[1];
                    if(!filteredAccodingTo.equals(filteringAccodingTo))
                    {
                        filter(filteringAccodingTo);
                    }
                    filteringAccodingTo = null;
                    if(!sortedAccordingTo.equals(sortingAccordingTo)) 
                    {
                        sort(sortingAccordingTo);
                    }
                    sortingAccordingTo = null;
                    return null;
                }
                @Override
                protected void onPostExecute(Void v) {
                    notifyDataSetChanged();
                    onPostBackgroundOperation();
                }
            };
        applyFilterTask.execute((Object)filter, (Object)sortMode, (Object) group);
    }

    protected synchronized void selectGroup(String groupName){
        if (groupName.equals("") || !groupMap.containsKey(groupName)) {
            bibtexGroupEntryList = bibtexEntryList;
            selectedGroup = "";
        }
        else {
            bibtexGroupEntryList = groupMap.get(groupName);
            selectedGroup = groupName;
        }
        selectingGroup = null;
        filter(""); //put all group entries in the displayed list, whether or not additional filtering needed
        filteredAccodingTo = "";
        sortedAccordingTo = SortMode.None;
    }


    protected synchronized void filter(String... filter) {
        ArrayList<BibtexEntry> filteredTmpBibtexEntryList = new ArrayList<BibtexEntry>();
        if (filter[0].trim().equals(""))
        {
            filteredTmpBibtexEntryList.addAll(bibtexGroupEntryList);
        }
        else
        {
            for ( BibtexEntry entry : bibtexGroupEntryList ) {
                String blob = entry.getStringBlob().toLowerCase();
                String[] substrings = filter[0].toLowerCase().split(" ");
                boolean matches = true;
                for (String substring : substrings) 
                {
                    if ( !blob.contains(substring) ) {
                        matches = false;
                        break;
                    }
                }
                if (matches)
                    filteredTmpBibtexEntryList.add(entry);
            }
        }
        displayedBibtexEntryList = filteredTmpBibtexEntryList;
        filteringAccodingTo = null;
        filteredAccodingTo = filter[0];
        sortedAccordingTo = SortMode.None;
    }
    

    public synchronized void sortInBackground(SortMode sortMode) {
        if(sortMode == null)
            return;
        
        if(sortTask!=null)
        {
            sortTask.cancel(true);
        }

        sortTask = new AsyncTask<BibtexAdapter.SortMode,Void,Void>() {
                @Override
                protected void onPreExecute() {
                        onPreBackgroundOperation();
                    }
                @Override
                protected Void doInBackground(BibtexAdapter.SortMode... sortMode) {
                    filterAndSortInBackground(null, null, null);//Does nothing if filtering is already done, else waits until filtering is finished
                    sortingAccordingTo = (SortMode)sortMode[0];
                    if(!sortedAccordingTo.equals(sortingAccordingTo)) 
                    {
                        sort(sortingAccordingTo);
                    }
                    sortingAccordingTo = null;
                    return null;
                }
                @Override
                protected void onPostExecute(Void v) {
                    notifyDataSetChanged();
                    onPostBackgroundOperation();
                }        
            };
        sortTask.execute(sortMode);
    }

    
    protected synchronized void sort(SortMode sortMode) {
        if(sortMode == null) return;
        
        switch(sortMode) {
            case None:
                Collections.sort(displayedBibtexEntryList, new Comparator<BibtexEntry>() {
                        @Override
                        public int compare(BibtexEntry entry1, BibtexEntry entry2) {
                            return  entry1.getNumberInFile().compareTo(entry2.getNumberInFile());
                        }
                    });
                separatorComparator = null;
                break;
            case Date:
                Collections.sort(displayedBibtexEntryList, new Comparator<BibtexEntry>() {
                        @Override
                        public int compare(BibtexEntry entry1, BibtexEntry entry2) {
                            return  (entry2.getDateFormated()+entry2.getNumberInFile()).compareTo(entry1.getDateFormated()+entry1.getNumberInFile());
                        }
                    });
                separatorComparator = new Comparator<BibtexEntry>() {
                    @Override
                    public int compare(BibtexEntry entry1, BibtexEntry entry2) {
                        return (entry2.getYear().compareTo(entry1.getYear()));
                    }
                    };
                break;
            case Author:
                Collections.sort(displayedBibtexEntryList, new Comparator<BibtexEntry>() {
                        @Override
                        public int compare(BibtexEntry entry1, BibtexEntry entry2) {
                            return  (entry1.getAuthorSortKey()+entry1.getNumberInFile()).compareTo(entry2.getAuthorSortKey()+entry2.getNumberInFile());
                        }
                    });
                separatorComparator = new Comparator<BibtexEntry>() {
                    @Override
                    public int compare(BibtexEntry entry1, BibtexEntry entry2) {
                        if(entry1.getAuthorSortKey().length() == 0 && entry1.getAuthorSortKey().length() == 0)
                            return 0;
                        else if(entry1.getAuthorSortKey().length() == 0)
                            return -1;
                        else if(entry2.getAuthorSortKey().length() == 0)
                            return 1;
                        else
                            return entry1.getAuthorSortKey().substring(0,1).compareTo(entry2.getAuthorSortKey().substring(0,1));
                    }
                };
                break;
            case Journal:
                Collections.sort(displayedBibtexEntryList, new Comparator<BibtexEntry>() {
                        @Override
                        public int compare(BibtexEntry entry1, BibtexEntry entry2) {
                            return  (entry1.getJournal()+entry1.getNumberInFile()).compareTo(entry2.getJournal()+entry2.getNumberInFile());
                        }
                    });
                separatorComparator = new Comparator<BibtexEntry>() {
                    @Override
                    public int compare(BibtexEntry entry1, BibtexEntry entry2) {
                        return entry1.getJournal().toLowerCase().compareTo(entry2.getJournal().toLowerCase());
                    }
                };
                break;
            case Title:
                Collections.sort(displayedBibtexEntryList, new Comparator<BibtexEntry>() {
                        @Override
                        public int compare(BibtexEntry entry1, BibtexEntry entry2) {
                            return  (entry1.getTitle()+entry1.getNumberInFile()).compareTo(entry2.getTitle()+entry2.getNumberInFile());
                        }
                    });
                separatorComparator = new Comparator<BibtexEntry>() {
                    @Override
                    public int compare(BibtexEntry entry1, BibtexEntry entry2) {
                        if(entry1.getTitle().length() == 0 && entry1.getTitle().length() == 0)
                            return 0;
                        else if(entry1.getTitle().length() == 0)
                            return -1;
                        else if(entry2.getTitle().length() == 0)
                            return 1;
                        else                  
                            return entry1.getTitle().substring(0,1).compareTo(entry2.getTitle().substring(0,1));
                    }
                };

        }
        sortingAccordingTo = null;
        sortedAccordingTo = sortMode;
        fill(mRowStates,0);
    }

    public synchronized void prepareForFiltering()
    {
        AsyncTask<Void, Void, Void> PrepareBibtexAdapterForFilteringTask = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... v) {
                    if(bibtexEntryList != null)
                        for ( BibtexEntry entry : bibtexEntryList ) {
                            entry.getStringBlob();
                        }
                    return null;
                }
            };
        PrepareBibtexAdapterForFilteringTask.execute();
    }

    private void setTextViewAppearance(TextView textView, String text){
        if(text==null || text.equals(""))
            textView.setVisibility(View.GONE);
        else
        {       
            textView.setText(text);
            textView.setVisibility(View.VISIBLE);
        }
    }

   @Override
    public View getView(final int position, View convertView, ViewGroup parent)
    {
        final Context context = parent.getContext();
        final LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        boolean showSeparator = false;
        String separatorText ="";

        BibtexEntry entry = getItem(position);


        if (convertView == null) {
            convertView = inflater.inflate(R.layout.bibtexentry, null);
        }

        if(displayedBibtexEntryList == null || displayedBibtexEntryList.size() == 0) {
            setTextViewAppearance((TextView)convertView.findViewById(R.id.separator), "");
            setTextViewAppearance((TextView)convertView.findViewById(R.id.bibtex_info), context.getString(R.string.no_matches));
            setTextViewAppearance((TextView)convertView.findViewById(R.id.bibtex_title), "");
            setTextViewAppearance((TextView)convertView.findViewById(R.id.bibtex_authors), "");
            setTextViewAppearance((TextView)convertView.findViewById(R.id.bibtex_journal), "");
        }
        else
        {
            if (separatorComparator != null) {
                switch (mRowStates[position]) {
                    case SECTIONED_STATE:
                        showSeparator = true;
                        break;

                    case REGULAR_STATE:
                        showSeparator = false;
                        break;

                    default:
                        if (position == 0) {
                            showSeparator = true;
                        } else {
                            BibtexEntry prevEntry = getItem(position - 1);
                            if (separatorComparator.compare(entry, prevEntry) != 0){
                                showSeparator = true;
                            }
                        }
                        mRowStates[position] = showSeparator ? SECTIONED_STATE : REGULAR_STATE;
                        break;
                }
                if (showSeparator) {
                    switch (sortedAccordingTo) {
                        case Date:
                            separatorText = entry.getYear();
                            break;
                        case Author:
                            if(entry.getAuthorSortKey().length()>0)
                                separatorText = entry.getAuthorSortKey().substring(0, 1);
                            else
                                separatorText = "";
                            break;
                        case Journal:
                            separatorText = entry.getJournal();
                            break;
                        case Title:
                            if(entry.getTitle().length()>0)
                                separatorText = entry.getTitle().substring(0, 1);
                            else
                                separatorText = "";
                            break;
                    }
                }
            }
            setTextViewAppearance((TextView)convertView.findViewById(R.id.separator), separatorText);
            setTextViewAppearance((TextView)convertView.findViewById(R.id.bibtex_info), "");
            setTextViewAppearance((TextView)convertView.findViewById(R.id.bibtex_title), entry.getTitle());
            setTextViewAppearance((TextView)convertView.findViewById(R.id.bibtex_authors), entry.getAuthorsFormated(context));
            setTextViewAppearance((TextView)convertView.findViewById(R.id.bibtex_journal), entry.getJournalFormated(context));

            if(entry.extraInfoVisible())
                makeExtraInfoVisible(position, convertView, context, false);
            else
                makeExtraInfoInvisible(position, convertView, false);
            
            convertView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onEntryClick(v);
                        LinearLayout extraInfo = (LinearLayout)v.findViewById(R.id.LinearLayout02);
                        if(extraInfo.getVisibility() != View.VISIBLE)
                        {
                            makeExtraInfoVisible(position, v, context, true);
                        } else {
                            makeExtraInfoInvisible(position, v, true);
                        }
                    }
                }
                );
        }
        return convertView;
    }

    @Override
    public int getCount() {
        if(displayedBibtexEntryList == null || displayedBibtexEntryList.size() == 0)
            return 1;
        else
            return displayedBibtexEntryList.size();
    }

    @Override
    public BibtexEntry getItem(int position) {
        if(displayedBibtexEntryList == null || displayedBibtexEntryList.size() == 0) 
            return null;
        else
            return displayedBibtexEntryList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }    

        //Must be overwritten to create a Uri suitable for the respective Android version 
    Uri getUriForActionViewIntent(String path) {
        return Uri.parse("file://"+path);
    }

        //Can be overriden to modify the path for opening files
    String getModifiedPath(String path) {
        return path;
    };

    private void makeExtraInfoVisible(final int position, View v, final Context context, boolean animate) {
        final LinearLayout extraInfo = (LinearLayout)v.findViewById(R.id.LinearLayout02);
        extraInfo.removeAllViews();
        
        BibtexEntry entry = getItem(position);
        entry.setExtraInfoVisible(true);

        LinearLayout.LayoutParams buttonLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        final TextView doiTV = new TextView(context);
        setTextViewAppearance(doiTV, entry.getDoiFormated(context));
        extraInfo.addView(doiTV);
        final TextView arxivTV = new TextView(context);
        setTextViewAppearance(arxivTV, entry.getEprintFormated());
        extraInfo.addView(arxivTV);

            //Read the Files list from the BibtexEntry
        List<String> associatedFilesList = entry.getFiles();
        if (associatedFilesList != null)
        {
            for (String file : associatedFilesList)
            {
                final String path = getModifiedPath(file);//Path replacement can be done by overriding getModifiedPath()
                
                if (path == null || path.equals("")) continue;
                
                final Button button = new Button(context);
                button.setLayoutParams(buttonLayoutParams);
                button.setText(context.getString(R.string.file)+": "+path);
                button.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v)
                            {
                                Uri uri = getUriForActionViewIntent(path);
                                if(uri == null) {
                                    return;
                                }
                                
                                checkCanWriteToUri(context, uri);

                                openExternally(context, uri);
                            }
                    });
                extraInfo.addView(button);
            }
        }

            //Read the URLs list from the BibtexEntry
        List<String> associatedUrlList = entry.getUrls(context);
        if (associatedUrlList != null)
        {
            for (final String url : associatedUrlList)
            {
                if ( url == null || url.equals("") ) continue;
                                    
                final Button button = new Button(context);
                button.setLayoutParams(buttonLayoutParams);
                button.setText(context.getString(R.string.url)+": "+url);
                button.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v)
                            {
                                                    
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(url));
                                try 
                                {
                                    context.startActivity(intent);
                                }
                                catch (ActivityNotFoundException e) 
                                {
                                    Toast.makeText(context, context.getString(R.string.error_opening_webbrowser),Toast.LENGTH_SHORT).show();
                                }
                            }
                    });
                extraInfo.addView(button);
            }
        }
                            
                            
            //Read from the DOIs list from the BibtexEntry
        List<String> associatedDoiList = entry.getDoiLinks(context);
        if (associatedDoiList != null)
        {
            for (final String doi : associatedDoiList)
            {
                if ( doi == null || doi.equals("") ) continue;
                                    
                final Button button = new Button(context);
                button.setLayoutParams(buttonLayoutParams);
                button.setText(context.getString(R.string.doi)+": "+doi);
                button.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v)
                            {
                                                    
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(doi));
//                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                try 
                                {
                                    context.startActivity(intent);
                                }
                                catch (ActivityNotFoundException e) 
                                {
                                    Toast.makeText(context, context.getString(R.string.error_opening_webbrowser),Toast.LENGTH_SHORT).show();
                                }
                            }
                    });
                extraInfo.addView(button);
            }   
        }

            //Add a share button
        final String entryString = entry.getEntryAsString();
        final Button button = new Button(context);
        button.setLayoutParams(buttonLayoutParams);
        button.setText(context.getString(R.string.share));
        button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v)
                    {
                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.setType("plain/text");
                        shareIntent.setType("*/*");
                        shareIntent.putExtra(Intent.EXTRA_TEXT, entryString);
                        try 
                        {
                            context.startActivity(shareIntent);
                        }
                        catch (ActivityNotFoundException e) 
                        {
                            Toast.makeText(context, context.getString(R.string.error_starting_share_intent),Toast.LENGTH_SHORT).show();
                        }
                    }
            });
        extraInfo.addView(button);           
        extraInfo.setVisibility(View.VISIBLE);
        
        if(animate)
        {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT);
            extraInfo.measure(View.MeasureSpec.makeMeasureSpec(((LinearLayout)extraInfo.getParent()).getWidth(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));//Need to call this once so that extraInfo knows how large it wants to be
            final int bottomMargin = -extraInfo.getMeasuredHeight();
            layoutParams.setMargins(0, 0, 0, bottomMargin);
            extraInfo.setLayoutParams(layoutParams);
            Animation marginAnimation = new Animation() {
                    @Override
                    protected void applyTransformation(float interpolatedTime, Transformation t) {
                        MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)extraInfo.getLayoutParams();
                        layoutParams.setMargins(0, 0, 0, (int)((1.0-interpolatedTime)*bottomMargin));
                        extraInfo.setLayoutParams(layoutParams);
                    }
                    @Override
                    public boolean willChangeBounds() {
                        return true;
                    }
                };
            marginAnimation.setDuration(200);
            extraInfo.startAnimation(marginAnimation);
        }
    }

    private void makeExtraInfoInvisible(final int position, View v, boolean animate) {
        final LinearLayout extraInfo = (LinearLayout)v.findViewById(R.id.LinearLayout02);

        BibtexEntry entry = getItem(position);
        entry.setExtraInfoVisible(false);
            
        if(animate)
        {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT);
            final int bottomMargin = -extraInfo.getHeight();
            extraInfo.setLayoutParams(layoutParams);
            Animation marginAnimation = new Animation() {
                    @Override
                    protected void applyTransformation(float interpolatedTime, Transformation t) {
                        MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)extraInfo.getLayoutParams();
                        layoutParams.setMargins(0, 0, 0, (int)(interpolatedTime*bottomMargin));
                        extraInfo.setLayoutParams(layoutParams);
                    }
                    @Override
                    public boolean willChangeBounds() {
                        return true;
                    }
                };
            marginAnimation.setAnimationListener(new Animation.AnimationListener() 
                {
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        extraInfo.setVisibility(View.GONE);
                    }
                    @Override
                    public void onAnimationStart(Animation animation) {}
                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
            marginAnimation.setDuration(200);
            extraInfo.startAnimation(marginAnimation);
        }
        else
        {
            extraInfo.setVisibility(View.GONE);
        }
    }

    public void checkCanWriteToUri(Context context, Uri uri)
    {
        OutputStream os = null;
        Log.i(context.getString(R.string.app_name), "checking if we can somehow open an output stream to uri");
        try{
            os = context.getContentResolver().openOutputStream(uri, "wa");
            if(os != null)
            {
                Log.i(context.getString(R.string.app_name), "opened os succesfully");
                os.close();
                Log.i(context.getString(R.string.app_name), "output stream successfully opened and closed");
            }
        }
        catch(Exception e)
        {
            Log.i(context.getString(R.string.app_name), "exception while opening os: "+e);
            if(os != null)
                try
                {
                    os.close();
                }
                catch(Exception e2)
                {
                    os = null;
                }
        }
    }

    public void openExternally(Context context, Uri uri)
        {
            if(uri == null | context == null)
                return;
            
                //Determine mime type
            MimeTypeMap map = MimeTypeMap.getSingleton();
            String extension ="";
            String uriString = uri.toString();
            if (uriString.lastIndexOf(".") != -1) extension = uriString.substring((uriString.lastIndexOf(".") + 1), uriString.length());
            
            String type = map.getMimeTypeFromExtension(extension);
            
                //Start application to open the file and grant permissions
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            intent.setDataAndType(uri, type);
            try 
            {
                context.startActivity(intent);
                if(android.os.Build.VERSION.SDK_INT >= 19){
                        //Taken from http://stackoverflow.com/questions/18249007/how-to-use-support-fileprovider-for-sharing-content-to-other-apps
                    try
                    {
                        List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                        for (ResolveInfo resolveInfo : resInfoList) {
                            String packageName = resolveInfo.activityInfo.packageName;
                            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                        }
                    }
                    catch(Exception e)
                    {
                    }
                }
            }
            catch (ActivityNotFoundException e) 
            {
                Toast.makeText(context, context.getString(R.string.no_application_to_view_files_of_type)+" "+type+".",Toast.LENGTH_SHORT).show();
            }
        }
}

