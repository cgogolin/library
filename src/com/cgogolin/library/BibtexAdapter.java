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

import android.content.Context;
import android.content.Intent;
import android.content.ActivityNotFoundException;

import android.net.Uri;

import android .util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.view.View.OnClickListener;

import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import android.webkit.MimeTypeMap;

public class BibtexAdapter extends BaseAdapter {
    
    public static final int STATUS_SORTING = 3;
    public static final int STATUS_FILTERING = 2;
    public static final int STATUS_NOT_INITIALIZED = 1;
    public static final int STATUS_OK = 0;
    public static final int STATUS_FILE_NOT_FOUND = -1;
    public static final int STATUS_IO_EXCEPTION = -2;
    public static final int STATUS_IO_EXCEPTION_WHILE_CLOSING = -3;
    public static final int STATUS_INPUTSTREAM_NULL = -4;

    public enum SortMode {None, Date, Author, Journal}
    
    private ArrayList<BibtexEntry> bibtexEntryList;
    private ArrayList<BibtexEntry> filteredBibtexEntryList;
    private String filter = null;
    private int status = BibtexAdapter.STATUS_NOT_INITIALIZED;

    SortMode sortMode = SortMode.None;
    
    public BibtexAdapter(InputStream inputStream)
    {
        if(inputStream == null) {    
            status = STATUS_INPUTSTREAM_NULL;
            return;
        }
        try{
            bibtexEntryList = BibtexParser.parse(inputStream);
        }
        catch (java.io.IOException e) {
            status = STATUS_IO_EXCEPTION;
            return;
        }
        finally{
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (java.io.IOException e) {
                    status = STATUS_IO_EXCEPTION_WHILE_CLOSING;
                }
            }
        }
            //Copy all entries to the filtered list
        filteredBibtexEntryList = new ArrayList<BibtexEntry>();
        filteredBibtexEntryList.addAll(bibtexEntryList);
        
        status = STATUS_OK;
    }

    public void applyFilter(String filter)
    {
            //If not successfully initialized or filter invalid or unchanged do nothing
        if (getStatus() != BibtexAdapter.STATUS_OK || filter == null || filter.equals(this.filter)) return;
            //Else start filtering
        status = STATUS_FILTERING;
            //Clear so that we can populate from scratch
        filteredBibtexEntryList.clear();
            //If filter is empty we return everything
        if (filter.trim().equals(""))
        {
            filteredBibtexEntryList.addAll(bibtexEntryList);
        }
            //Else we filter with "and" ignoring case
        else
        {
            for ( BibtexEntry entry : bibtexEntryList ) {
                String blob = entry.getStringBlob().toLowerCase();
                String[] substrings = filter.toLowerCase().split(" ");
                boolean matches = true;
                for (String substring : substrings) 
                {
                    if ( !blob.contains(substring) ) {
                        matches = false;
                        break;
                    }
                }
                if (matches)
                    filteredBibtexEntryList.add(entry);
            }
        }
        this.filter = filter;
        status = STATUS_OK;
        sort();
    }

    public void sort(SortMode sortMode) {
        if(this.sortMode != sortMode)
        {
            int oldStatus = status;
            this.sortMode = sortMode;
            sort();
            status = oldStatus;
        }
    }
    
    private void sort() {
        if(filteredBibtexEntryList==null || status != STATUS_OK) return;
        status = STATUS_SORTING;
        switch(sortMode) {
            case None:
                Collections.sort(filteredBibtexEntryList, new Comparator<BibtexEntry>() {
                        @Override
                        public int compare(BibtexEntry entry1, BibtexEntry entry2) {
                            return  entry1.getNumberInFile().compareTo(entry2.getNumberInFile());
                        }
                    });
                return;
            case Date:
                Collections.sort(filteredBibtexEntryList, new Comparator<BibtexEntry>() {
                        @Override
                        public int compare(BibtexEntry entry1, BibtexEntry entry2) {
                            return  (entry2.getDateFormated()+entry2.getNumberInFile()).compareTo(entry1.getDateFormated()+entry1.getNumberInFile());
                        }
                    });
                return;                
            case Author:
                Collections.sort(filteredBibtexEntryList, new Comparator<BibtexEntry>() {
                        @Override
                        public int compare(BibtexEntry entry1, BibtexEntry entry2) {
                            return  (entry1.getAuthor()+entry1.getNumberInFile()).compareTo(entry2.getAuthor()+entry2.getNumberInFile());
                        }
                    });
                return;
            case Journal:
                Collections.sort(filteredBibtexEntryList, new Comparator<BibtexEntry>() {
                        @Override
                        public int compare(BibtexEntry entry1, BibtexEntry entry2) {
                            return  (entry1.getJournal()+entry1.getNumberInFile()).compareTo(entry2.getJournal()+entry2.getNumberInFile());
                        }
                    });
                return;
        }
    }

    
    public boolean resetFilter()
    {
        if (filter == null || filter.equals(""))
            return false;
        else
            applyFilter("");
        return true;
    }


    public void prepareForFiltering() //Calls generateStringBlob() for each entry to speed up subsequent searches
    {
        for ( BibtexEntry entry : bibtexEntryList ) {
            entry.generateStringBlob();
        }
    }

    private void setTextViewAppearance(TextView textView, String text){
        if(text.equals(""))
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
                    
        BibtexEntry entry = getItem(position);
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.bibtexentry, null);
        }
        
        
        setTextViewAppearance((TextView)convertView.findViewById(R.id.bibtex_title), entry.getTitle());
        setTextViewAppearance((TextView) convertView.findViewById(R.id.bibtex_authors), entry.getAuthorsFormated(context));
        setTextViewAppearance((TextView) convertView.findViewById(R.id.bibtex_journal), entry.getJournalFormated(context));
        setTextViewAppearance((TextView) convertView.findViewById(R.id.bibtex_doi), entry.getDoiFormated(context));
        setTextViewAppearance((TextView) convertView.findViewById(R.id.bibtex_arxiv), entry.getEprintFormated());
