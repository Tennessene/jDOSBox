package jdos.win.builtin.user32;

import jdos.hardware.Memory;
import jdos.util.IntRef;
import jdos.win.Win;
import jdos.win.builtin.WinAPI;
import jdos.win.builtin.gdi32.*;
import jdos.win.system.WinRect;
import jdos.win.system.WinSize;
import jdos.win.system.WinSystem;
import jdos.win.utils.StringUtil;

public class WinText extends WinAPI {
    static final public int TAB = 9;
    static final public int LF = 10;
    static final public int CR = 13;
    static final public int SPACE = 32;
    static final public int PREFIX = 38;

    static final public String ELLIPSIS = "...";

    static final public char FORWARD_SLASH = '/';
    static final public char BACK_SLASH = '\\';

    // int DrawText(HDC hDC, LPCTSTR lpchText, int nCount, LPRECT lpRect, UINT uFormat)
    static public int DrawTextA(int hDC, int lpchText, int nCount, int lpRect, int flags) {
        DRAWTEXTPARAMS dtp = new DRAWTEXTPARAMS();

        if ((flags & DT_TABSTOP) != 0) {
            dtp.iTabLength = (flags >> 8) & 0xff;
            flags &= 0xffff00ff;
        }
        return DrawTextEx(hDC, lpchText, nCount, lpRect, flags, dtp);
    }

    // int DrawTextEx(HDC hdc, LPTSTR lpchText, int cchText, LPRECT lprc, UINT dwDTFormat, LPDRAWTEXTPARAMS lpDTParams)
    static public int DrawTextExA(int hdc, int lpchText, int i_count, int lpRect, int flags, int pDtp) {
        DRAWTEXTPARAMS dtp = null;

        if (pDtp != 0)
            dtp = new DRAWTEXTPARAMS(pDtp);
        int result = DrawTextEx(hdc, lpchText, i_count, lpRect, flags, dtp);
        if (dtp != null) {
            dtp.write(pDtp);
        }
        return result;
    }

