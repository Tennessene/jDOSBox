package jdos.hardware.mame;

public class RasterizerCompiler {
    static private void CLAMP(StringBuilder method, String val, String min, String max) {
        method.append("if (").append(val).append(" < ").append(min).append(") {\n");
        method.append("    ").append(val).append(" = ").append(min).append(";\n");
        method.append("} else if (").append(val).append(" > ").append(max).append(") {\n");
        method.append("    ").append(val).append(" = ").append(max).append(";\n");
        method.append("}\n");
    }

    static private void CLAMPED_Z(StringBuilder method, String iterz, int fbzcp, String result) {
        method.append(result).append(" = ").append(iterz).append(" >> 12;\n");
        if (!VoodooCommon.FBZCP_RGBZW_CLAMP(fbzcp)) {
            method.append(result).append(" &= 0xfffff;\n");
            method.append("if (").append(result).append(" == 0xfffff)\n");
            method.append("    ").append(result).append(" = 0;\n");
            method.append("else if (").append(result).append(" == 0x10000)\n");
            method.append("    ").append(result).append(" = 0xffff;\n");
            method.append("else\n");
            method.append("    ").append(result).append(" &= 0xffff;\n");
        } else {
            CLAMP(method, result, "0", "0xffff");
        }
    }

    static public Poly.poly_draw_scanline_func compile(int tmuCount, int colorPath, int alphaMode, int fogMode, int fbzMode, int textureMode0, int textureMode1) {
        StringBuilder method = new StringBuilder();
        compile(method, tmuCount, colorPath, alphaMode, fogMode, fbzMode, textureMode0, textureMode1);
        return null;
    }

