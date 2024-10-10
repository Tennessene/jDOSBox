package jdos.win.builtin.gdi32;


import jdos.win.system.WinObject;
import jdos.win.system.WinRect;

import java.util.Vector;

public class WinRegion extends WinGDI {
    static public WinRegion create() {
        return new WinRegion(nextObjectId());
    }

    static public WinRegion createNoHandle() {
        return new WinRegion(0);
    }

    static public WinRegion get(int handle) {
        WinObject object = getObject(handle);
        if (object == null || !(object instanceof WinRegion))
            return null;
        return (WinRegion) object;
    }

    // INT WINAPI GetRgnBox( HRGN hrgn, LPRECT rect )
    static public int GetRgnBox(int hrgn, int pRect) {
        WinRegion obj = WinRegion.get(hrgn);
        if (obj != null) {
            obj.extents.write(pRect);
            return obj.getType();
        }
        return ERROR;
    }

    // HRGN WINAPI CreateRectRgn(INT left, INT top, INT right, INT bottom)
    static public int CreateRectRgn(int left, int top, int right, int bottom) {
        WinRegion rgn = WinRegion.create();
        rgn.rects.add(new WinRect(left, top, right, bottom));
        return rgn.handle;
    }

    // HRGN WINAPI CreateRectRgnIndirect( const RECT* rect )
    static public int CreateRectRgnIndirect(WinRect rect) {
        return CreateRectRgn(rect.left, rect.top, rect.right, rect.bottom );
    }

    // INT WINAPI CombineRgn(HRGN hDest, HRGN hSrc1, HRGN hSrc2, INT mode)
    static public int CombineRgn(int hDest, int hSrc1, int hSrc2, int mode) {
        WinRegion destObj = WinRegion.get(hDest);
        WinRegion src1Obj = WinRegion.get(hSrc1);

        if (destObj == null || src1Obj == null) {
            return ERROR;
        }

        if (mode == RGN_COPY) {
            destObj.copy(src1Obj);
            return destObj.getType();
        }

        WinRegion src2Obj = WinRegion.get(hSrc2);
        if (src2Obj == null) {
            return ERROR;
        }

        switch (mode) {
            case RGN_AND:
            if (WinRegion.intersect(destObj, src1Obj, src2Obj))
                return destObj.getType();
            break;
            case RGN_OR:
            if (WinRegion.union(destObj, src1Obj, src2Obj))
                return destObj.getType();
            break;
            case RGN_XOR:
            if (WinRegion.xor(destObj, src1Obj, src2Obj))
                return destObj.getType();
            break;
            case RGN_DIFF:
            if (WinRegion.subtract(destObj, src1Obj, src2Obj))
                return destObj.getType();
            break;
        }
        return ERROR;
    }
    
    private WinRegion(int id) {
        super(id);
    }

    public int getType() {
        switch (rects.size()) {
            case 0:
                return NULLREGION;
            case 1:
                return SIMPLEREGION;
            default:
                return COMPLEXREGION;
        }
    }

    public Vector<WinRect> rects = new Vector<WinRect>();
    public WinRect extents = new WinRect();

    public boolean isEmpty() {
        return rects.size() == 0;
    }

    public void offset(int x, int y) {
        for (int i = 0; i < rects.size(); i++) {
            WinRect rect = (WinRect) rects.elementAt(i);
            rect.offset(x, y);
        }
        extents.offset(x, y);
    }

    /**
     * ********************************************************************
     * REGION_IntersectRegion
     */
    static public boolean intersect(WinRegion newReg, WinRegion reg1, WinRegion reg2) {
        /* check for trivial reject */
        if (reg1.rects.size() == 0 || reg2.rects.size() == 0 || !EXTENTCHECK(reg1.extents, reg2.extents))
            newReg.rects.clear();
        else if (!region_op(newReg, reg1, reg2, intersect0, null, null)) return false;

        /*
         * Can't alter newReg's extents before we call miRegionOp because
         * it might be one of the source regions and miRegionOp depends
         * on the extents of those regions being the same. Besides, this
         * way there's no checking against rectangles that will be nuked
         * due to coalescing, so we have to examine fewer rectangles.
         */
        newReg.calculateExtents();
        return true;
    }