    static public int DrawTextEx(int hdc, int lpchText, int i_count, int lpRect, int flags, DRAWTEXTPARAMS dtp) {
        if (lpchText == 0)
            return 0;
        String text = StringUtil.getString(lpchText, i_count);
        i_count = text.length();

        WinRect rect = new WinRect(lpRect);
        WinSize size = new WinSize();
        int strPtr = lpchText;
        int retstr;
        IntRef p_retstr = new IntRef(0);
        int size_retstr;
        final int MAX_BUFFER_SIZE = 1024;
        int line = getTempBuffer(MAX_BUFFER_SIZE);
        IntRef len = new IntRef(0);
        int lh;
        IntRef count = new IntRef(i_count);
        int lmargin = 0, rmargin = 0;
        int x = rect.left, y = rect.top;
        int width = rect.width();
        int max_width = 0;
        boolean last_line;
        int tabwidth /* to keep gcc happy */ = 0;
        IntRef prefix_offset = new IntRef(0);
        ellipsis_data ellip = new ellipsis_data();
        int invert_y = 0;

        if ((flags & DT_SINGLELINE) != 0)
            flags &= ~DT_WORDBREAK;

        int pTm = getTempBuffer(TEXTMETRIC.SIZE);
        WinFont.GetTextMetricsA(hdc, pTm);
        TEXTMETRIC tm = new TEXTMETRIC(pTm);
        if ((flags & DT_EXTERNALLEADING) != 0)
            lh = tm.tmHeight + tm.tmExternalLeading;
        else
            lh = tm.tmHeight;

        if (text.length() == 0) {
            if ((flags & DT_CALCRECT) != 0) {
                rect.right = rect.left;
                if ((flags & DT_SINGLELINE) != 0)
                    rect.bottom = rect.top + lh;
                else
                    rect.bottom = rect.top;
                rect.write(lpRect);
            }
            return lh;
        }

        if (dtp != null && dtp.cbSize != DRAWTEXTPARAMS.SIZE)
            return 0;

        if (dtp != null) {
            lmargin = dtp.iLeftMargin;
            rmargin = dtp.iRightMargin;
            if ((flags & (DT_CENTER | DT_RIGHT)) == 0)
                x += lmargin;
            dtp.uiLengthDrawn = 0;     /* This param RECEIVES number of chars processed */
        }

        if ((flags & DT_EXPANDTABS) != 0) {
            int tabstop = ((flags & DT_TABSTOP) != 0 && dtp != null) ? dtp.iTabLength : 8;
            tabwidth = tm.tmAveCharWidth * tabstop;
        }

        if ((flags & DT_CALCRECT) != 0)
            flags |= DT_NOCLIP;

        if ((flags & DT_MODIFYSTRING) != 0) {
            size_retstr = (count.value + 4);
            retstr = WinSystem.getCurrentProcess().heap.alloc(size_retstr, false);
            if (retstr == 0)
                return 0;
            Memory.mem_memcpy(retstr, lpchText, size_retstr);
        } else {
            size_retstr = 0;
            retstr = NULL;
        }
        p_retstr.value = retstr;

        do {
            len.value = MAX_BUFFER_SIZE;
            last_line = (flags & DT_NOCLIP) == 0 && (y + ((flags & DT_EDITCONTROL) != 0 ? 2 * lh - 1 : lh) > rect.bottom);
            strPtr = TEXT_NextLine(hdc, strPtr, count, line, len, width, flags, size, last_line, p_retstr, tabwidth, prefix_offset, ellip);

            if ((flags & DT_CENTER) != 0)
                x = (rect.left + rect.right - size.cx) / 2;
            else if ((flags & DT_RIGHT) != 0)
                x = rect.right - size.cx;

            if ((flags & DT_SINGLELINE) != 0) {
                if ((flags & DT_VCENTER) != 0)
                    y = rect.top + (rect.bottom - rect.top) / 2 - size.cy / 2;
                else if ((flags & DT_BOTTOM) != 0)
                    y = rect.bottom - size.cy;
            }

            if ((flags & DT_CALCRECT) == 0) {
                int str = line;
                int xseg = x;
                while (len.value != 0) {
                    int len_seg;
                    WinSize tmpSize = new WinSize();
                    if ((flags & DT_EXPANDTABS) != 0) {
                        int p = str;
                        while (p < str + len.value && readb(p) != TAB) p++;
                        len_seg = p - str;
                        int pSize = tmpSize.allocTemp();
                        if (len_seg != len.value && WinFont.GetTextExtentPointA(hdc, str, len_seg, pSize) == 0)
                            return 0;
                        tmpSize.copy(pSize);
                    } else
                        len_seg = len.value;

                    int pRect = rect.allocTemp();
                    if (WinDC.ExtTextOutA(hdc, xseg, y, ((flags & DT_NOCLIP) != 0 ? 0 : ETO_CLIPPED) | ((flags & DT_RTLREADING) != 0 ? ETO_RTLREADING : 0), pRect, str, len_seg, NULL) == 0)
                        return 0;
                    rect.copy(pRect);
                    if (prefix_offset.value != -1 && prefix_offset.value < len_seg) {
                        TEXT_DrawUnderscore(hdc, xseg, y + tm.tmAscent + 1, str, prefix_offset.value, (flags & DT_NOCLIP) != 0 ? null : rect);
                    }
                    len.value -= len_seg;
                    str += len_seg;
                    if (len.value != 0) {
                        if ((flags & DT_EXPANDTABS) == 0 || readb(str) != TAB) {
                            Win.panic("DrawTextEx bad state");
                        }
                        len.value--;
                        str++;
                        xseg += ((tmpSize.cx / tabwidth) + 1) * tabwidth;
                        if (prefix_offset.value != -1) {
                            if (prefix_offset.value < len_seg) {
                                /* We have just drawn an underscore; we ought to
                                 * figure out where the next one is.  I am going
                                 * to leave it for now until I have a better model
                                 * for the line, which will make reprefixing easier.
                                 * This is where ellip would be used.
                                 */
                                prefix_offset.value = -1;
                            } else {
                                prefix_offset.value -= len_seg;
                            }
                        }
                    }
                }
            } else if (size.cx > max_width) {
                max_width = size.cx;
            }

            y += lh;
            if (dtp != null)
                dtp.uiLengthDrawn += len.value;
        }
        while (strPtr != 0 && !last_line);

        if ((flags & DT_CALCRECT) != 0) {
            rect.right = rect.left + max_width;
            rect.bottom = y;
            if (dtp != null)
                rect.right += lmargin + rmargin;
            rect.write(lpRect);
        }
        if (retstr != 0) {
            Memory.mem_memcpy(lpchText, retstr, size_retstr);
            WinSystem.getCurrentProcess().heap.free(retstr);
        }
        return y - rect.top;
    }

    static private class ellipsis_data {
        int before;
        int len;
        int under;
        int after;
    }

