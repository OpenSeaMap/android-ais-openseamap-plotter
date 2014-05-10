package com.klein.activities;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import com.klein.aistcpopenmapplotter051.R;






import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;




/**
 * Demonstration of styled text resources from the com.example.android.apis.content demos. 
 */
public class ReadAssetActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // See assets/res/any/layout/styled_text.xml for this
        // view layout definition.
        setContentView(R.layout.read_asset);

        // Programmatically load text from an asset and place it into the
        // text view.  Note that the text we are loading is ASCII, so we
        // need to convert it to UTF-16.
        try {
            InputStream is = getAssets().open("hilfe.txt");
            /*
            // We guarantee that the available method returns the total
            // size of the asset...  of course, this does mean that a single
            // asset can't be more than 2 gigs.
            
            int size = is.available();
            
            // Read the entire asset into a local byte buffer.
            byte[] buffer = new byte[size];
            is.read(buffer);*/
            int size = is.available();
            String text = readIt(is,size);
            is.close();
            
            // Convert the buffer into a string.
            //String text = new String(buffer);
            
            // Finally stick the string into the text view.
            TextView tv = (TextView)findViewById(R.id.info_text);
            tv.setText(text);
        } catch (IOException e) {
            // Should never happen!
            throw new RuntimeException(e);
        }
    }
	
	// Reads an InputStream and converts it to a String.
	public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
	    Reader reader = null;
	    reader = new InputStreamReader(stream, "UTF-8");        
	    char[] buffer = new char[len];
	    reader.read(buffer);
	    return new String(buffer);
	}
}
