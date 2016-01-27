package com.cgogolin.library;

import java.lang.Character;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import android.content.Context;
import android.content.Intent;
import android.content.ActivityNotFoundException;

import android.net.Uri;

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

    public static final int STATUS_FILTERING = 2;
    public static final int STATUS_NOT_INITIALIZED = 1;
    public static final int STATUS_OK = 0;
    public static final int STATUS_FILE_NOT_FOUND = -1;
    public static final int STATUS_IO_EXCEPTION = -2;
    public static final int STATUS_IO_EXCEPTION_WHILE_CLOSING = -3;
    public static final int STATUS_INPUTSTREAM_NULL = -4;
    
    private ArrayList<BibtexEntry> BibtexEntryList = new ArrayList<BibtexEntry>();
    private ArrayList<BibtexEntry> FilteredBibtexEntryList = new ArrayList<BibtexEntry>();
    private String filter = null;
    private int status = BibtexAdapter.STATUS_NOT_INITIALIZED;


        //Modify the url based on the target and replacement strings taking into account that
    String getModifiedPath(String path) {
        return path;
    };
    
    public BibtexAdapter(InputStream inputStream)
    {
        if(inputStream == null) {    
            status = STATUS_INPUTSTREAM_NULL;
            return;
        }
        try {
                //Set up buffered input
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            
            String line = null;
                //int i = 1;
            Boolean eofReached = false;
            String buffer = "";

                //Go through the file  
          SEARCH_FOR_ENTRY: while (true)
            {                   
                    //If we have not yet found an '@' continue reading
                if (buffer.indexOf('@') == -1) 
                {
                        //Break if we reach the end of the file, otherwise add the line to the buffer
                    if ((line = bufferedReader.readLine()) == null) break SEARCH_FOR_ENTRY;
                    buffer = buffer+line.trim();
                }
                if (buffer.indexOf('@') == -1) continue SEARCH_FOR_ENTRY;
                
                    //Now we have an '@', so start processing the bibtex entry by 
                    //throwing away everyting before and including the first '@' and triming whitespaces
                buffer = buffer.substring(buffer.indexOf('@')+1).trim();
            
                    //Continue reading lines until we have a '{' and a ',' in the buffer so that we know
                    //we have the documenttype of the entry and the label in the buffer or we reach the end of the file
                while (buffer.indexOf('{') == -1 || buffer.indexOf(',') == -1)
                {
                    if ( (line = bufferedReader.readLine()) == null ) break SEARCH_FOR_ENTRY;
                    buffer = buffer+line.trim();
                }
                
                String documentTyp = buffer.substring(0,buffer.indexOf('{')).trim().toLowerCase();
                String label = buffer.substring(buffer.indexOf('{')+1,buffer.indexOf(',')).trim();

                    //Create a new BibtexEntry
                BibtexEntry entry = new BibtexEntry();
                entry.put("documenttyp",documentTyp);
                entry.put("label",label);
                             
                    //Discard the type and the label
                buffer = buffer.substring(buffer.indexOf(',')+1).trim();
                
                    //int j = 0;
              SEARCH_FOR_TAG: while (true)
                {
                        //If we have not yet found an '=' or a '}' continue reading
                    if ( buffer.indexOf('=') == -1 && buffer.indexOf('}') == -1 ) 
                    {
                            //Break if we reach the end of the file, otherwise add the line to the buffer
                        if ((line = bufferedReader.readLine()) == null) break SEARCH_FOR_ENTRY;
                        buffer = buffer+line.trim();
                    }
                    if ( buffer.indexOf('=') == -1 && buffer.indexOf('}') == -1 ) continue SEARCH_FOR_TAG;
                    
                        //Break if we have rached the end of the entry, i.e. there is a '}' before the next '=' 
                    if ( buffer.indexOf('}') != -1 && ( buffer.indexOf('=') == -1 || buffer.indexOf('}') < buffer.indexOf('=') ) ) {
                        
                            //Throw away the rest of this entry and break
                        if (buffer.indexOf('}')+1 < buffer.length())
                        {
                            buffer = buffer.substring(buffer.indexOf('}')+1);
                        } else {
                            buffer = "";
                        }
                            //We have found all tags of this entry so stop searching for more
                        break SEARCH_FOR_TAG;
                    }
                    
                        //Now we have an '=', so start processing the bibtex tag
                        
                        //Everything before the first '=' is the name of the tag
                    String name = buffer.substring(0,buffer.indexOf('=')).trim().toLowerCase();
                    
                        //If name contains whitespaces we have a malformed tag name and thus break and serch for a new entry
                    if (name != name.replace(" ","")) break SEARCH_FOR_TAG;
                    
                        //Now we extract the value
                    String value = "";
                        
                        //Discard the name and the '=' from the buffer
                    buffer = buffer.substring(buffer.indexOf('=')+1).trim();
                    
                        //Treat the value of the tag differentyl depending on the delimiter
                    char Delimiter1 = buffer.charAt(0);
                    
                    switch (Delimiter1)
                    {
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9': //It is a number which has no delimiter
                                //Continue reading lines until we find the first possible delimiter character
                            while (buffer.indexOf(' ') == -1 && buffer.indexOf('}') == -1 && buffer.indexOf(',') == -1 && buffer.indexOf('\"') == -1)
                            {
                                if ( (line = bufferedReader.readLine()) == null ) break SEARCH_FOR_ENTRY;
                                buffer = buffer+line.trim();
                            }
                                //Now that we have at least one non digit character in the buffer we intepret the first non interrupted sequence of numbers as the value of the tag
                            int lengthOfNumber=0;
                            while ( Character.isDigit(buffer.charAt(lengthOfNumber)) ) lengthOfNumber++;
                                //Copy the value and remove it from the buffer
                            value = buffer.substring(0,lengthOfNumber);
                            buffer = buffer.substring(lengthOfNumber).trim();
                            break;
                        case '{':
                        case '\"':
                            int lengthOfValue = 0;
                                //Determine what the closing delimiter is
                            char Delimiter2 = ( Delimiter1 == '{') ? '}' : '\"';
                                //Discard the opening delimiter and put it into value
                            buffer = buffer.substring(1);
                            value = Character.toString(Delimiter1);
                                //Find the closing delimiter of the value
                            do
                            {                                
                                    //Continue reading until we find the first unescaped closing delemiter
                                while ( buffer.replace("\\\\","").replace("\\"+Delimiter2,"").indexOf(Delimiter2) == -1 )
                                {
                                    if ( (line = bufferedReader.readLine()) == null ) break SEARCH_FOR_ENTRY;
                                    buffer = buffer+line.trim();
                                }    
                                    //Find the position of the first unescaped closing delemiter
                                lengthOfValue = buffer.replace("\\\\","__").replace("\\"+Delimiter2,"__").indexOf(Delimiter2);
                                
                                    //Copy the everything before and including the closing delimiter into value and remove it from the buffer
                                value = value+buffer.substring(0,lengthOfValue+1); //Closing delimiter is put into value
                                buffer = buffer.substring(lengthOfValue+1); //Closing delimiter not left in buffer
                                
                            } while (
                                value.replace("\\\\","__").replace("\\"+Delimiter1,"__").replace("\\"+Delimiter2,"__").replace(Character.toString(Delimiter1),"").length()
                                !=
                                value.replace("\\\\","__").replace("\\"+Delimiter1,"__").replace("\\"+Delimiter2,"__").replace(Character.toString(Delimiter2),"").length()
                                     ); //While value is not "balanced"
                            break;
                        default: //Try to be nice and also read non bibtex conform values assuming that the value consist of exactly one word
                                //Continue reading lines until we find the first possible delimiter character
                            while (buffer.indexOf(' ') == -1 && buffer.indexOf('}') == -1 && buffer.indexOf(',') == -1 && buffer.indexOf('\"') == -1)
                            {
                                if ( (line = bufferedReader.readLine()) == null ) break SEARCH_FOR_ENTRY;
                                buffer = buffer+line.trim();
                            }
                                //Now that we have at least one non letter character in the buffer we intepret the first non interrupted sequence of letters as the value of the tag
                            int lengthOfWord=0;
                            while ( Character.isLetter(buffer.charAt(lengthOfWord)) ) lengthOfWord++;
                                //Copy the value and remove it from the buffer
                            value = buffer.substring(0,lengthOfWord);
                            buffer = buffer.substring(lengthOfWord).trim();
                            
                                //Remove everything until the next ',', '}' or '@' in an attempt to read the rest of the entry and file
                            while (buffer.indexOf(',') == -1 && buffer.indexOf('}') == -1 && buffer.indexOf('@') == -1 )
                            {
                                if ( (line = bufferedReader.readLine()) == null ) break SEARCH_FOR_ENTRY;
                                buffer = buffer+line.trim();
                            }
                            int cutoff = (buffer.indexOf(',') == -1) ? 0 : buffer.indexOf(',');
                            cutoff = (buffer.indexOf('}') == -1 || cutoff < buffer.indexOf('}') ) ? cutoff : buffer.indexOf('}');
                            cutoff = (buffer.indexOf('@') == -1 || cutoff < buffer.indexOf('@') ) ? cutoff : buffer.indexOf('@');
                            buffer = buffer.substring(cutoff);
                            break;
                    }
                    
                        //Discard a trailing ',' that might be left in the buffer
                    if ( buffer.length() > 0 && buffer.charAt(0) == ',' )
                        if ( buffer.length() > 1 )
                            buffer = buffer.substring(1);
                        else
                            buffer = "";
                        
                        //Trim left over delimiters from the value
                    value = trimDelimiters(value);
                        //Add the bibtex tag to the entry
                    entry.put(name, value);
                }
                
                BibtexEntryList.add(entry);
            }
            
                //Copy all entries to the filtered list
            FilteredBibtexEntryList.addAll(BibtexEntryList);
            
                //Close the file
            bufferedReader.close();
            inputStreamReader.close();
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (java.io.IOException e) {
                    status = STATUS_IO_EXCEPTION_WHILE_CLOSING;
                    return;
                }
            }
            status = STATUS_OK;
        }
        catch (java.io.IOException e) {
            status = STATUS_IO_EXCEPTION;
            return;
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (java.io.IOException e) {
                    status = STATUS_IO_EXCEPTION_WHILE_CLOSING;
                }
            }   
        }            
    }
    
    public void applyFilter(String filter)
    {
            //If not successfully initialized or filter invalid or unchanged do nothing
        if (getStatus() != BibtexAdapter.STATUS_OK || filter == null || filter.equals(this.filter)) return;
            //Else start filtering
        status = STATUS_FILTERING;
            //Clear so that we can populate from scratch
        FilteredBibtexEntryList.clear();
            //If filter is empty we return everything
        if (filter.trim().equals(""))
        {
            FilteredBibtexEntryList.addAll(BibtexEntryList);
        }
            //Else we filter with "and" ignoring case
        else
        {
            for ( BibtexEntry entry : BibtexEntryList ) {
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
                if (matches) FilteredBibtexEntryList.add(entry);
            }
        }
        this.filter = filter;
        status = STATUS_OK;
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
        for ( BibtexEntry entry : BibtexEntryList ) {
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
        
        BibtexEntry entry = getItem(position);
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
                            
                                //Add an item to the menu and
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
                    final String entryString = getEntryAsString(position);
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
                }
            });
        return convertView;
    }

    @Override
    public int getCount() {
        return FilteredBibtexEntryList.size();
    }

    @Override
    public BibtexEntry getItem(int position) {
        return FilteredBibtexEntryList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
    
    public String getEntryAsString(int position)
    {
        return getEntryAsString(getItem(position));
    }
    public String getEntryAsString(BibtexEntry entry) {
        return entry.getEntryAsString();
    }
    
    public int getStatus()
    {
        return status;
    }

    private String trimDelimiters(String string)
    {
        String s = string;
        String t = s.replace("\\\\","").replace("\\\"","").replace("\\{","").replace("\\}","");
        if ( t.length() >= 2 && ( (t.startsWith("{") && t.endsWith("}") && s.startsWith("{") && s.endsWith("}")) || (t.startsWith("\"") && t.endsWith("\"") && s.startsWith("\"") && s.endsWith("\"")) ) )
        {
            s = s.substring(1,s.length()-1);
            t = t.substring(1,t.length()-1);
        }
        return s;
    }
}
