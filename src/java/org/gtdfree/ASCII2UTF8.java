package org.gtdfree;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.Properties;

public class ASCII2UTF8 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			File from= new File("messages_de_DE.properties"); //$NON-NLS-1$
			File to= new File("messages_de.properties"); //$NON-NLS-1$

			Properties prop= new Properties();
			prop.load(new FileInputStream(from));
			
			System.out.println(prop.toString());
			
			PrintWriter w= new PrintWriter(to);
			
			prop.store(w, ""); //$NON-NLS-1$
			
			w.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    /*
     * Converts encoded &#92;uxxxx to unicode chars
     * and changes special saved chars to their original forms
     */
    private static String loadConvert (char[] in, int off, int len, char[] convtBuf) {
        if (convtBuf.length < len) {
            int newLen = len * 2;
            if (newLen < 0) {
            	newLen = Integer.MAX_VALUE;
            } 
            convtBuf = new char[newLen];
        }
        char aChar;
        char[] out = convtBuf; 
        int outLen = 0;
        int end = off + len;

        while (off < end) {
            aChar = in[off++];
            if (aChar == '\\') {
                aChar = in[off++];   
                if(aChar == 'u') {
                    // Read the xxxx
                    int value=0;
		    for (int i=0; i<4; i++) {
		        aChar = in[off++];  
		        switch (aChar) {
		          case '0': case '1': case '2': case '3': case '4':
		          case '5': case '6': case '7': case '8': case '9':
		             value = (value << 4) + aChar - '0';
			     break;
			  case 'a': case 'b': case 'c':
                          case 'd': case 'e': case 'f':
			     value = (value << 4) + 10 + aChar - 'a';
			     break;
			  case 'A': case 'B': case 'C':
                          case 'D': case 'E': case 'F':
			     value = (value << 4) + 10 + aChar - 'A';
			     break;
			  default:
                              throw new IllegalArgumentException(
                                           "Malformed \\uxxxx encoding."); //$NON-NLS-1$
                        }
                     }
                    out[outLen++] = (char)value;
                } else {
                    if (aChar == 't') aChar = '\t'; 
                    else if (aChar == 'r') aChar = '\r';
                    else if (aChar == 'n') aChar = '\n';
                    else if (aChar == 'f') aChar = '\f'; 
                    out[outLen++] = aChar;
                }
            } else {
	        out[outLen++] = (char)aChar;
            }
        }
        return new String (out, 0, outLen);
    }

}