    static private void compile(StringBuilder method, int tmuCount, int colorPath, int alphaMode, int fogMode, int fbzMode, int textureMode0, int textureMode1) {
        method.append(          "final VoodooCommon v = extra.state;\n" +
                                "final stats_block stats = v.thread_stats[threadid];\n");
        if (VoodooCommon.FBZMODE_ENABLE_DITHERING(fbzMode)) {
            method.append(      "byte[] dither_lookup = null;\n" +
                                "int dither_lookupPos = 0;\n" +
                                "byte[] dither4 = null;\n" +
                                "int dither4Pos = 0;\n" +
                                "byte[] dither = null;\n" +
                                "int ditherPos=0;");
        }

        method.append(          "int startx = extent.startx;\n" +
                                "int stopx = extent.stopx;\n" +
                                "int iterz;\n" +
                                "long iterw;\n" +
                                "long iterw0 = 0, iterw1 = 0;\n" +
                                "long iters0 = 0, iters1 = 0;\n" +
                                "long itert0 = 0, itert1 = 0;\n" +
                                "int depthPos;\n" +
                                "int destPos;\n" +
                                "int dx, dy;\n" +
                                "int scry=y;\n" +
                                "int x;\n");

        if (VoodooCommon.FBZMODE_Y_ORIGIN(fbzMode)) {
            method.append(      "scry = (v.fbi.yorigin - y) & 0x3ff;\n");
        }

        if (VoodooCommon.FBZMODE_ENABLE_DITHERING(fbzMode)) {
            method.append(      "dither4 = dither_matrix_4x4;\n" +
                                "dither4Pos = (y & 3) * 4;\n");
            if (!VoodooCommon.FBZMODE_DITHER_TYPE(fbzMode)) {
                method.append(  "dither = dither4;\n" +
                                "ditherPos = dither4Pos;\n" +
                                "dither_lookup = dither4_lookup;\n" +
                                "dither_lookupPos = (y & 3) << 11;\n");
            } else {
                method.append(  "dither = dither_matrix_2x2;\n" +
                                "ditherPos = (y & 3) * 4;\n" +
                                "dither_lookup = dither2_lookup;\n" +
                                "dither_lookupPos = (y & 3) << 11;\n");
            }
        }

        if (VoodooCommon.FBZMODE_ENABLE_CLIPPING(fbzMode)) {
            method.append(      "if (scry < ((v.reg[clipLowYHighY] >> 16) & 0x3ff) || scry >= (v.reg[clipLowYHighY] & 0x3ff)) {\n" +
                                "    stats.pixels_in += stopx - startx;\n" +
                                "    stats.clip_fail += stopx - startx;\n" +
                                "    return;\n" +
                                "}\n" +
                                "int tempclip = (v.reg[clipLeftRight] >> 16) & 0x3ff;\n" +
                                "if (startx < tempclip) {\n" +
                                "    stats.pixels_in += tempclip - startx;\n" +
                                "    v.stats.total_clipped += tempclip - startx;\n" +
                                "    startx = tempclip;\n" +
                                "}\n" +
                                "tempclip = v.reg[clipLeftRight] & 0x3ff;\n" +
                                "if (stopx >= tempclip) {\n" +
                                "    stats.pixels_in += stopx - tempclip;\n" +
                                "    v.stats.total_clipped += stopx - tempclip;\n" +
                                "    stopx = tempclip - 1;\n" +
                                "}\n");
        }
        method.append(          "destPos = destbasePos+scry * v.fbi.rowpixels;\n" +
                                "depthPos = (v.fbi.auxoffs != -1) ? (v.fbi.auxoffs + scry * v.fbi.rowpixels) : -1;\n" +
                                "dx = startx - (extra.ax >> 4);\n" +
                                "dy = y - (extra.ay >> 4);\n" +
                                "int iterr = extra.startr + dy * extra.drdy + dx * extra.drdx;\n" +
                                "int iterg = extra.startg + dy * extra.dgdy + dx * extra.dgdx;\n" +
                                "int iterb = extra.startb + dy * extra.dbdy + dx * extra.dbdx;\n" +
                                "int itera = extra.starta + dy * extra.dady + dx * extra.dadx;\n" +
                                "iterz = extra.startz + dy * extra.dzdy + dx * extra.dzdx;\n" +
                                "iterw = extra.startw + dy * extra.dwdy + dx * extra.dwdx;\n");
        if (tmuCount >= 1) {
            method.append(      "iterw0 = extra.startw0 + dy * extra.dw0dy + dx * extra.dw0dx;\n" +
                                "iters0 = extra.starts0 + dy * extra.ds0dy + dx * extra.ds0dx;\n" +
                                "itert0 = extra.startt0 + dy * extra.dt0dy + dx * extra.dt0dx;\n");
        }
        if (tmuCount >= 2) {
            method.append(      "iterw1 = extra.startw1 + dy * extra.dw1dy + dx * extra.dw1dx;\n" +
                                "iters1 = extra.starts1 + dy * extra.ds1dy + dx * extra.ds1dx;\n" +
                                "itert1 = extra.startt1 + dy * extra.dt1dy + dx * extra.dt1dx;\n");
        }
        method.append("for (x = startx; x < stopx; x++) {\n" +
                "int texel = 0;\n" +
                "int iterargb = 0\n" +
                "int depthval;\n" +
                "int r, g, b, a;\n" +
                "stats.pixels_in++;\n");
        if (VoodooCommon.FBZMODE_ENABLE_STIPPLE(fbzMode)) {
            /* rotate mode */
            if (!VoodooCommon.FBZMODE_STIPPLE_PATTERN(fbzMode)) {
                method.append(  "v.reg[").append(VoodooCommon.stipple).append("] = (v.reg[").append(VoodooCommon.stipple).append("] << 1) | (v.reg[").append(VoodooCommon.stipple).append("] >> 31);\n");
                method.append(  "if ((v.reg[").append(VoodooCommon.stipple).append("] & 0x80000000) == 0) {\n" +
                                "    v.stats.total_stippled++;\n" +
                                "    return;\n" +
                                "}\n");
            } else { /* pattern mode */
                method.append(  "if (((reg[").append(VoodooCommon.stipple).append("] >> (((y & 3) << 3) | (~x & 7))) & 1) == 0) {\n" +
                        "    v.stats.total_stippled++;\n" +
                        "    return;\n" +
                        "}\n");
            }
        }

        boolean needDepthVal = (VoodooCommon.FBZMODE_ENABLE_DEPTHBUF(fbzMode) && !VoodooCommon.FBZMODE_DEPTH_SOURCE_COMPARE(fbzMode)) || (VoodooCommon.FBZMODE_AUX_BUFFER_MASK(fbzMode) && !VoodooCommon.FBZMODE_ENABLE_ALPHA_PLANES(fbzMode));
        boolean needWFloat = (needDepthVal && VoodooCommon.FBZMODE_WBUFFER_SELECT(fbzMode) && !VoodooCommon.FBZMODE_DEPTH_FLOAT_SELECT(fbzMode)) || (VoodooCommon.FOGMODE_ENABLE_FOG(fogMode) && VoodooCommon.FOGMODE_FOG_ZALPHA(fogMode)==0);

        if (needWFloat) {
            method.append(      "int wfloat;\n" +
                                "if ((iterw & 0xffff00000000l)!=0) {\n" +
                                "    wfloat = 0x0000;\n" +
                                "} else {\n" +
                                "    int temp = (int)ITERW.value;\n" +
                                "    if ((temp & 0xffff0000) == 0) {\n" +
                                "        wfloat = 0xffff;\n" +
                                "    } else {\n" +
                                "        int exp = Integer.numberOfLeadingZeros(temp);\n" +
                                "        wfloat = ((exp << 12) | ((~temp >> (19 - exp)) & 0xfff)) + 1;\n" +
                                "    }\n"+
                                "}\n");
        }
        if (needDepthVal) {
            if (!VoodooCommon.FBZMODE_WBUFFER_SELECT(fbzMode)) {
                method.append(  "int depthval = ");CLAMPED_Z(method, "iterz", colorPath, "depthval");
            } else if (!VoodooCommon.FBZMODE_DEPTH_FLOAT_SELECT(fbzMode))
                method.append(  "int depthval = wfloat;\n");
            else {
                method.append(  "int depthval;\n");
                method.append(  "if ((iterz & 0xf0000000)!=0) {\n" +
                                "    depthval = 0x0000;\n" +
                                "} else {\n" +
                                "    int temp = iterz << 4;\n" +
                                "    if ((temp & 0xffff0000) == 0) {\n" +
                                "        depthval = 0xffff;\n" +
                                "    } else {\n" +
                                "        int exp = Integer.numberOfLeadingZeros(temp);\n" +
                                "        depthval = ((exp << 12) | ((~temp >> (19 - exp)) & 0xfff)) + 1;\n" +
                                "    }\n" +
                                "}\n");
            }
        }

        if (VoodooCommon.FBZMODE_ENABLE_DEPTH_BIAS(fbzMode)) {
            method.append(      "depthval += (short)v.reg[zaColor];\n");
            CLAMP(method, "depthval", "0", "0xffff");
        }

        if (VoodooCommon.FBZMODE_ENABLE_DEPTHBUF(fbzMode)) {
            if (!VoodooCommon.FBZMODE_DEPTH_SOURCE_COMPARE(fbzMode))
                method.append(  "int depthsource = depthval;\n");
            else
                method.append(  "int depthsource = v.reg[").append(VoodooCommon.zaColor).append("] & 0xFFFF;\n");

            //////////////////////////
            //                      //
            // depthbase is UINT16  //
            //                      //
            //////////////////////////
            /* test against the depth buffer */
            switch (VoodooCommon.FBZMODE_DEPTH_FUNCTION(fbzMode))
            {
                case 0:     /* depthOP = never */
                    method.append(  "stats.zfunc_fail++;\n" +
                                    "return;\n");
                    return;
                case 1:     /* depthOP = less than */
                    method.append(  "if (depthsource >= depthbase[depthPos+XX] & 0xFFFF)) {\n" +
                                    "    stats.zfunc_fail++;\n" +
                                    "    return;\n" +
                                    "}\n");
                    break;
                case 2:     /* depthOP = equal */
                    method.append(  "if (depthsource != depthbase[depthPos+XX] & 0xFFFF)) {\n" +
                                    "    stats.zfunc_fail++;\n" +
                                    "    return;\n" +
                                    "}\n");
                    break;
                case 3:     /* depthOP = less than or equal */
                    method.append(  "if (depthsource > depthbase[depthPos+XX] & 0xFFFF)) {\n" +
                                    "    stats.zfunc_fail++;\n" +
                                    "    return;\n" +
                                    "}\n");
                    break;
                case 4:     /* depthOP = greater than */
                    method.append(  "if (depthsource <= depthbase[depthPos+XX] & 0xFFFF)) {\n" +
                                    "    stats.zfunc_fail++;\n" +
                                    "    return;\n" +
                                    "}\n");
                    break;
                case 5:     /* depthOP = not equal */
                    method.append(  "if (depthsource == depthbase[depthPos+XX] & 0xFFFF)) {\n" +
                                    "    stats.zfunc_fail++;\n" +
                                    "    return;\n" +
                                    "}\n");
                    break;
                case 6:     /* depthOP = greater than or equal */
                    method.append(  "if (depthsource < depthbase[depthPos+XX] & 0xFFFF)) {\n" +
                                    "    stats.zfunc_fail++;\n" +
                                    "    return;\n" +
                                    "}\n");
                    break;
                case 7:     /* depthOP = always */
                    break;
            }
        }
    }
}
