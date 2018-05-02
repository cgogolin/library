package com.cgogolin.library;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class BaseBibtexEntry {

    private static LatexPrettyPrinter latexPrettyPrinter;

    private HashMap<String,String> entryMap;
    private HashMap<String,String> latexPrettyPrinterEntryMap;

    public BaseBibtexEntry()
    {
        super();
        entryMap = new HashMap<String,String>();
        latexPrettyPrinterEntryMap = new HashMap<String,String>();
    }

    public void put(String name, String value) {
        entryMap.put(name, value);
    }

    protected String saveGet(String name){
        return (entryMap.containsKey(name) ? entryMap.get(name) : "");
    }

    private String saveGetPretty(String name){
        if (!entryMap.containsKey(name))
            return "";
        if (!latexPrettyPrinterEntryMap.containsKey(name))
            latexPrettyPrinterEntryMap.put(name,latexPrettyPrinter.parse(entryMap.get(name)));
        return latexPrettyPrinterEntryMap.get(name);
    }

    public String getLabel() {
        return saveGet("label");
    }
    public String getDocumentType() {
        return saveGet("documenttyp");
    }
    public String getGroup() {
        return saveGet("groups");
    }
    public List<String> getGroups(){
        //We assume the following format:
        //{group1, group2...}
        if ( getGroup().equals("") ) return null;
        String[] rawGroupString = getGroup().split(",");
        for (int i = 0; i < rawGroupString.length; i++) {
            rawGroupString[i] = rawGroupString[i].trim();
        }
        List<String> groups = new ArrayList<String>(Arrays.asList(rawGroupString));
        return groups;
    }
    public String getFile() {
        return saveGet("file");
    }
    public List<String> getFiles() {
            //We assume the either of the following formats:
            //{:path1/file1.end1:end1;:path2/file2.end1:end2;...}
            //{path1/file1.end1:end1;path2/file2.end1:end2;...}
            //{path1/file1.end1;path2/file2.end1;...}
            //{:path1\file1.end1:end1;:path2\file2.end1:end2;...}
            //{path1\file1.end1:end1;path2\file2.end1:end2;...}
            //{path1\file1.end1;path2\file2.end1;...}
            //whereby path can contains Windows drive letters such as 'c:\'.
            //Furthermore we assume that '\_' is an escape sequence for '_'.
        if ( getFile().equals("") ) return null;
        String[] rawFileString = getFile().split(";");
        for (int i = 0; i < rawFileString.length; i++) {
            int start = rawFileString[i].indexOf(':')+1;
            int end = (rawFileString[i].lastIndexOf(':') != rawFileString[i].indexOf(':')) ? rawFileString[i].lastIndexOf(':') : rawFileString[i].length();
            rawFileString[i] = rawFileString[i].substring(start,end).replace("\\_","_");
        }
        List<String> files = new ArrayList<String>(Arrays.asList(rawFileString));
        return files;
    }
    public String getUrl() {
        return saveGet("url");
    }
    public String getDoi() {
        return saveGet("doi");
    }
    public String getArchivePrefix() {
        return saveGet("archiveprefix");
    }
    public String getArxivId() {
        return saveGet("arxivid");
    }
    public String getEntryAsString() {
        String output = "@"+getDocumentType()+"{"+getLabel();
        for (String key : entryMap.keySet())
            output += ",\n"+key+" = {"+entryMap.get(key)+"}";
        output += "\n}";
        return output;
    }

        //Functions above output raw values, functions below use the LaTeX pretty printer
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
    public void generateAuthorSortKey() {
        String[] authors = getRawAuthor().split(" and ");
        String rawAuthorString = authors[0]; //We only sort according to the first authors, which should be enugh for all practical puposes

                // Based on explanations from Tame the BeaST: http://tug.ctan.org/info/bibtex/tamethebeast/ttb_en.pdf
        if (rawAuthorString.isEmpty()) return;

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
                        if (depth < 0) return; // throw an error? log warning?
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
        String authorSortKey = authorParts[LAST] + " " + authorParts[FIRST] + " " + authorParts[JR];

        authorSortKey = authorSortKey.toUpperCase();
        put("authorSortKey", LatexPrettyPrinter.parse(authorSortKey));

            //In case these are ever needed individually we can save them here and get them with the methors below (if this is activated this functino should be renamed)
        // put("authorLast", authorParts[LAST]);
        // put("authorFirst", authorParts[FIRST]);
        // put("authorJR", authorParts[JR]);
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
    public String getAuthor() {
        return saveGetPretty("author");
    }
    public String getRawAuthor() {
        return saveGet("author");
    }
    public String getEditor() {
        return saveGetPretty("editor");
    }
    public String getEprint() {
        return saveGet("eprint");
    }
    public String getPrimaryclass() {
        return saveGet("primaryclass");
    }
    public String getHowpublished() {
        return saveGet("howpublished");
    }
    public String getJournal() {
        return saveGetPretty("journal");
    }
    public String getNumber() {
        return saveGet("number");
    }
    public String getPages() {
        return saveGetPretty("pages");
    }
    public String getTitle() {
        return saveGetPretty("title");
    }
    public String getVolume() {
        return saveGet("volume");
    }
    public String getDay() {
        String day = saveGet("day");
        if(day.length() == 2)
            return day;
        else if(day.length() == 1)
            return "0"+day;
        else
            return "";
    }
    public String getMonth() {
        return saveGet("month");
    }
    public String getMonthNumeric() {
        String month = getMonth().trim().replaceAll("[^0-9a-zA-z]", "").toLowerCase();
        if(month.equals("jan")) return "01";
        if(month.equals("feb")) return "02";
        if(month.equals("mar")) return "03";
        if(month.equals("apr")) return "04";
        if(month.equals("may")) return "05";
        if(month.equals("jun")) return "06";
        if(month.equals("jul")) return "07";
        if(month.equals("aug")) return "08";
        if(month.equals("sep")) return "09";
        if(month.equals("oct")) return "10";
        if(month.equals("nov")) return "11";
        if(month.equals("dec")) return "12";
        return "";
    }
    public String getYear() {
        return saveGet("year");
    }
}