    /**
     * ******************************************************************
     * Return next line of text from a string.
     * <p/>
     * hdc - handle to DC.
     * str - string to parse into lines.
     * count - length of str.
     * dest - destination in which to return line.
     * len - dest buffer size in chars on input, copied length into dest on output.
     * width - maximum width of line in pixels.
     * format - format type passed to DrawText.
     * retsize - returned size of the line in pixels.
     * last_line - TRUE if is the last line that will be processed
     * p_retstr - If DT_MODIFYSTRING this points to a cursor in the buffer in which
     * the return string is built.
     * tabwidth - The width of a tab in logical coordinates
     * pprefix_offset - Here is where we return the offset within dest of the first
     * prefixed (underlined) character.  -1 is returned if there
     * are none.  Note that there may be more; the calling code
     * will need to use TEXT_Reprefix to find any later ones.
     * pellip - Here is where we return the information about any ellipsification
     * that was carried out.  Note that if tabs are being expanded then
     * this data will correspond to the last text segment actually
     * returned in dest; by definition there would not have been any
     * ellipsification in earlier text segments of the line.
     * <p/>
     * Returns pointer to next char in str after end of the line
     * or NULL if end of str reached.
     */
    static private int TEXT_NextLine(int hdc, int str, IntRef count, int dest, IntRef len, int width, int format,
                                     WinSize retsize, boolean last_line, IntRef p_retstr, int tabwidth, IntRef pprefix_offset, ellipsis_data pellip) {
        int i = 0, j = 0;
        int plen = 0;
        WinSize size = new WinSize();
        int maxl = len.value;
        int seg_i, seg_count, seg_j;
        int max_seg_width;
        int num_fit;
        int word_broken;
        boolean line_fits;
        IntRef j_in_seg = new IntRef(0);
        int ellipsified;
        int pNumFit = getTempBuffer(4);
        int pSize = getTempBuffer(WinSize.SIZE);

        pprefix_offset.value = -1;

        /* For each text segment in the line */

        retsize.cy = 0;
        while (count.value != 0) {
            /* Skip any leading tabs */
            if (readb(str + i) == TAB && (format & DT_EXPANDTABS) != 0) {
                plen = ((plen / tabwidth) + 1) * tabwidth;
                count.value--;
                if (j < maxl)
                    writeb(dest + j++, readb(str + i++));
                else
                    i++;
                while (count.value != 0 && readb(str + i) == TAB) {
                    plen += tabwidth;
                    count.value--;
                    if (j < maxl)
                        writeb(dest + j++, readb(str + i++));
                    else
                        i++;
                }
            }


            /* Now copy as far as the next tab or cr/lf or eos */

            seg_i = i;
            seg_count = count.value;
            seg_j = j;

            while (count.value != 0 && (readb(str + i) != TAB || (format & DT_EXPANDTABS) == 0) && ((readb(str + i) != CR && readb(str + i) != LF) || (format & DT_SINGLELINE) != 0)) {
                if (readb(str + i) == PREFIX && (format & DT_NOPREFIX) == 0 && count.value > 1) {
                    count.value--;
                    i++; /* Throw away the prefix itself */
                    if (readb(str + i) == PREFIX) {
                        /* Swallow it before we see it again */
                        count.value--;
                        if (j < maxl)
                            writeb(dest + j++, readb(str + i++));
                        else
                            i++;
                    } else if (pprefix_offset.value == -1 || pprefix_offset.value >= seg_j) {
                        pprefix_offset.value = j;
                    }
                    /* else the previous prefix was in an earlier segment of the
                     * line; we will leave it to the drawing code to catch this
                     * one.
                     */
                } else {
                    count.value--;
                    if (j < maxl)
                        writeb(dest + j++, readb(str + i++));
                    else
                        i++;
                }
            }

            /* Measure the whole text segment and possibly WordBreak and
             * ellipsify it
             */

            j_in_seg.value = j - seg_j;
            max_seg_width = width - plen;
            WinFont.GetTextExtentExPointA(hdc, dest + seg_j, j_in_seg.value, max_seg_width, pNumFit, NULL, pSize);
            num_fit = readd(pNumFit);
            size.copy(pSize);

            /* The Microsoft handling of various combinations of formats is weird.
             * The following may very easily be incorrect if several formats are
             * combined, and may differ between versions (to say nothing of the
             * several bugs in the Microsoft versions).
             */
            word_broken = 0;
            line_fits = (num_fit >= j_in_seg.value);
            if (!line_fits && (format & DT_WORDBREAK) != 0) {
                IntRef s = new IntRef(0);
                IntRef chars_used = new IntRef(0);
                TEXT_WordBreak(hdc, dest + seg_j, maxl - seg_j, j_in_seg, max_seg_width, format, num_fit, chars_used, size);
                line_fits = (size.cx <= max_seg_width);
                /* and correct the counts */
                TEXT_SkipChars(count, s, seg_count, str + seg_i, i - seg_i, chars_used.value, (format & DT_NOPREFIX) == 0);
                i = s.value - str;
                word_broken = 1;
            }
            pellip.before = j_in_seg.value;
            pellip.under = 0;
            pellip.after = 0;
            pellip.len = 0;
            ellipsified = 0;
            if (!line_fits && (format & DT_PATH_ELLIPSIS) != 0) {
                TEXT_PathEllipsify(hdc, dest + seg_j, maxl - seg_j, j_in_seg, max_seg_width, size, p_retstr.value, pellip);
                line_fits = (size.cx <= max_seg_width);
                ellipsified = 1;
            }
            /* NB we may end up ellipsifying a word-broken or path_ellipsified
             * string */
            if ((!line_fits && (format & DT_WORD_ELLIPSIS) != 0) || ((format & DT_END_ELLIPSIS) != 0 && ((last_line && count.value != 0) ||
                    (remainder_is_none_or_newline(count.value, str + i) && !line_fits)))) {
                IntRef before = new IntRef(0);
                IntRef len_ellipsis = new IntRef(0);
                TEXT_Ellipsify(hdc, dest + seg_j, maxl - seg_j, j_in_seg, max_seg_width, size, p_retstr.value, before, len_ellipsis);
                if (before.value > pellip.before) {
                    /* We must have done a path ellipsis too */
                    pellip.after = before.value - pellip.before - pellip.len;
                    /* Leave the len as the length of the first ellipsis */
                } else {
                    /* If we are here after a path ellipsification it must be
                     * because even the ellipsis itself didn't fit.
                     */
                    assert (pellip.under == 0 && pellip.after == 0);
                    pellip.before = before.value;
                    pellip.len = len_ellipsis.value;
                    /* pellip->after remains as zero as does
                     * pellip->under
                     */
                }
                ellipsified = 1;
            }
            /* As an optimisation if we have ellipsified and we are expanding
             * tabs and we haven't reached the end of the line we can skip to it
             * now rather than going around the loop again.
             */
            if ((format & DT_EXPANDTABS) != 0 && ellipsified != 0) {
                if ((format & DT_SINGLELINE) != 0)
                    count.value = 0;
                else {
                    while (count.value != 0 && readb(str + i) != CR && readb(str + i) != LF) {
                        count.value--;
                        i++;
                    }
                }
            }

            j = seg_j + j_in_seg.value;
            if (pprefix_offset.value >= seg_j + pellip.before) {
                pprefix_offset.value = TEXT_Reprefix(str + seg_i, i - seg_i, pellip);
                if (pprefix_offset.value != -1)
                    pprefix_offset.value += seg_j;
            }

            plen += size.cx;
            if (size.cy > retsize.cy)
                retsize.cy = size.cy;

            if (word_broken != 0)
                break;
            else if (count.value == 0)
                break;
            else if (readb(str + i) == CR || readb(str + i) == LF) {
                count.value--;
                i++;
                if (count.value != 0 && (readb(str + i) == CR || readb(str + i) == LF) && readb(str + i) != readb(str + i - 1)) {
                    count.value--;
                    i++;
                }
                break;
            }
            /* else it was a Tab and we go around again */
        }

        retsize.cx = plen;
        len.value = j;
        if (count.value != 0)
            return str + i;
        else
            return NULL;
    }