    /**
     * ********************************************************************
     * REGION_UnionRegion
     */
    static public boolean union(WinRegion newReg, WinRegion reg1, WinRegion reg2) {
        /*  checks all the simple cases */

        /*
        * Region 1 and 2 are the same or region 1 is empty
        */
        if (reg1 == reg2 || reg1.rects.size() == 0) {
            if (newReg != reg2)
                newReg.copy(reg2);
            return true;
        }

        /*
        * if nothing to union (region 2 empty)
        */
        if (reg2.rects.size() == 0) {
            if (newReg != reg1)
                newReg.copy(reg1);
            return true;
        }

        /*
        * Region 1 completely subsumes region 2
        */
        if (reg1.rects.size() == 1 && reg1.extents.left <= reg2.extents.left && reg1.extents.top <= reg2.extents.top &&
                reg1.extents.right >= reg2.extents.right && reg1.extents.bottom >= reg2.extents.bottom) {
            if (newReg != reg1)
                newReg.copy(reg1);
            return true;
        }

        /*
        * Region 2 completely subsumes region 1
        */
        if (reg2.rects.size() == 1 && reg2.extents.left <= reg1.extents.left && reg2.extents.top <= reg1.extents.top &&
                reg2.extents.right >= reg1.extents.right && reg2.extents.bottom >= reg1.extents.bottom) {
            if (newReg != reg2)
                newReg.copy(reg2);
            return true;
        }

        if (!region_op(newReg, reg1, reg2, union0, unionNon0, unionNon0)) return false;
        newReg.calculateExtents();
        return true;
    }

    static public boolean xor(WinRegion dr, WinRegion sra, WinRegion srb) {
        WinRegion tra = WinRegion.createNoHandle();
        WinRegion trb = WinRegion.createNoHandle();

        return subtract(tra, sra, srb) && subtract(trb, srb, sra) && union(dr, tra, trb);
    }

    static public boolean subtract(WinRegion dst, WinRegion src1, WinRegion src2) {
        if (src1.rects.size() == 0 || src2.rects.size() == 0 || !EXTENTCHECK(src1.extents, src2.extents)) {
            dst.copy(src1);
            return true;
        }
        if (!region_op(dst, src1, src2, subtract_overlapping, subtract_non_overlapping, null)) return false;
        dst.calculateExtents();
        return true;
    }

    static private interface Overlapped {
        public boolean call(WinRegion rg, Vector reg1, int r1, int r1Stop, Vector reg2, int r2, int r2Stop, int top, int bottom);
    }

    static private interface NonOverlapped {
        public boolean call(WinRegion rg, Vector r, int rStart, int rStop, int top, int bottom);
    }

    static int left(Vector v, int pos) {
        return ((WinRect) v.elementAt(pos)).left;
    }

    static int right(Vector v, int pos) {
        return ((WinRect) v.elementAt(pos)).right;
    }

    static int top(Vector v, int pos) {
        return ((WinRect) v.elementAt(pos)).top;
    }

    static int bottom(Vector v, int pos) {
        return ((WinRect) v.elementAt(pos)).bottom;
    }

    /**
     * ********************************************************************
     * REGION_IntersectO
     * <p/>
     * Handle an overlapping band for REGION_Intersect.
     * <p/>
     * Results:
     * None.
     * <p/>
     * Side Effects:
     * Rectangles may be added to the region.
     */
    static private Overlapped intersect0 = new Overlapped() {
        public boolean call(WinRegion pReg, Vector reg1, int r1, int r1End, Vector reg2, int r2, int r2End, int top, int bottom) {
            int left, right;

            while ((r1 != r1End) && (r2 != r2End)) {
                left = Math.max(left(reg1, r1), left(reg2, r2));
                right = Math.min(right(reg1, r1), right(reg2, r2));

                /*
                * If there's any overlap between the two rectangles, add that
                * overlap to the new region.
                * There's no need to check for subsumption because the only way
                * such a need could arise is if some region has two rectangles
                * right next to each other. Since that should never happen...
                */
                if (left < right) {
                    pReg.rects.add(new WinRect(left, top, right, bottom));
                }

                /*
                * Need to advance the pointers. Shift the one that extends
                * to the right the least, since the other still has a chance to
                * overlap with that region's next rectangle, if you see what I mean.
                */
                if (right(reg1, r1) < right(reg2, r2)) {
                    r1++;
                } else if (right(reg2, r2) < right(reg1, r1)) {
                    r2++;
                } else {
                    r1++;
                    r2++;
                }
            }
            return true;
        }
    };

