package jdos.win.utils;

public class Locale {
    public static final int LCMAP_LOWERCASE = 0x00000100;	/* Make lower-case */
    public static final int LCMAP_UPPERCASE = 0x00000200;	/* Make upper-case */
    public static final int LCMAP_SORTKEY = 0x00000400;	/* Create a sort key */
    public static final int LCMAP_BYTEREV = 0x00000800;	/* Reverse the result */

    public static final int SORT_STRINGSORT = 0x00001000;	/* Take punctuation into account */

    public static final int LCMAP_HIRAGANA = 0x00100000;	/* Transform Japanese katakana into hiragana */
    public static final int LCMAP_KATAKANA = 0x00200000;	/* Transform Japanese hiragana into katakana */
    public static final int LCMAP_HALFWIDTH = 0x00400000;	/* Use single byte chars in output */
    public static final int LCMAP_FULLWIDTH = 0x00800000;	/* Use double byte chars in output */

    public static final int LCMAP_LINGUISTIC_CASING = 0x01000000; /* Change case by using language context */
    public static final int LCMAP_SIMPLIFIED_CHINESE = 0x02000000; /* Transform Chinese traditional into simplified */
    public static final int LCMAP_TRADITIONAL_CHINESE = 0x04000000; /* Transform Chinese simplified into traditional */

    public static final int NORM_IGNORECASE = 0x00001;
    public static final int NORM_IGNORENONSPACE = 0x00002;
    public static final int NORM_IGNORESYMBOLS = 0x00004;
    public static final int NORM_STRINGSORT = 0x01000;
    public static final int NORM_IGNOREKANATYPE = 0x10000;
    public static final int NORM_IGNOREWIDTH = 0x20000;
}
