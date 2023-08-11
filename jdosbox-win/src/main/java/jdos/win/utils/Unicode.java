package jdos.win.utils;

public class Unicode {
    public static final int C1_UPPER   = 0x0001;
    public static final int C1_LOWER   = 0x0002;
    public static final int C1_DIGIT   = 0x0004;
    public static final int C1_SPACE   = 0x0008;
    public static final int C1_PUNCT   = 0x0010;
    public static final int C1_CNTRL   = 0x0020;
    public static final int C1_BLANK   = 0x0040;
    public static final int C1_XDIGIT  = 0x0080;
    public static final int C1_ALPHA   = 0x0100;
    public static final int C1_DEFINED = 0x0200;

    public static final int C2_NOTAPPLICABLE = 0x0000; /* unassigned */
    public static final int C2_LEFTTORIGHT = 0x0001; /* L */
    public static final int C2_RIGHTTOLEFT = 0x0002; /* R */
    public static final int C2_EUROPENUMBER = 0x0003; /* EN */
    public static final int C2_EUROPESEPARATOR = 0x0004; /* ES */
    public static final int C2_EUROPETERMINATOR = 0x0005; /* ET */
    public static final int C2_ARABICNUMBER = 0x0006; /* AN */
    public static final int C2_COMMONSEPARATOR = 0x0007; /* CS */
    public static final int C2_BLOCKSEPARATOR = 0x0008; /* B */
    public static final int C2_SEGMENTSEPARATOR = 0x0009; /* S */
    public static final int C2_WHITESPACE = 0x000A; /* WS */
    public static final int C2_OTHERNEUTRAL = 0x000B; /* ON */

    public static short get_char_directionW(char c) {
        byte direction = Character.getDirectionality((char)c);
        switch (direction) {
            case Character.DIRECTIONALITY_LEFT_TO_RIGHT:
                return C2_LEFTTORIGHT;
            case Character.DIRECTIONALITY_RIGHT_TO_LEFT:
            case Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC:
                return C2_RIGHTTOLEFT;
            case Character.DIRECTIONALITY_EUROPEAN_NUMBER:
                return C2_EUROPENUMBER;
            case Character.DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR:
                return C2_EUROPESEPARATOR;
            case Character.DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR:
                return C2_EUROPETERMINATOR;
            case Character.DIRECTIONALITY_ARABIC_NUMBER:
                return C2_ARABICNUMBER;
            case Character.DIRECTIONALITY_COMMON_NUMBER_SEPARATOR:
                return C2_COMMONSEPARATOR;
            case Character.DIRECTIONALITY_PARAGRAPH_SEPARATOR:
                return C2_BLOCKSEPARATOR;
            case Character.DIRECTIONALITY_SEGMENT_SEPARATOR:
                return C2_SEGMENTSEPARATOR;
            case Character.DIRECTIONALITY_WHITESPACE:
                return C2_WHITESPACE;
            case Character.DIRECTIONALITY_OTHER_NEUTRALS:
            case Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING:
            case Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE:
            case Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING:
            case Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE:
            case Character.DIRECTIONALITY_POP_DIRECTIONAL_FORMAT:
                return C2_OTHERNEUTRAL;
            case Character.DIRECTIONALITY_NONSPACING_MARK:
            case Character.DIRECTIONALITY_BOUNDARY_NEUTRAL:
                return C2_NOTAPPLICABLE;

        }
        return C2_NOTAPPLICABLE;
    }
    public static short get_char_typeW(char c) {
        switch (c) {
            case 0x00:
                return C1_CNTRL | C1_DEFINED;
            case 0x09: // tab
                return C1_SPACE | C1_BLANK | C1_CNTRL | C1_DEFINED;
            case 0x0A: // line feed
            case 0x0B: // line tab
            case 0x0C: // form feed
            case 0x0D: // carriage return
                return C1_SPACE | C1_CNTRL | C1_DEFINED;
            case 0x20: // space
            case 0xA2: // no break space
            case 0x3000: // ideographic space
                return C1_SPACE | C1_BLANK | C1_DEFINED;
        }
        int javaType = Character.getType(c);
        short winType = 0;
        if ((javaType & Character.UPPERCASE_LETTER) != 0) {
            winType|=C1_UPPER|C1_ALPHA;
        }
        if ((javaType & Character.LOWERCASE_LETTER) != 0) {
            winType|=C1_LOWER|C1_ALPHA;
        }
        if ((javaType & Character.DECIMAL_DIGIT_NUMBER) != 0) {
            winType|=C1_DIGIT;
        }
        if ((javaType & Character.SPACE_SEPARATOR) != 0) {
            winType|=C1_SPACE;
        }
        if ((javaType & Character.DASH_PUNCTUATION) != 0 || (javaType & Character.CONNECTOR_PUNCTUATION) != 0 || (javaType & Character.OTHER_PUNCTUATION) != 0
                 || (javaType & Character.END_PUNCTUATION) != 0 || (javaType & Character.FINAL_QUOTE_PUNCTUATION) != 0 || (javaType & Character.INITIAL_QUOTE_PUNCTUATION) !=0
                 || (javaType & Character.START_PUNCTUATION) != 0) {
            winType|=C1_PUNCT;
        }
        if ((javaType & Character.SPACE_SEPARATOR) != 0) {
            winType|=C1_SPACE;
        }
        if ((javaType & Character.MODIFIER_LETTER) != 0 || (javaType & Character.OTHER_LETTER) != 0) {
            winType|=C1_ALPHA;
        }
        if (javaType != 0) {
            winType|=C1_DEFINED;
        }
        if ((javaType & Character.LETTER_NUMBER) != 0) {
            winType|=C1_XDIGIT;
        }
        if ((javaType & Character.CONTROL) != 0) {
            winType|=C1_CNTRL;
        }
        return winType;
    }
}