    /**
     * ********************************************************************
     * TEXT_DrawUnderscore
     * <p/>
     * Draw the underline under the prefixed character
     * <p/>
     * Parameters
     * hdc        [in] The handle of the DC for drawing
     * x          [in] The x location of the line segment (logical coordinates)
     * y          [in] The y location of where the underscore should appear
     * (logical coordinates)
     * str        [in] The text of the line segment
     * offset     [in] The offset of the underscored character within str
     * rect       [in] Clipping rectangle (if not NULL)
     */

    static void TEXT_DrawUnderscore(int hdc, int x, int y, int str, int offset, WinRect rect) {
        int prefix_x;
        int prefix_end;
        WinSize size = new WinSize();

        int pSize = getTempBuffer(WinSize.SIZE);
        WinFont.GetTextExtentPointA(hdc, str, offset, pSize);
        size.copy(pSize);
        prefix_x = x + size.cx;
        WinFont.GetTextExtentPointA(hdc, str, offset + 1, pSize);
        size.copy(pSize);
        prefix_end = x + size.cx - 1;
        /* The above method may eventually be slightly wrong due to kerning etc. */

        /* Check for clipping */
        if (rect != null) {
            if (prefix_x > rect.right || prefix_end < rect.left || y < rect.top || y > rect.bottom)
                return; /* Completely outside */
            /* Partially outside */
            if (prefix_x < rect.left)
                prefix_x = rect.left;
            if (prefix_end > rect.right)
                prefix_end = rect.right;
        }

        int hpen = WinPen.CreatePen(PS_SOLID, 1, WinDC.GetTextColor(hdc));
        int oldPen = WinDC.SelectObject(hdc, hpen);
        PaintingGDI.MoveToEx(hdc, prefix_x, y, NULL);
        PaintingGDI.LineTo(hdc, prefix_end, y);
        WinDC.SelectObject(hdc, oldPen);
        GdiObj.DeleteObject(hpen);
    }

