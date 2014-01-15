package com.cgogolin.library;

import java.lang.Character;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import android.content.Context;

import android.net.Uri;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;

import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
//import android.widget.Toast;

public class BibtexAdapter extends BaseAdapter {

    public static final int STATUS_FILTERING = 2;
    public static final int STATUS_NOT_INITIALIZED = 1;
    public static final int STATUS_OK = 0;
    public static final int STATUS_FILE_NOT_FOUND = -1;
    public static final int STATUS_IO_EXCEPTION = -2;
    public static final int STATUS_IO_EXCEPTION_WHILE_CLOSING = -3;
    
    private Context context;
    private String libraryUrlString;
    private ArrayList<BibtexEntry> BibtexEntryList = new ArrayList<BibtexEntry>();
    private ArrayList<BibtexEntry> FilteredBibtexEntryList = new ArrayList<BibtexEntry>();
    private String filter = null;
    private String initialFilter = null;
    private int status = BibtexAdapter.STATUS_NOT_INITIALIZED;
    
    public BibtexAdapter(Context context, String libraryUrlString)
    {
        this.context = context;
        this.libraryUrlString = libraryUrlString;
        
            //Open the file
        FileInputStream inputStream = null;
        try {

            Uri uri = Uri.parse(libraryUrlString);
            libraryUrlString = uri.getPath();
            
            inputStream = new FileInputStream(libraryUrlString);
            
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
                    
                    //     //Continue reading lines until we find the first '}' which could be, but is not necessarially the end of the current entry.
                    // while (buffer.indexOf('}') == -1)
                    // {
                    //     if ( (line = bufferedReader.readLine()) == null ) break SEARCH_FOR_ENTRY;
                    //     buffer = buffer+line.trim();
                    // }
                    
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
                            
                                //Toast.makeText(context, "Non bibtex conform tag value in entry "+label+" intepreted as "+name+"="+value, Toast.LENGTH_SHORT).show();
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
            
                //Copy all entries to the filtered list or filter accoding to the initial filter if this has been set to something !=null in the meantime.
//            FilteredBibtexEntryList.addAll(BibtexEntryList);
            applyFilter(initialFilter);
            
                //Close the file
            bufferedReader.close();
            inputStreamReader.close();
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (java.io.IOException e) {
                        //Toast.makeText(context, "IO Exception while closing stream to "+libraryUrlString, Toast.LENGTH_LONG).show();
                    status = STATUS_IO_EXCEPTION_WHILE_CLOSING;
                    return;
                }
            }
            status = STATUS_OK;
        }
        catch (java.io.FileNotFoundException e) {
                //Toast.makeText(context, "File "+libraryUrlString+" not foud", Toast.LENGTH_LONG).show();
            status = STATUS_FILE_NOT_FOUND;
            return;
        }
        catch (java.io.IOException e) {
                //Toast.makeText(context, "IO Exception while reading from "+libraryUrlString, Toast.LENGTH_LONG).show();
            status = STATUS_IO_EXCEPTION;
            return;
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (java.io.IOException e) {
                        //Toast.makeText(context, "IO Exception while closing stream to "+libraryUrlString, Toast.LENGTH_LONG).show();
                    status = STATUS_IO_EXCEPTION_WHILE_CLOSING;
                }
            }   
        }            
    }

    public void setInitialFilter(String filter)
    {
        initialFilter = filter;
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
                
    
    public View getView(int position, View convertView, ViewGroup viewGroup)
    {
        BibtexEntry entry = getItem(position);
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.bibtexentry, null);
        }
        
        TextView bibtexTitleTextView = (TextView) convertView.findViewById(R.id.bibtex_title);
        bibtexTitleTextView.setText(entry.getTitle());

        TextView bibtexAuthorsTextView = (TextView) convertView.findViewById(R.id.bibtex_authors);
