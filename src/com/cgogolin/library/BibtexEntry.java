package com.cgogolin.library;

import android.content.Context;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class BibtexEntry extends BaseBibtexEntry {

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
        String howpublished = getHowpublished();
        if ( !url.equals("") && !howpublished.equals("") ) url+=howpublished;
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
        return context.getString(R.string.doi)+": "+getDoi();
    }
        public String getAuthorsFormated(Context context) {
        String[] authors = getAuthor().split("and");
        String[] editors = getEditor().split("and");
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
            authorsString += author.trim().replaceAll(" *([^,]*) *,? *(.*) *","$2 $1").trim();
        }
        return authorsString.trim();
    }
    public String getJournalFormated(Context context) {
        return (getJournal()+
                (getVolume().equals("") ? "" : " "+context.getString(R.string.vol)+" "+getVolume() )+
         (getNumber().equals("") ? "" : " "+context.getString(R.string.num)+" "+getNumber() )+
         (getPages().equals("") ? "" : " "+context.getString(R.string.page)+" "+getPages() )+
         (getYear().equals("") ? "" : " ("+(getMonth().equals("") ? "" : getMonth()+" ")+getYear()+")" )+"."
         ).trim();
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
}