    /**
     * ******************************************************************
     * TEXT_Ellipsify (static)
     * <p/>
     * Add an ellipsis to the end of the given string whilst ensuring it fits.
     * <p/>
     * If the ellipsis alone doesn't fit then it will be returned anyway.
     * <p/>
     * See Also TEXT_PathEllipsify
     * <p/>
     * Arguments
     * hdc        [in] The handle to the DC that defines the font.
     * str        [in/out] The string that needs to be modified.
     * max_str    [in] The dimension of str (number of WCHAR).
     * len_str    [in/out] The number of characters in str
     * width      [in] The maximum width permitted (in logical coordinates)
     * size       [out] The dimensions of the text
     * modstr     [out] The modified form of the string, to be returned to the
     * calling program.  It is assumed that the caller has
     * made sufficient space available so we don't need to
     * know the size of the space.  This pointer may be NULL if
     * the modified string is not required.
     * len_before [out] The number of characters before the ellipsis.
     * len_ellip  [out] The number of characters in the ellipsis.
     * <p/>
     * See for example Microsoft article Q249678.
     * <p/>
     * For now we will simply use three dots rather than worrying about whether
     * the font contains an explicit ellipsis character.
     */
    static void TEXT_Ellipsify(int hdc, int str, int max_len, IntRef len_str, int width, WinSize size, int modstr, IntRef len_before, IntRef len_ellip) {
        int len_ellipsis;
        int lo, mid, hi;

        len_ellipsis = ELLIPSIS.length();
        if (len_ellipsis > max_len)
            len_ellipsis = max_len;
        if (len_str.value > max_len - len_ellipsis)
            len_str.value = max_len - len_ellipsis;

        /* First do a quick binary search to get an upper bound for *len_str. */
        int pSize = getTempBuffer(WinSize.SIZE);
        if (len_str.value > 0 && WinFont.GetTextExtentExPointA(hdc, str, len_str.value, width, NULL, NULL, pSize) != 0) {
            size.copy(pSize);
            if (size.cx > width) {
                for (lo = 0, hi = len_str.value; lo < hi; ) {
                    mid = (lo + hi) / 2;
                    if (WinFont.GetTextExtentExPointA(hdc, str, mid, width, NULL, NULL, pSize) == 0)
                        break;
                    size.copy(pSize);
                    if (size.cx > width)
                        hi = mid;
                    else
                        lo = mid + 1;
                }
                len_str.value = hi;
            }
        }
        /* Now this should take only a couple iterations at most. */
        while (true) {
            Memory.mem_memcpy(str + len_str.value, ELLIPSIS.getBytes(), 0, len_ellipsis);

            if (WinFont.GetTextExtentExPointA(hdc, str, len_str.value + len_ellipsis, width, NULL, NULL, pSize) == 0)
                break;

            if (len_str.value == 0 || size.cx <= width)
                break;

            len_str.value--;
        }

        len_ellip.value = len_ellipsis;
        len_before.value = len_str.value;
        len_str.value += len_ellipsis;

        if (modstr != 0) {
            Memory.mem_memcpy(modstr, str, len_str.value);
            writeb(modstr + len_str.value, 0);
        }
    }