//        bibtexAuthorsTextView.setText((entry.getAuthor()+" "+entry.getEditor()).trim());
        String[] authors = (entry.getAuthor()+" and "+entry.getEditor()).split("and");
        String authorsString = "";
        boolean firstAuthor = true;
        for (String author : authors)
        {
            if (author.trim().equals("")) break;
            if( !firstAuthor )
                authorsString +=", ";
            else
                firstAuthor=false;
            authorsString += author.trim().replaceAll(" *([^,]*) *,? *(.*) *","$2 $1").trim();
        }
        bibtexAuthorsTextView.setText(authorsString.trim());
        
        TextView bibtexJournalTextView = (TextView) convertView.findViewById(R.id.bibtex_journal);
        bibtexJournalTextView.setText((
                                          entry.getJournal()+
                                          (entry.getVolume().equals("") ? "" : " "+entry.getVolume() )+
                                          (entry.getNumber().equals("") ? "" : " "+entry.getNumber() )+
                                          (entry.getPages().equals("") ? "" : " p."+entry.getPages() )+
                                          (entry.getYear().equals("") ? "" : " ("+entry.getYear()+")" )
                                       ).trim());

        TextView bibtexDoiTextView = (TextView) convertView.findViewById(R.id.bibtex_doi);
        bibtexDoiTextView.setText(entry.getDoi());

        TextView bibtexArxivTextView = (TextView) convertView.findViewById(R.id.bibtex_arxiv);
        bibtexArxivTextView.setText((
                                        ( entry.getArchivePrefix().equals("") ? "" : entry.getArchivePrefix()+":" )+
                                        ( entry.getArxivId().equals("") ? entry.getEprint() : entry.getArxivId()) 
                                     ).trim());
        
        TextView bibtexFileTextView = (TextView) convertView.findViewById(R.id.bibtex_file);
        List<String> associatedFilesList = getFiles(entry);
        String filesString = "";
        if (associatedFilesList != null)
        {
            filesString = "Files: ";
            for (String file : associatedFilesList)
            {
//                if ( file == null ) continue;
                filesString = filesString+file+" ";
            }
        }
        bibtexFileTextView.setText(filesString.trim());
            
        return convertView;
    }

    public int getCount() {
        return FilteredBibtexEntryList.size();
    }

    public BibtexEntry getItem(int position) {
        return FilteredBibtexEntryList.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public List<String> getFiles(int position)
    {
        return getFiles(getItem(position));
    }
    
    public List<String> getFiles(BibtexEntry entry) {
            //We assume the either of the following formats:
            //{:path1/file1.end1:end1;:path2/file2.end1:end2;...}
            //{path1/file1.end1:end1;path2/file2.end1:end2;...}
            //{path1/file1.end1;path2/file2.end1;...}
            //Furthermore we assume that '\_' is an escape sequence for '_'.
        if ( entry.getFile().equals("") ) return null;
        String[] rawFileString = entry.getFile().split(";");
        for (int i = 0; i < rawFileString.length; i++) { 
            int start = rawFileString[i].indexOf(':')+1;
            int end = (rawFileString[i].lastIndexOf(':') != rawFileString[i].indexOf(':')) ? rawFileString[i].lastIndexOf(':') : rawFileString[i].length();
            rawFileString[i] = rawFileString[i].substring(start,end).replace("\\_","_");
        }
        List<String> files = new ArrayList<String>(Arrays.asList(rawFileString));
        return files;
    }

    public List<String> getUrls(int position)
    {
        return getUrls(getItem(position));
    }
    public List<String> getUrls(BibtexEntry entry) {
        String url = entry.getUrl();
        String howpublished = entry.getHowpublished();
        if ( !url.equals("") && !howpublished.equals("") ) url+=howpublished;
        String eprint = entry.getArxivId().equals("") ? entry.getEprint() : entry.getArxivId();
        if ( url.equals("") && eprint.equals("") ) return null;
        List<String> urls = new ArrayList<String>();
        if ( !url.equals("") ) urls.add(url);
        if ( !eprint.equals("") )
        {
            eprint = "http://arxiv.org/abs/"+eprint;
            if (!eprint.equals(url) )
                urls.add(eprint);
        }
        return urls;
    }

    public List<String> getDois(int position)
    {
        return getDois(getItem(position));
    }
    
    public List<String> getDois(BibtexEntry entry) {
        String doi = entry.getDoi();
        if( doi.equals("") ) return null;
        List<String> dois = new ArrayList<String>();
        dois.add("http://dx.doi.org/"+doi);
        return dois;
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

    public String getLibraryUrlString()
    {
        return libraryUrlString;
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