//        setTextViewAppearance((TextView) convertView.findViewById(R.id.bibtex_file), entry.getFilesFormated(context));

        convertView.findViewById(R.id.LinearLayout02).setVisibility(View.GONE);
        
        convertView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    LinearLayout extraInfo = (LinearLayout)v.findViewById(R.id.LinearLayout02);
                    if(extraInfo.getVisibility() != View.VISIBLE)
                    {
                        extraInfo.removeAllViews();

                        BibtexEntry entry = getItem(position);
                            //Add views here!!!                    



                            //Read the Files list from the BibtexEntry
                        List<String> associatedFilesList = entry.getFiles();
                        if (associatedFilesList != null)
                        {
                            for (String file : associatedFilesList)
                            {
                                final String url = getModifiedPath(file);//Path replacement can be done by overriding getModifiedPath()
                            
                                if ( url == null || url.equals("") ) continue;
                            
                                final Button button = new Button(context);
                                button.setText(context.getString(R.string.file)+": "+url);
                                button.setOnClickListener(new OnClickListener() {
                                        @Override
                                        public void onClick(View v)
                                            {
                                                Uri uri = Uri.parse("file://"+url); // Some PDF viewers seem to need this to open the file properly
                                                if( uri != null && (new File(uri.getPath())).isFile() ) 
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
                                                        context.startActivity(intent);
                                                    }
                                                    catch (ActivityNotFoundException e) 
                                                    {
                                                        Toast.makeText(context, context.getString(R.string.no_application_to_view_files_of_type)+" "+type+".",Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                                else
                                                {
                                                    Toast.makeText(context, context.getString(R.string.couldnt_find_file)+" "+url+".\n\n"+context.getString(R.string.path_conversion_hint),Toast.LENGTH_LONG).show();    
                                                }
                                            }
                                    });
                                extraInfo.addView(button);
                            }
                        }


                            //Read from the URLs list from the BibtexEntry
                        List<String> associatedUrlList = entry.getUrls(context);
                        if (associatedUrlList != null)
                        {
                            for (final String url : associatedUrlList)
                            {
                                if ( url == null || url.equals("") ) continue;
                            
                                final Button button = new Button(context);
                                button.setText(context.getString(R.string.url)+": "+url);
                                button.setOnClickListener(new OnClickListener() {
                                        @Override
                                        public void onClick(View v)
                                            {
                            
                                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                                intent.setData(Uri.parse(url));
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
    

                            //Read from the DOIs list from the BibtexEntry
                        List<String> associatedDoiList = entry.getDoiLinks(context);
                        if (associatedDoiList != null)
                        {
                            for (final String doi : associatedDoiList)
                            {
                                if ( doi == null || doi.equals("") ) continue;
                            
                                final Button button = new Button(context);
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
//                    final String entryString = getEntryAsString(position);
                        final String entryString = entry.getEntryAsString();
                        final Button button = new Button(context);
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
                    } else {
                        extraInfo.setVisibility(View.GONE);
                    }
                }}
            );
        return convertView;
    }

    @Override
    public int getCount() {
        return filteredBibtexEntryList.size();
    }

    @Override
    public BibtexEntry getItem(int position) {
        return filteredBibtexEntryList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
    
    // public String getEntryAsString(int position)
    // {
    //     return getEntryAsString(getItem(position));
    // }
    // public String getEntryAsString(BibtexEntry entry) {
    //     return entry.getEntryAsString();
    // }
    
    public int getStatus()
    {
        return status;
    }

        //Is overriden in Library.java to modify the url based on the target and replacement strings
    String getModifiedPath(String path) {
        return path;
    };
    

}