    /**
     * ******************************************************************
     * TEXT_PathEllipsify (static)
     * <p/>
     * Add an ellipsis to the provided string in order to make it fit within
     * the width.  The ellipsis is added as specified for the DT_PATH_ELLIPSIS
     * flag.
     * <p/>
     * See Also TEXT_Ellipsify
     * <p/>
     * Arguments
     * hdc        [in] The handle to the DC that defines the font.
     * str        [in/out] The string that needs to be modified
     * max_str    [in] The dimension of str (number of WCHAR).
     * len_str    [in/out] The number of characters in str
     * width      [in] The maximum width permitted (in logical coordinates)
     * size       [out] The dimensions of the text
     * modstr     [out] The modified form of the string, to be returned to the
     * calling program.  It is assumed that the caller has
     * made sufficient space available so we don't need to
     * know the size of the space.  This pointer may be NULL if
     * the modified string is not required.
     * pellip     [out] The ellipsification results
     * <p/>
     * For now we will simply use three dots rather than worrying about whether
     * the font contains an explicit ellipsis character.
     * <p/>
     * The following applies, I think to Win95.  We will need to extend it for
     * Win98 which can have both path and end ellipsis at the same time (e.g.
     * C:\MyLongFileName.Txt becomes ...\MyLongFileN...)
     * <p/>
     * The resulting string consists of as much as possible of the following:
     * 1. The ellipsis itself
     * 2. The last \ or / of the string (if any)
     * 3. Everything after the last \ or / of the string (if any) or the whole
     * string if there is no / or \.  I believe that under Win95 this would
     * include everything even though some might be clipped off the end whereas
     * under Win98 that might be ellipsified too.
     * Yet to be investigated is whether this would include wordbreaking if the
     * filename is more than 1 word and splitting if DT_EDITCONTROL was in
     * effect.  (If DT_EDITCONTROL is in effect then on occasions text will be
     * broken within words).
     * 4. All the stuff before the / or \, which is placed before the ellipsis.
     */
    static void TEXT_PathEllipsify(int hdc, int str, int max_len, IntRef len_str, int width, WinSize size, int modstr, ellipsis_data pellip) {
        int len_ellipsis;
        int len_trailing;
        int len_under;

        len_ellipsis = ELLIPSIS.length();
        if (max_len == 0) return;
        if (len_ellipsis >= max_len) len_ellipsis = max_len - 1;
        if (len_str.value + len_ellipsis >= max_len)
            len_str.value = max_len - len_ellipsis - 1;
        /* Hopefully this will never happen, otherwise it would probably lose
        * the wrong character
        */
        writeb(str + len_str.value, 0); /* to simplify things */

        int lastBkSlash = StringUtil.strrchr(str, BACK_SLASH);
        int lastFwdSlash = StringUtil.strrchr(str, FORWARD_SLASH);
        int lastSlash = lastBkSlash > lastFwdSlash ? lastBkSlash : lastFwdSlash;
        if (lastSlash == 0) lastSlash = str;
        len_trailing = len_str.value - (lastSlash - str);

        /* overlap-safe movement to the right */
        Memory.mem_memmove(lastSlash + len_ellipsis, lastSlash, len_trailing);
        Memory.mem_memcpy(lastSlash, ELLIPSIS.getBytes(), 0, len_ellipsis);
        len_trailing += len_ellipsis;
        /* From this point on lastSlash actually points to the ellipsis in front
         * of the last slash and len_trailing includes the ellipsis
         */

        len_under = 0;
        int pSize = getTempBuffer(WinSize.SIZE);
        while (true) {
            if (WinFont.GetTextExtentExPointA(hdc, str, len_str.value + len_ellipsis, width, NULL, NULL, pSize) == 0)
                break;
            size.copy(pSize);
            if (lastSlash == str || size.cx <= width)
                break;

            /* overlap-safe movement to the left */
            Memory.mem_memmove(lastSlash - 1, lastSlash, len_trailing);
            lastSlash--;
            len_under++;

            assert (len_str.value != 0);
            len_str.value--;
        }
        pellip.before = lastSlash - str;
        pellip.len = len_ellipsis;
        pellip.under = len_under;
        pellip.after = len_trailing - len_ellipsis;
        len_str.value += len_ellipsis;

        if (modstr != 0) {
            Memory.mem_memcpy(modstr, str, len_str.value);
            writeb(modstr + len_str.value, 0);
        }
    }

