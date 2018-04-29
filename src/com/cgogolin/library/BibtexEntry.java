package com.cgogolin.library;

import android.content.Context;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.regex.Pattern;

public class BibtexEntry extends BaseBibtexEntry {

    private boolean extraInfoVisible = false;
    //For performance reasons, compile regexps once:
    private static final Pattern authorCommaPattern = Pattern.compile(" *([^,]*) *,? *(.*) *");
    private static final Pattern authorAndPattern = Pattern.compile(" and ");
    public BibtexEntry()
    {
        super();
    }
    public String getFilesFormated(Context context) {
        List<String> associatedFilesList = getFiles();
        String filesString = "";
        if (associatedFilesList != null)
        {
            filesString = context.getString(R.string.files)+": ";
            for (String file : associatedFilesList)
            {
                filesString = filesString+file+" ";
            }
        }
        return filesString.trim();
    }
        public List<String> getUrls(Context context) {
        String url = getUrl();
        if ( url.equals("") )
            url=getHowpublished();
        String eprint = getArxivId().equals("") ? getEprint() : getArxivId();
        if ( url.equals("") && eprint.equals("") ) return null;
        List<String> urls = new ArrayList<String>();
        if ( !url.equals("") ) urls.add(url);
        if ( !eprint.equals("") )
        {
            eprint = context.getString(R.string.arxiv_url_prefix)+eprint;
            if (!eprint.equals(url) )
                urls.add(eprint);
        }
        return urls;
    }
    public List<String> getDoiLinks(Context context) {
        String doi = getDoi();
        if( doi.equals("") ) return null;
        List<String> dois = new ArrayList<String>();
        dois.add(context.getString(R.string.doi_url_prefix)+doi);
        return dois;
    }
    public String getDoiFormated(Context context) {
        if(!getDoi().equals(""))
            return context.getString(R.string.doi)+": "+getDoi();
        else
            return "";
    }
    public String getAuthorsFormated(Context context) {
        //use precompiled regex
        String[] authors = authorAndPattern.split(getAuthor());
        String[] editors = authorAndPattern.split(getEditor());
        String authorsString = "";
        boolean firstAuthor = true;
        for (String author : authors)
        {
            if (author.trim().equals("")) break;
            if( !firstAuthor )
                authorsString +=", ";
            else
                firstAuthor=false;
            //apply regex twice to take care of "Last, Jr ,First" cases
            //use precompiled regex
            authorsString += authorCommaPattern.matcher(authorCommaPattern.matcher(author.trim()).replaceAll("$2 $1").trim()).replaceAll("$2 $1").trim();
        }
        boolean firstEditor = true;
        for (String author : editors)
        {
            if (author.trim().equals("")) break;
            if( !firstEditor )
                authorsString +=", ";
            else
            {
                authorsString += " "+context.getString(R.string.edited_by)+" ";
                firstEditor=false;
            }
            //apply regex twice to take care of "Last, Jr ,First" cases
            //use precompiled regex
            authorsString += authorCommaPattern.matcher(authorCommaPattern.matcher(author.trim()).replaceAll("$2 $1").trim()).replaceAll("$2 $1").trim();
        }
        return authorsString.trim();
    }
    public String getJournalFormated(Context context) {
        String jounnal = getJournal();
        if(!getVolume().equals(""))
            jounnal += " "+context.getString(R.string.vol)+" "+getVolume();
        if(!getNumber().equals(""))
            jounnal += " "+context.getString(R.string.num)+" "+getNumber();
        if(!getPages().equals(""))
            jounnal += " "+context.getString(R.string.page)+" "+getPages();
        if(!getYear().equals("") || !getMonth().equals("")) {
            jounnal += " (";
            if(!getMonth().equals(""))
                jounnal += getMonth()+" ";
            jounnal += getYear()+")";
        }
        if(!jounnal.equals(""))
            jounnal += ".";
        return jounnal;
    }
    public String getEprintFormated() {
        if(!getArxivId().equals(""))
            if(!getArchivePrefix().equals("") && !getArxivId().startsWith(getArchivePrefix()))
                return getArchivePrefix()+":"+getArxivId();
            else
                return getArxivId();
        else if(!getEprint().equals(""))
            if(!getArchivePrefix().equals("") && !getEprint().startsWith(getArchivePrefix()))
                return getArchivePrefix()+":"+getEprint();
            else
                return getEprint();
        else
            return "";
    }
    public String getDateFormated() {
        String year = getYear();
        String month = getMonthNumeric();
        String day = getDay();
        if(month.equals(""))
            return year;
        else
            if(day.equals(""))
                return year+"-"+month;
            else
                return year+"-"+month+"-"+day;
    }
    public String getNumberInFile() {
        return saveGet("numberInFile");
    }
    public boolean extraInfoVisible() {
        return extraInfoVisible;
    }
    public void setExtraInfoVisible(boolean extraInfoVisible) {
        this.extraInfoVisible = extraInfoVisible;
    }
}
