package nom.tam.fits.header.hierarch;

import nom.tam.fits.utilities.FitsLineAppender;

import java.util.Locale;

import static nom.tam.fits.header.NonStandard.HIERARCH;

@SuppressWarnings("deprecation")
public class BlanksDotHierarchKeyFormatter implements IHierarchKeyFormatter {

    private final String blanks;
    
    private boolean allowMixedCase;

    public BlanksDotHierarchKeyFormatter(int count) {
        if (count < 1) {
            throw new IllegalArgumentException("HIERARCH needs at least one blank space after it."); 
        }
        
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < count; index++) {
            builder.append(' ');
        }
        blanks = builder.toString();
    }
    
    @Override
    public void append(String key, FitsLineAppender buffer) {
        buffer.append(toHeaderString(key));
    }
    
    @Override
    public int getExtraSpaceRequired(String key) {
        // The number of blank spaces minus the one standard, and the one extra space before '='...
        return blanks.length();
    }
    

    @Override
    public String toHeaderString(String key) {
        if (!allowMixedCase) {
            key = key.toUpperCase(Locale.US);
        }
        
        // cfitsio specifies a required space before the '=', so let's play nice with it.
        return HIERARCH.key() + blanks + key.substring(HIERARCH.key().length() + 1) + " ";
    }
    
    @Override
    public void setCaseSensitive(boolean value) {
        allowMixedCase = value;
    }

    @Override
    public final boolean isCaseSensitive() {
        return allowMixedCase;
    }
}