    /**
     * ********************************************************************
     * REGION_UnionNonO
     * <p/>
     * Handle a non-overlapping band for the union operation. Just
     * Adds the rectangles into the region. Doesn't have to check for
     * subsumption or anything.
     * <p/>
     * Results:
     * None.
     * <p/>
     * Side Effects:
     * pReg->numRects is incremented and the final rectangles overwritten
     * with the rectangles we're passed.
     */
    static private NonOverlapped unionNon0 = new NonOverlapped() {
        public boolean call(WinRegion pReg, Vector reg, int r, int rEnd, int top, int bottom) {
            while (r != rEnd) {
                pReg.rects.add(new WinRect(left(reg, r), top, right(reg, r), bottom));
                r++;
            }
            return true;
        }
    };

    /**
     * ********************************************************************
     * REGION_UnionO
     * <p/>
     * Handle an overlapping band for the union operation. Picks the
     * left-most rectangle each time and merges it into the region.
     * <p/>
     * Results:
     * None.
     * <p/>
     * Side Effects:
     * Rectangles are overwritten in pReg->rects and pReg->numRects will
     * be changed.
     */
    static private void MERGERECT(WinRegion pReg, Vector reg, int r, int top, int bottom) {
        int last = pReg.rects.size() - 1;
        if (pReg.rects.size() != 0 && top(pReg.rects, last) == top && bottom(pReg.rects, last) == bottom && right(pReg.rects, last) >= left(reg, r)) {
            if (right(pReg.rects, last) < right(reg, r))
                ((WinRect) pReg.rects.elementAt(last)).right = right(reg, r);
        } else {
            pReg.rects.add(new WinRect(left(reg, r), top, right(reg, r), bottom));
        }
    }

    static private Overlapped union0 = new Overlapped() {
        public boolean call(WinRegion pReg, Vector reg1, int r1, int r1End, Vector reg2, int r2, int r2End, int top, int bottom) {
            while ((r1 != r1End) && (r2 != r2End)) {
                if (left(reg1, r1) < left(reg2, r2)) {
                    MERGERECT(pReg, reg1, r1++, top, bottom);
                } else {
                    MERGERECT(pReg, reg2, r2++, top, bottom);
                }
            }

            if (r1 != r1End) {
                do {
                    MERGERECT(pReg, reg1, r1++, top, bottom);
                } while (r1 != r1End);
            } else {
                while (r2 != r2End) {
                    MERGERECT(pReg, reg2, r2++, top, bottom);
                }
            }
            return true;
        }
    };

    static private NonOverlapped subtract_non_overlapping = new NonOverlapped() {
        public boolean call(WinRegion rg, Vector reg, int r, int rEnd, int top, int bottom) {
            while (r != rEnd) {
                WinRect rect = new WinRect();
                rg.rects.add(rect);
                rect.left = left(reg, r);
                rect.top = top;
                rect.right = right(reg, r);
                rect.bottom = bottom;
                r++;
            }
            return true;
        }
    };

    /* handle an overlapping band for subtract_region */
    static private Overlapped subtract_overlapping = new Overlapped() {
        public boolean call(WinRegion rg, Vector reg1, int r1, int r1End, Vector reg2, int r2, int r2End, int top, int bottom) {
            int left = left(reg1, r1);

            while ((r1 != r1End) && (r2 != r2End)) {
                if (right(reg2, r2) <= left) r2++;
                else if (left(reg2, r2) <= left) {
                    left = right(reg2, r2);
                    if (left >= right(reg1, r1)) {
                        r1++;
                        if (r1 != r1End)
                            left = left(reg1, r1);
                    } else r2++;
                } else if (left(reg2, r2) < right(reg1, r1)) {
                    WinRect rect = new WinRect();
                    rg.rects.add(rect);
                    rect.left = left;
                    rect.top = top;
                    rect.right = left(reg2, r2);
                    rect.bottom = bottom;
                    left = right(reg2, r2);
                    if (left >= right(reg1, r1)) {
                        r1++;
                        if (r1 != r1End)
                            left = left(reg1, r1);
                    } else r2++;
                } else {
                    if (right(reg1, r1) > left) {
                        WinRect rect = new WinRect();
                        rg.rects.add(rect);
                        rect.left = left;
                        rect.top = top;
                        rect.right = right(reg1, r1);
                        rect.bottom = bottom;
                    }
                    r1++;
                    left = left(reg1, r1);
                }
            }

            while (r1 != r1End) {
                WinRect rect = new WinRect();
                rg.rects.add(rect);
                rect.left = left;
                rect.top = top;
                rect.right = right(reg1, r1);
                rect.bottom = bottom;
                r1++;
                if (r1 != r1End) left = left(reg1, r1);
            }
            return true;
        }
    };

