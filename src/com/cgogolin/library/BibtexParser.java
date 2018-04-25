package com.cgogolin.library;

import java.lang.Character;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import android.util.Log;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class BibtexParser {
    public static ArrayList<BibtexEntry> parse(InputStream inputStream) throws java.io.IOException {
        ArrayList<BibtexEntry> BibtexEntryList = new ArrayList<BibtexEntry>();

            //Set up buffered input
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);


        String line = null;
        int i = 1;
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
            String label = buffer.substring(buffer.indexOf('{')+1,buffer.indexOf(',', buffer.indexOf('{'))).trim();

                //Create a new BibtexEntry
            BibtexEntry entry = new BibtexEntry();
            entry.put("numberInFile", Integer.toString(i));
            i++;
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
            String[] authors = entry.getRawAuthor().split("and");
            entry.put("authorSortKey",parseAuthor(authors[0].trim()));
            BibtexEntryList.add(entry);
        }
        bufferedReader.close();

        return BibtexEntryList;
    }
    private static String parseAuthor(String rawAuthorString) {
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
        authorSortKey = authorSortKey.toUpperCase();
        return LatexPrettyPrinter.parse(authorSortKey);

    }
    private static String trimDelimiters(String string) {
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
