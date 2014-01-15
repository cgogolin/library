package com.cgogolin.library;

import java.util.HashMap;

public class BibtexEntry {

    private static LatexPrettyPrinter latexPrettyPrinter;
    
    private HashMap<String,String> entryMap;
    private HashMap<String,String> latexPrettyPrinterEntryMap;

    public BibtexEntry()
    {
        super();
        entryMap = new HashMap<String,String>();
        latexPrettyPrinterEntryMap = new HashMap<String,String>();
    }
    
    public void put(String name, String value) {
        entryMap.put(name, value);
    }

    private String saveGet(String name){
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
    
    public void generateStringBlob()
    {
        String blob = "";
//            for (String key : entryMap.keySet()) blob = blob+""+key+"="+entryMap.get(key)+" ";
        for (String key : new String[]{"label","documenttyp","author","editor","eprint","primaryclass","doi","journal","number","pages","title","volume","month","year","archiveprefix","arxivid"} )
        {
            if (entryMap.containsKey(key))
                blob = blob+""+key+"="+entryMap.get(key)+" ";
        }
        blob=blob+" "+latexPrettyPrinter.parse(blob);
        latexPrettyPrinterEntryMap.put("stringblob",blob);
    }
    
    public String getStringBlob() {
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
        return saveGetPretty("eprint");
    }
    public String getPrimaryclass() {
        return saveGetPretty("primaryclass");
    }
    public String getHowpublished() {
        return saveGetPretty("howpublished");
    }
    public String getJournal() {
        return saveGetPretty("journal");
    }
    public String getNumber() {
        return saveGetPretty("number");
    }
    public String getPages() {
        return saveGetPretty("pages");
    }
    public String getTitle() {
        return saveGetPretty("title");
    }
    public String getVolume() {
        return saveGetPretty("volume");
    }
    public String getMonth() {
        return saveGetPretty("month");
    }
    public String getYear() {
        return saveGetPretty("year");
    }
}
