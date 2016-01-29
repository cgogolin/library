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
    public String getFile() {
        return saveGet("file");
    }
    public List<String> getFiles() {
            //We assume the either of the following formats:
            //{:path1/file1.end1:end1;:path2/file2.end1:end2;...}
            //{path1/file1.end1:end1;path2/file2.end1:end2;...}
            //{path1/file1.end1;path2/file2.end1;...}
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
//            for (String key : entryMap.keySet()) blob = blob+""+key+"="+entryMap.get(key)+" ";
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
    public String getAuthor() {
        return saveGetPretty("author");
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
        return saveGet("pages");
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
