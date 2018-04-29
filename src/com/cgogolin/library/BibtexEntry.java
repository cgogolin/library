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
    private void generateStringBlob()
    {
        String blob = "";
        for (String key : new String[]{"label","documenttyp","author","editor","eprint","primaryclass","doi","journal","number","pages","title","volume","month","year","archiveprefix","arxivid","keywords","mendeley-tags","url"} )
        {
            if (entryMap.containsKey(key))
                blob = blob+""+key+"="+entryMap.get(key)+" ";
        }
        blob=blob+" "+latexPrettyPrinter.parse(blob);
        latexPrettyPrinterEntryMap.put("stringblob",blob);
    }
    public synchronized String getStringBlob() {
        if (!latexPrettyPrinterEntryMap.containsKey("stringblob")) generateStringBlob();
        return latexPrettyPrinterEntryMap.get("stringblob");
    }
    public String generateAuthorSortKey() {
        String[] authors = getRawAuthor().split(" and ");
        String rawAuthorString = authors[0];

                // Based on explanations from Tame the BeaST: http://tug.ctan.org/info/bibtex/tamethebeast/ttb_en.pdf
        if (rawAuthorString.isEmpty()) return "";

        String[] authorParts = new String[] {"", "", "", ""};
        final int FIRST = 0; //a few constants
        final int VON = 1;
        final int LAST = 2;
        final int JR = 3;

        boolean word = false;
        int depth = 0; //brace depth
        int specialChar = 0; //brace depth inside special character

        // indices of commas in brace depth 0 (up to two commas)
        List<Integer> commaInd = new ArrayList<Integer>();
        // indices of spaces in brace depth 0
        List<Integer> spaceInd = new ArrayList<Integer>();
        // indices of the first letter of words
        List<Integer> newWordInd = new ArrayList<Integer>();

        int[] depthArray = new int[rawAuthorString.length()]; // brace depth of each character
        int[] specialCharArray = new int[rawAuthorString.length()]; // special-charachter brace depth
        MAP_CHARS_TAG:
        for (int i = 0; i < rawAuthorString.length(); i++) {
            switch (rawAuthorString.charAt(i)) {
                case '{': // either deeper brace depth of deeper special-char depth
                    if (specialChar > 0)
                        specialChar++;
                    else if (depth == 0 && i + 1 < rawAuthorString.length() && rawAuthorString.charAt(i + 1) == '\\')
                        specialChar = 1;
                    else
                        depth++;
                    break;
                case ',': // add to comma list only if depth 0
                    if (depth == 0 && specialChar == 0) {
                        commaInd.add(i);
                        if (commaInd.size() == 2)
                            break MAP_CHARS_TAG; // don't care what happens after 2 commas
                    }
                    break;
                case '}':
                    if (specialChar > 0)
                        specialChar--;
                    else {
                        depth--;
                        if (depth < 0) return ""; // throw an error? log warning?
                    }
                    break;
                case ' ': // add to space list only if depth zero, and before first comma
                //case '-': // count as space
                //case '~': // count as space
                    if (depth == 0 && specialChar == 0 && commaInd.size() == 0) {
                        spaceInd.add(i);
                        word = false;
                    }
                    break;
                default:
                    // look for first letter of words
                    if (!word && Character.isLetter(rawAuthorString.charAt(i)) && commaInd.size() == 0) {
                        newWordInd.add(i);
                        word = true;
                    }
            }
            specialCharArray[i] = specialChar;
            if (specialChar == 0)
                depthArray[i] = depth;
            else //inside special character depth always zero
                depthArray[i] = 0;
        }

        int part = -1;
        int firstStart = 0;
        int firstEnd = 0;
        int vonStart = 0;
        int vonEnd = 0;
        int lastStart = 0;
        int lastEnd = 0;
        int jrStart = 0;
        int jrEnd = 0;

        switch (commaInd.size()) {
            case 2:
                firstStart = commaInd.get(1) + 1;
                firstEnd = rawAuthorString.length();
                jrStart = commaInd.get(0) + 1;
                jrEnd = commaInd.get(1);
                lastEnd = commaInd.get(0) - 1;
                part = LAST;
                break;
            case 1:
                firstStart = commaInd.get(0) + 1;
                firstEnd = rawAuthorString.length();
                lastEnd = commaInd.get(0) - 1;
                part = LAST;
                break;
            case 0:
                lastEnd = rawAuthorString.length();
                break;
        }
        char thisChar = '\0';
        int wordInd = 0;
        for (int i = 0; i < newWordInd.size() - 1; i++) {
            wordInd = newWordInd.get(i);

            thisChar = rawAuthorString.charAt(wordInd);
            depth = depthArray[wordInd];
            if (depth == 0) {
                if (Character.isUpperCase(thisChar)) {
                    switch (part) {
                        case -1:
                        case FIRST:
                            part = FIRST;
                            break;
                        case VON:
                            vonEnd = spaceInd.get(i > 0 ? i - 1 : 0);
                        case LAST:
                            part = LAST;
                            break;
                    }
                } else {
                    switch (part) {
                        case -1:
                            vonEnd = spaceInd.get(i);
                            part = VON;
                            break;
                        case FIRST:
                            firstEnd = spaceInd.get(i - 1);
                            vonStart = spaceInd.get(i - 1);
                            vonEnd = spaceInd.get(i);
                            part = VON;
                            break;
                        case VON:
                            vonEnd = spaceInd.get(i);
                            break;
                        case LAST:
                            vonEnd = spaceInd.get(i);
                            part = VON;
                            break;
                    }
                }
            }
        }
        if (part == FIRST){
            firstEnd = newWordInd.size() > 1 ? spaceInd.get(newWordInd.size() - 2) : 0;
            lastStart = firstEnd;
        } else
            lastStart = vonEnd;

        authorParts[FIRST] = rawAuthorString.substring(firstStart, firstEnd).trim();
        authorParts[VON] = rawAuthorString.substring(vonStart, vonEnd).trim();
        authorParts[LAST] =rawAuthorString.substring(lastStart, lastEnd).trim();
        authorParts[JR] = rawAuthorString.substring(jrStart, jrEnd).trim();
        String authorSortKey = authorParts[LAST] + authorParts[FIRST] + authorParts[JR];

        authorSortKey = authorSortKey.toLowerCase();
        entry.put("authorSortKey", LatexPrettyPrinter.parse(authorSortKey));

            //In case these are ever needed individually we can save them here and get them with the methors below
        // entry.put("authorLast", authorParts[LAST]);
        // entry.put("authorFirst", authorParts[FIRST]);
        // entry.put("authorJR", authorParts[JR]);
    }
    public synchronized String getAuthorSortKey() {
        if (!entryMap.containsKey("authorSortKey")) generateAuthorSortKey();
        return saveGet("authorSortKey");
    }
    // public synchronized String getAuthorLast() {
    //     if (!entryMap.containsKey("authorLast")) generateAuthorSortKey();
    //     return saveGet("authorLast");
    // }
    // public synchronized String getAuthorFirst() {
    //     if (!entryMap.containsKey("authorFirst")) generateAuthorSortKey();
    //     return saveGet("authorFirst");
    // }
    // public synchronized String getAuthorJR() {
    //     if (!entryMap.containsKey("authorJR")) generateAuthorSortKey();
    //     return saveGet("authorJR");
    // }
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