    /**
     * ******************************************************************
     * TEXT_WordBreak (static)
     * <p/>
     * Perform wordbreak processing on the given string
     * <p/>
     * Assumes that DT_WORDBREAK has been specified and not all the characters
     * fit.  Note that this function should even be called when the first character
     * that doesn't fit is known to be a space or tab, so that it can swallow them.
     * <p/>
     * Note that the Windows processing has some strange properties.
     * 1. If the text is left-justified and there is room for some of the spaces
     * that follow the last word on the line then those that fit are included on
     * the line.
     * 2. If the text is centered or right-justified and there is room for some of
     * the spaces that follow the last word on the line then all but one of those
     * that fit are included on the line.
     * 3. (Reasonable behaviour) If the word breaking causes a space to be the first
     * character of a new line it will be skipped.
     * <p/>
     * Arguments
     * hdc        [in] The handle to the DC that defines the font.
     * str        [in/out] The string that needs to be broken.
     * max_str    [in] The dimension of str (number of WCHAR).
     * len_str    [in/out] The number of characters in str
     * width      [in] The maximum width permitted
     * format     [in] The format flags in effect
     * chars_fit  [in] The maximum number of characters of str that are already
     * known to fit; chars_fit+1 is known not to fit.
     * chars_used [out] The number of characters of str that have been "used" and
     * do not need to be included in later text.  For example this will
     * include any spaces that have been discarded from the start of
     * the next line.
     * size       [out] The size of the returned text in logical coordinates
     * <p/>
     * Pedantic assumption - Assumes that the text length is monotonically
     * increasing with number of characters (i.e. no weird kernings)
     * <p/>
     * Algorithm
     * <p/>
     * Work back from the last character that did fit to either a space or the last
     * character of a word, whichever is met first.
     * If there was one or the first character didn't fit then
     * If the text is centered or right justified and that one character was a
     * space then break the line before that character
     * Otherwise break the line after that character
     * and if the next character is a space then discard it.
     * Suppose there was none (and the first character did fit).
     * If Break Within Word is permitted
     * break the word after the last character that fits (there must be
     * at least one; none is caught earlier).
     * Otherwise
     * discard any trailing space.
     * include the whole word; it may be ellipsified later
     * <p/>
     * Break Within Word is permitted under a set of circumstances that are not
     * totally clear yet.  Currently our best guess is:
     * If DT_EDITCONTROL is in effect and neither DT_WORD_ELLIPSIS nor
     * DT_PATH_ELLIPSIS is
     */

    static void TEXT_WordBreak(int hdc, int str, int max_str, IntRef len_str, int width, int format, int chars_fit, IntRef chars_used, WinSize size) {
        int p;
        boolean word_fits;
        assert ((format & DT_WORDBREAK) != 0);
        assert (chars_fit < len_str.value);

        /* Work back from the last character that did fit to either a space or the
         * last character of a word, whichever is met first.
         */
        p = str + chars_fit; /* The character that doesn't fit */
        word_fits = true;
        if (chars_fit == 0)
            ; /* we pretend that it fits anyway */
        else if (readb(p) == SPACE) /* chars_fit < *len_str so this is valid */
            p--; /* the word just fitted */
        else {
            while (p > str && readb(--p) != SPACE)
                ;
            word_fits = (p != str || readb(p) == SPACE);
        }
        /* If there was one or the first character didn't fit then */
        if (word_fits) {
            boolean next_is_space;
            /* break the line before/after that character */
            if ((format & (DT_RIGHT | DT_CENTER)) == 0 || readb(p) != SPACE)
                p++;
            next_is_space = (p - str) < len_str.value && readb(p) == SPACE;
            len_str.value = p - str;
            /* and if the next character is a space then discard it. */
            chars_used.value = len_str.value;
            if (next_is_space)
                chars_used.value++;
        }
        /* Suppose there was none. */
        else {
            if ((format & (DT_EDITCONTROL | DT_WORD_ELLIPSIS | DT_PATH_ELLIPSIS)) == DT_EDITCONTROL) {
                /* break the word after the last character that fits (there must be
                 * at least one; none is caught earlier).
                 */
                len_str.value = chars_fit;
                chars_used.value = chars_fit;

                /* FIXME - possible error.  Since the next character is now removed
                 * this could make the text longer so that it no longer fits, and
                 * so we need a loop to test and shrink.
                 */
            }
            /* Otherwise */
            else {
                /* discard any trailing space. */
                int e = str + len_str.value;
                p = str + chars_fit;
                while (p < e && readb(p) != SPACE)
                    p++;
                chars_used.value = p - str;
                if (p < e) /* i.e. loop failed because *p == SPACE */
                    chars_used.value++;

                /* include the whole word; it may be ellipsified later */
                len_str.value = p - str;
                /* Possible optimisation; if DT_WORD_ELLIPSIS only use chars_fit+1
                 * so that it will be too long
                 */
            }
        }
        /* Remeasure the string */
        int lpSize = getTempBuffer(WinSize.SIZE);
        WinFont.GetTextExtentExPointA(hdc, str, len_str.value, 0, NULL, NULL, lpSize);
        size.copy(lpSize);
    }