    int getTop(int index) {
        return ((WinRect) rects.elementAt(index)).top;
    }

    int getBottom(int index) {
        return ((WinRect) rects.elementAt(index)).bottom;
    }

    int getLeft(int index) {
        return ((WinRect) rects.elementAt(index)).left;
    }

    int getRight(int index) {
        return ((WinRect) rects.elementAt(index)).right;
    }

    /* apply an operation to two regions */
    /* check the GDI version of the code for explanations */
    static public boolean region_op(WinRegion newReg, WinRegion reg1, WinRegion reg2, Overlapped overlap_func, NonOverlapped non_overlap1_func, NonOverlapped non_overlap2_func) {
        int ybot, ytop, top, bot, prevBand, curBand;
        int r1BandEnd;
        int r2BandEnd;

        int r1 = 0;
        int r2 = 0;
        int r1End = reg1.rects.size();
        int r2End = reg2.rects.size();

        newReg.rects.clear();

        if (reg1.extents.top < reg2.extents.top)
            ybot = reg1.extents.top;
        else
            ybot = reg2.extents.top;

        prevBand = 0;

        do {
            curBand = newReg.rects.size();

            r1BandEnd = r1;
            while ((r1BandEnd != r1End) && (reg1.getTop(r1BandEnd) == reg1.getTop(r1))) r1BandEnd++;

            r2BandEnd = r2;
            while ((r2BandEnd != r2End) && (reg2.getTop(r2BandEnd) == reg2.getTop(r2))) r2BandEnd++;

            if (reg1.getTop(r1) < reg2.getTop(r2)) {
                top = Math.max(reg1.getTop(r1), ybot);
                bot = Math.min(reg1.getBottom(r1), reg2.getTop(r2));
                if ((top != bot) && non_overlap1_func != null) {
                    if (!non_overlap1_func.call(newReg, reg1.rects, r1, r1BandEnd, top, bot)) return false;
                }
                ytop = reg2.getTop(r2);
            } else if (reg2.getTop(r2) < reg1.getTop(r1)) {
                top = Math.max(reg2.getTop(r2), ybot);
                bot = Math.min(reg2.getBottom(r2), reg1.getTop(r1));

                if ((top != bot) && non_overlap2_func != null) {
                    if (!non_overlap2_func.call(newReg, reg2.rects, r2, r2BandEnd, top, bot)) return false;
                }

                ytop = reg1.getTop(r1);
            } else {
                ytop = reg1.getTop(r1);
            }

            if (newReg.rects.size() != curBand)
                prevBand = coalesce_region(newReg, prevBand, curBand);

            ybot = Math.min(reg1.getBottom(r1), reg2.getBottom(r2));
            curBand = newReg.rects.size();
            if (ybot > ytop) {
                if (!overlap_func.call(newReg, reg1.rects, r1, r1BandEnd, reg2.rects, r2, r2BandEnd, ytop, ybot))
                    return false;
            }

            if (newReg.rects.size() != curBand)
                prevBand = coalesce_region(newReg, prevBand, curBand);

            if (reg1.getBottom(r1) == ybot) r1 = r1BandEnd;
            if (reg2.getBottom(r2) == ybot) r2 = r2BandEnd;
        } while ((r1 != r1End) && (r2 != r2End));

        curBand = newReg.rects.size();
        if (r1 != r1End) {
            if (non_overlap1_func != null) {
                do {
                    r1BandEnd = r1;
                    while ((r1BandEnd < r1End) && (reg1.getTop(r1BandEnd) == reg1.getTop(r1))) r1BandEnd++;
                    if (!non_overlap1_func.call(newReg, reg1.rects, r1, r1BandEnd, Math.max(reg1.getTop(r1), ybot), reg1.getBottom(r1)))
                        return false;
                    r1 = r1BandEnd;
                } while (r1 != r1End);
            }
        } else if ((r2 != r2End) && non_overlap2_func != null) {
            do {
                r2BandEnd = r2;
                while ((r2BandEnd < r2End) && (reg2.getTop(r2BandEnd) == reg2.getTop(r2))) r2BandEnd++;
                if (!non_overlap2_func.call(newReg, reg2.rects, r2, r2BandEnd, Math.max(reg2.getTop(r2), ybot), reg2.getBottom(r2)))
                    return false;
                r2 = r2BandEnd;
            } while (r2 != r2End);
        }

        if (newReg.rects.size() != curBand) coalesce_region(newReg, prevBand, curBand);

        return true;
    }