    /**
     * ******************************************************************
     * TEXT_SkipChars
     * <p/>
     * Skip over the given number of characters, bearing in mind prefix
     * substitution and the fact that a character may take more than one
     * WCHAR (Unicode surrogates are two words long) (and there may have been
     * a trailing &)
     * <p/>
     * Parameters
     * new_count  [out] The updated count
     * new_str    [out] The updated pointer
     * start_count [in] The count of remaining characters corresponding to the
     * start of the string
     * start_str  [in] The starting point of the string
     * max        [in] The number of characters actually in this segment of the
     * string (the & counts)
     * n          [in] The number of characters to skip (if prefix then
     * &c counts as one)
     * prefix     [in] Apply prefix substitution
     * <p/>
     * Return Values
     * none
     * <p/>
     * Remarks
     * There must be at least n characters in the string
     * We need max because the "line" may have ended with a & followed by a tab
     * or newline etc. which we don't want to swallow
     */

    static void TEXT_SkipChars(IntRef new_count, IntRef new_str, int start_count, int start_str, int max, int n, boolean prefix) {
        /* This is specific to wide characters, MSDN doesn't say anything much
         * about Unicode surrogates yet and it isn't clear if _wcsinc will
         * correctly handle them so we'll just do this the easy way for now
         */

        if (prefix) {
            int str_on_entry = start_str;
            assert (max >= n);
            max -= n;
            while (n-- != 0) {
                if (readb(start_str++) == PREFIX && max-- != 0)
                    start_str++;
            }
            start_count -= (start_str - str_on_entry);
        } else {
            start_str += n;
            start_count -= n;
        }
        new_str.value = start_str;
        new_count.value = start_count;
    }

    /**
     * ******************************************************************
     * TEXT_Reprefix
     * <p/>
     * Reanalyse the text to find the prefixed character.  This is called when
     * wordbreaking or ellipsification has shortened the string such that the
     * previously noted prefixed character is no longer visible.
     * <p/>
     * Parameters
     * str        [in] The original string segment (including all characters)
     * ns         [in] The number of characters in str (including prefixes)
     * pe         [in] The ellipsification data
     * <p/>
     * Return Values
     * The prefix offset within the new string segment (the one that contains the
     * ellipses and does not contain the prefix characters) (-1 if none)
     */

    static int TEXT_Reprefix(int str, int ns, ellipsis_data pe) {
        int result = -1;
        int i = 0;
        int n = pe.before + pe.under + pe.after;
        assert (n <= ns);
        while (i < n) {
            if (i == pe.before) {
                /* Reached the path ellipsis; jump over it */
                if (ns < pe.under)
                    break;
                str += pe.under;
                ns -= pe.under;
                i += pe.under;
                if (pe.after == 0) /* Nothing after the path ellipsis */
                    break;
            }
            if (ns == 0)
                break;
            ns--;
            if (readb(str++) == PREFIX) {
                if (ns == 0)
                    break;
                if (readb(str) != PREFIX)
                    result = (i < pe.before || pe.under == 0) ? i : i - pe.under + pe.len;
                /* pe->len may be non-zero while pe_under is zero */
                str++;
                ns--;
            }
            i++;
        }
        return result;
    }

    /**
     * ******************************************************************
     * Returns true if and only if the remainder of the line is a single
     * newline representation or nothing
     */

    static private boolean remainder_is_none_or_newline(int num_chars, int str) {
        if (num_chars == 0) return true;
        if (readb(str) != LF && readb(str) != CR) return false;
        if (--num_chars == 0) return true;
        if (readb(str) == readb(str + 1)) return false;
        str++;
        if (readb(str) != CR && readb(str) != LF) return false;
        if (--num_chars != 0) return false;
        return true;
    }
}