    /* attempt to merge the rects in the current band with those in the */
    /* previous one. Used only by region_op. */
    static private int coalesce_region(WinRegion reg, int prevStart, int curStart) {
        int curNumRects;
        int pRegEnd = reg.rects.size();
        int pPrevRect = prevStart;
        int pCurRect = curStart;
        int prevNumRects = curStart - prevStart;
        int bandtop = reg.getTop(pCurRect);

        for (curNumRects = 0; (pCurRect != pRegEnd) && (reg.getTop(pCurRect) == bandtop); curNumRects++) {
            pCurRect++;
        }

        if (pCurRect != pRegEnd) {
            pRegEnd--;
            while (reg.getTop(pRegEnd - 1) == reg.getTop(pRegEnd)) pRegEnd--;
            curStart = pRegEnd;
            pRegEnd = reg.rects.size();
        }

        if ((curNumRects == prevNumRects) && (curNumRects != 0)) {
            pCurRect -= curNumRects;
            if (reg.getBottom(pPrevRect) == reg.getTop(pCurRect)) {
                int size = reg.rects.size();
                do {
                    if ((reg.getLeft(pPrevRect) != reg.getLeft(pCurRect)) ||
                            (reg.getRight(pPrevRect) != reg.getRight(pCurRect))) return curStart;
                    pPrevRect++;
                    pCurRect++;
                    prevNumRects -= 1;
                } while (prevNumRects != 0);

                size -= curNumRects;
                pCurRect -= curNumRects;
                pPrevRect -= curNumRects;

                do {
                    ((WinRect) reg.rects.get(pPrevRect)).bottom = reg.getBottom(pCurRect);
                    pPrevRect++;
                    pCurRect++;
                    curNumRects -= 1;
                } while (curNumRects != 0);

                if (pCurRect == pRegEnd) curStart = prevStart;
                else do {
                    ((WinRect) reg.rects.get(pPrevRect++)).copy((WinRect) reg.rects.get(pCurRect++));
                } while (pCurRect != pRegEnd);

                while (size < reg.rects.size()) {
                    reg.rects.remove(reg.rects.size() - 1);
                }
            }
        }
        return curStart;
    }

    /* recalculate the extents of a region */
    private void calculateExtents() {
        if (rects.size() == 0) {
            extents.left = 0;
            extents.top = 0;
            extents.right = 0;
            extents.bottom = 0;
        } else {
            extents = ((WinRect) rects.elementAt(0)).copy();
            for (int i = 1; i < rects.size(); i++) {
                WinRect rect = (WinRect) rects.elementAt(i);
                if (rect.left < extents.left) extents.left = rect.left;
                if (rect.top < extents.top) extents.top = rect.top;
                if (rect.right > extents.right) extents.right = rect.right;
                if (rect.bottom < extents.bottom) extents.bottom = rect.bottom;
            }
        }
    }

    static private boolean EXTENTCHECK(WinRect r1, WinRect r2) {
        return r1.right > r2.left && r1.left < r2.right && r1.bottom > r2.top && r1.top < r2.bottom;
    }

    public WinRegion copyNoHandle() {
        WinRegion result = createNoHandle();
        result.copy(this);
        return result;
    }

    public void copy(WinRegion rgn) {
        for (int i = 0; i < rects.size(); i++) {
            rects.add(((WinRect) rgn.rects.elementAt(i)).copy());
        }
        extents = rgn.extents.copy();
    }

    public WinRegion copy() {
        WinRegion result = create();
        result.copy(this);
        return result;
    }

    public String toString() {
        return "REGION size="+rects.size()+(rects.size()>0?" rect(1)="+rects.elementAt(0).toString():"");
    }
}
