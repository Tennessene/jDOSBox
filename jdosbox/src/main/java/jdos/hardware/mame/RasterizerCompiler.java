package jdos.hardware.mame;

import javassist.*;
import jdos.Dosbox;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class RasterizerCompiler extends RasterizerCompilerCommon {
    static private class SaveInfo {
        public SaveInfo(raster_info info, byte[] byteCode) {
            this.info = info;
            this.byteCode = byteCode;
        }
        raster_info info;
        byte[] byteCode;
    }
    private static Vector<SaveInfo> savedClasses = new Vector<SaveInfo>();

    static public void save(ZipOutputStream out) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        dos.writeInt(1); // version
        dos.writeInt(savedClasses.size());
        for (int i=0;i<savedClasses.size();i++) {
            SaveInfo info = savedClasses.elementAt(i);
            String name = "Rasterizer" + i;
            out.putNextEntry(new ZipEntry(name + ".class"));
            out.write(info.byteCode);
            dos.writeUTF(name);
            dos.writeInt(info.info.eff_color_path);
            dos.writeInt(info.info.eff_alpha_mode);
            dos.writeInt(info.info.eff_fog_mode);
            dos.writeInt(info.info.eff_fbz_mode);
            dos.writeInt(info.info.eff_tex_mode_0);
            dos.writeInt(info.info.eff_tex_mode_1);
        }
        out.putNextEntry(new ZipEntry("jdos/Rasterizer.index"));
        dos.flush();
        out.write(bos.toByteArray());
    }

    static private void getRegR(StringBuilder method, String value) {
        method.append("(").append(value).append(" >> 16) & 0xFF");
    }
    static private void getRegG(StringBuilder method, String value) {
        method.append("(").append(value).append(" >> 8) & 0xFF");
    }
    static private void getRegB(StringBuilder method, String value) {
        method.append(value).append(" & 0xFF");
    }
    static private void getRegA(StringBuilder method, String value) {
        method.append("(").append(value).append(" >> 24) & 0xFF");
    }

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

    static private void CLAMPED_W(StringBuilder method, String iterw, int fbzcp, String result) {
        method.append(result).append(" = (short)").append(iterw).append(" >> 32;\n");
        if (!VoodooCommon.FBZCP_RGBZW_CLAMP(fbzcp)) {
            method.append(result).append(" &= 0xffff;\n");
            method.append("if (").append(result).append(" == 0xffff)\n");
            method.append("    ").append(result).append(" = 0;\n");
            method.append("else if (").append(result).append(" == 0x100)\n");
            method.append("    ").append(result).append(" = 0xff;\n");
            method.append(result).append(" &= 0xff;\n");
        } else {
            CLAMP(method, result, "0", "0xff");
        }
    }

    static private void TEXTURE_PIPELINE(StringBuilder method, int texMode, String lodBase, String iters, String itert, String iterw) {
//        int blendr, blendg, blendb, blenda;
//        int tr, tg, tb, ta;
//        int oow, s, t, lod, ilod;
//        int smax, tmax;
//        int texbase;
//        int c_local;

        /* determine the S/T/LOD values for this texture */
        if (VoodooCommon.TEXMODE_ENABLE_PERSPECTIVE(texMode))
        {
            method.append(  "int lod, oow;\n");
            method.append(  "{\n");
            method.append(  "    int temp, recip, rlog;\n"+
                            "    int interp;\n"+
                            "    int tablePos;\n" +
                            "    boolean neg = false;\n" +
                            "    int lz, exp = 0;\n");
            method.append(  "    if (").append(iterw).append(" < 0) {\n");
            method.append(  "        ").append(iterw).append(" = -").append(iterw).append(";\n" +
                            "        neg = true;\n"+
                            "    }\n");

            method.append(  "    if ((").append(iterw).append(" & 0xffff00000000l)!=0) {\n");
            method.append("        temp = (int)(").append(iterw).append(" >> 16);\n" +
                    "        exp -= 16;\n" +
                    "     } else {\n" +
                    "         temp = (int)").append(iterw).append(";\n" +
                    "     }\n");
            method.append("     if (temp == 0) {\n" +
                    "         lod = 1000 << ").append(VoodooCommon.LOG_OUTPUT_PREC).append(";\n" +
                            "         oow = neg ? 0x80000000 : 0x7fffffff;\n" +
                            "     } else {\n");
            method.append(  "         lz = Integer.numberOfLeadingZeros(temp);\n" +
                            "         temp <<= lz;\n" +
                            "         exp += lz;\n" +
                            "         tablePos = (temp >>> (31 - ").append(VoodooCommon.RECIPLOG_LOOKUP_BITS).append(" - 1)) & ((2 << ").append(VoodooCommon.RECIPLOG_LOOKUP_BITS).append(") - 2);\n" +
                            "         interp = (temp >>> (31 - ").append(VoodooCommon.RECIPLOG_LOOKUP_BITS).append(" - 8)) & 0xff;\n" +
                            "         rlog = (VoodooCommon.voodoo_reciplog[tablePos+1] * (0x100 - interp) + VoodooCommon.voodoo_reciplog[tablePos+3] * interp) >>> 8;\n" +
                            "         recip = (VoodooCommon.voodoo_reciplog[tablePos] * (0x100 - interp) + VoodooCommon.voodoo_reciplog[tablePos+2] * interp) >>> 8;\n" +
                            "         rlog = (rlog + (1 << (").append(VoodooCommon.RECIPLOG_LOOKUP_PREC).append(" - ").append(VoodooCommon.LOG_OUTPUT_PREC).append(" - 1))) >> (").append(VoodooCommon.RECIPLOG_LOOKUP_PREC).append(" - ").append(VoodooCommon.LOG_OUTPUT_PREC).append(");\n"+
                            "         lod = ((exp - (31 - ").append(VoodooCommon.RECIPLOG_INPUT_PREC).append(")) << ").append(VoodooCommon.LOG_OUTPUT_PREC).append(") - rlog;\n" +
                            "         exp += (").append(VoodooCommon.RECIP_OUTPUT_PREC).append(" - ").append(VoodooCommon.RECIPLOG_LOOKUP_PREC).append(") - (31 - ").append(VoodooCommon.RECIPLOG_INPUT_PREC).append(");\n" +
                            "         if (exp < 0)\n"+
                            "             recip >>>= -exp;\n"+
                            "         else\n"+
                            "             recip <<= exp;\n"+
                            "         oow = (neg && recip>0) ? -recip : recip;\n"+
                            "    }\n"+
                            "}\n");
            method.append("int s = (int)(((long)oow * ").append(iters).append(") >>> 29);\n");
            method.append("int t = (int)(((long)oow * ").append(itert).append(") >>> 29);\n");
            method.append("lod += ").append(lodBase).append(";\n");
        }
        else
        {
            method.append("int s = (int)(").append(iters).append(" >> 14);\n");
            method.append("int t = (int)(").append(itert).append(" >> 14);\n");
            method.append("int lod = ").append(lodBase).append(";\n");
        }

        /* clamp W */
        if (VoodooCommon.TEXMODE_CLAMP_NEG_W(texMode))
            method.append("if (").append(iterw).append(" < 0) s = t = 0;\n");

        method.append("lod += tmu.lodbias;\n");
        if (VoodooCommon.TEXMODE_ENABLE_LOD_DITHER(texMode))
            method.append("lod += dither4[dither4Pos+(x & 3)] << 4;\n");
        method.append(  "if (lod < tmu.lodmin)\n" +
                        "    lod = tmu.lodmin;\n" +
                        "if (lod > tmu.lodmax)\n" +
                        "    lod = tmu.lodmax;\n");

        /* now the LOD is in range; if we don't own this LOD, take the next one */
        method.append("int ilod = lod >> 8;\n" +
                      "if (((tmu.lodmask >> ilod) & 1)==0)\n" +
                      "    ilod++;\n");

        method.append(  "int texbase = tmu.lodoffset[ilod];\n" +
                        "int smax = tmu.wmask >> ilod;\n" +
                        "int tmax = tmu.hmask >> ilod;\n");

        int pointSampled = 1;


        if (!VoodooCommon.TEXMODE_MAGNIFICATION_FILTER(texMode) && !VoodooCommon.TEXMODE_MINIFICATION_FILTER(texMode)) {
            pointSampled = 0;
        } else if (VoodooCommon.TEXMODE_MAGNIFICATION_FILTER(texMode) && VoodooCommon.TEXMODE_MINIFICATION_FILTER(texMode)) {
            pointSampled = 2;
        }
        method.append("int c_local;\n");
        if (pointSampled == 1) {
            if (!VoodooCommon.TEXMODE_MAGNIFICATION_FILTER(texMode))
                method.append("if (lod == tmu.lodmin) {\n");
            if (!VoodooCommon.TEXMODE_MINIFICATION_FILTER(texMode)) {
                method.append("if (lod != tmu.lodmin) {\n");
            }
        }
        if (pointSampled <= 1) {
            method.append(  "s >>= ilod + 18;\n" +
                            "t >>= ilod + 18;\n");

            /* clamp/wrap S/T if necessary */
            if (VoodooCommon.TEXMODE_CLAMP_S(texMode))
                CLAMP(method, "s", "0", "smax");

            if (VoodooCommon.TEXMODE_CLAMP_T(texMode))
                CLAMP(method, "t", "0", "tmax");
            method.append(  "s &= smax;\n" +
                            "t &= tmax;\n" +
                            "t *= smax + 1;\n");

            /* fetch texel data */
            if (VoodooCommon.TEXMODE_FORMAT(texMode) < 8) {
                method.append(  "int texel0 = tmu.ram[(texbase + t + s) & tmu.mask] & 0xFF;\n" +
                                "c_local = tmu.lookup[texel0];\n");
            } else {
                method.append(  "int texel0 = VoodooCommon.mem_readw(tmu.ram, (texbase + 2*(t + s)) & tmu.mask);\n");
                if (VoodooCommon.TEXMODE_FORMAT(texMode) >= 10 && VoodooCommon.TEXMODE_FORMAT(texMode) <= 12) {
                    method.append("c_local = tmu.lookup[texel0];\n");
                } else
                    method.append("c_local = (tmu.lookup[texel0 & 0xff] & 0xffffff) | ((texel0 & 0xff00) << 16);\n");
            }
        }
        if (pointSampled == 1)
            method.append("} else {\n");
        if (pointSampled >= 1) {
            method.append(  "s >>= ilod + 10;\n" +
                            "t >>= ilod + 10;\n" +
                            "s -= 0x80;\n" +
                            "t -= 0x80;\n" +
                            "int sfrac = s & tmu.bilinear_mask;\n" +
                            "int tfrac = t & tmu.bilinear_mask;\n" +
                            "s >>= 8;\n" +
                            "t >>= 8;\n" +
                            "int s1 = s + 1;\n" +
                            "int t1 = t + 1;\n");

            /* clamp/wrap S/T if necessary */
            if (VoodooCommon.TEXMODE_CLAMP_S(texMode)) {
                CLAMP(method, "s", "0", "smax");
                CLAMP(method, "s1", "0", "smax");
            }
            if (VoodooCommon.TEXMODE_CLAMP_T(texMode)) {
                CLAMP(method, "t", "0", "tmax");
                CLAMP(method, "t1", "0", "tmax");
            }
            method.append(  "s &= smax;\n"+
                            "s1 &= smax;\n"+
                            "t &= tmax;\n"+
                            "t1 &= tmax;\n"+
                            "t *= smax + 1;\n"+
                            "t1 *= smax + 1;\n");

            /* fetch texel data */
            if (VoodooCommon.TEXMODE_FORMAT(texMode) < 8)
            {
                method.append(  "int texel0 = tmu.ram[(texbase + t + s) & tmu.mask] & 0xFF;\n" +
                                "int texel1 = tmu.ram[(texbase + t + s1) & tmu.mask] & 0xFF;\n" +
                                "int texel2 = tmu.ram[(texbase + t1 + s) & tmu.mask] & 0xFF;\n" +
                                "int texel3 = tmu.ram[(texbase + t1 + s1) & tmu.mask] & 0xFF;\n" +
                                "texel0 = tmu.lookup[texel0];\n" +
                                "texel1 = tmu.lookup[texel1];\n" +
                                "texel2 = tmu.lookup[texel2];\n" +
                                "texel3 = tmu.lookup[texel3];\n");
            }
            else
            {
                method.append(  "int texel0 = VoodooCommon.mem_readw(tmu.ram, (texbase + 2*(t + s)) & tmu.mask);\n"+
                                "int texel1 = VoodooCommon.mem_readw(tmu.ram, (texbase + 2*(t + s1)) & tmu.mask);\n"+
                                "int texel2 = VoodooCommon.mem_readw(tmu.ram, (texbase + 2*(t1 + s)) & tmu.mask);\n"+
                                "int texel3 = VoodooCommon.mem_readw(tmu.ram, (texbase + 2*(t1 + s1)) & tmu.mask);\n");
                if (VoodooCommon.TEXMODE_FORMAT(texMode) >= 10 && VoodooCommon.TEXMODE_FORMAT(texMode) <= 12) {
                    method.append(  "texel0 = tmu.lookup[texel0];\n"+
                                    "texel1 = tmu.lookup[texel1];\n"+
                                    "texel2 = tmu.lookup[texel2];\n"+
                                    "texel3 = tmu.lookup[texel3];\n");
                } else {
                    method.append(  "texel0 = (tmu.lookup[texel0 & 0xff] & 0xffffff) | ((texel0 & 0xff00) << 16);\n"+
                                    "texel1 = (tmu.lookup[texel1 & 0xff] & 0xffffff) | ((texel1 & 0xff00) << 16);\n"+
                                    "texel2 = (tmu.lookup[texel2 & 0xff] & 0xffffff) | ((texel2 & 0xff00) << 16);\n"+
                                    "texel3 = (tmu.lookup[texel3 & 0xff] & 0xffffff) | ((texel3 & 0xff00) << 16);\n");
                }
            }
            method.append("c_local = VoodooCommon.rgba_bilinear_filter(texel0, texel1, texel2, texel3, sfrac, tfrac);\n");
        }
        if (pointSampled == 1)
            method.append("}\n");

        /* select zero/other for RGB */
        if (!VoodooCommon.TEXMODE_TC_ZERO_OTHER(texMode)) {
            method.append("int tr = ");getRegR(method, "texel");method.append(";\n");
            method.append("int tg = ");getRegG(method, "texel");method.append(";\n");
            method.append("int tb = ");getRegB(method, "texel");method.append(";\n");
        } else {
            method.append("int tr = 0, tg = 0, tb = 0;\n");
        }
        /* select zero/other for alpha */
        if (!VoodooCommon.TEXMODE_TCA_ZERO_OTHER(texMode)) {
            method.append("int ta = ");getRegA(method, "texel");method.append(";\n");
        } else {
            method.append("int ta = 0;\n");
        }

        /* potentially subtract c_local */
        if (VoodooCommon.TEXMODE_TC_SUB_CLOCAL(texMode)) {
            method.append("tr -= ");getRegR(method, "c_local");method.append(";\n");
            method.append("tg -= ");getRegG(method, "c_local");method.append(";\n");
            method.append("tb -= ");getRegB(method, "c_local");method.append(";\n");
        }
        if (VoodooCommon.TEXMODE_TCA_SUB_CLOCAL(texMode)) {
            method.append("ta -= ");getRegA(method, "c_local");method.append(";\n");
        }

        if (VoodooCommon.TEXMODE_TC_MSELECT(texMode)==0 && VoodooCommon.TEXMODE_TCA_MSELECT(texMode)==0 && !VoodooCommon.TEXMODE_TC_REVERSE_BLEND(texMode) && !VoodooCommon.TEXMODE_TCA_REVERSE_BLEND(texMode)) {
            System.out.println("  removed textured blend");
        } else {
            /* blend RGB */
            switch (VoodooCommon.TEXMODE_TC_MSELECT(texMode))
            {
                default:    /* reserved */
                case 0:     /* zero */
                    method.append("int blendr = 0, blendg = 0, blendb = 0;\n");
                    break;

                case 1:     /* c_local */
                    method.append("int blendr = ");getRegR(method, "c_local");method.append(";\n");
                    method.append("int blendg = ");getRegG(method, "c_local");method.append(";\n");
                    method.append("int blendb = ");getRegB(method, "c_local");method.append(";\n");
                    break;

                case 2:     /* a_other */
                    method.append("int blendr = ");getRegA(method, "texel");method.append(";\n");
                    method.append("int blendg = blendr;\n");
                    method.append("int blendb = blendr;\n");
                    break;

                case 3:     /* a_local */
                    method.append("int blendr = ");getRegA(method, "c_local");method.append(";\n");
                    method.append("int blendg = blendr;\n");
                    method.append("int blendb = blendr;\n");
                    break;

                case 4:     /* LOD (detail factor) */
                    method.append("int blendr, blendg, blendb;\n");
                    method.append(  "if (tmu.detailbias <= lod) {\n"+
                                    "    blendr = blendg = blendb = 0;\n"+
                                    "} else {\n" +
                                    "    blendr = (((tmu.detailbias - lod) << tmu.detailscale) >> 8);\n" +
                                    "        if (blendr > tmu.detailmax)\n"+
                                    "            blendr = tmu.detailmax;\n"+
                                    "    blendg = blendb = blendr;\n"+
                                    "}\n");
                    break;

                case 5:     /* LOD fraction */
                    method.append(  "int blendr = lod & 0xff;\n"+
                                    "int blendg = blendr, blendb = blendr;\n");
                    break;
            }

            /* blend alpha */
            switch (VoodooCommon.TEXMODE_TCA_MSELECT(texMode))
            {
                default:    /* reserved */
                case 0:     /* zero */
                    method.append("int blenda = 0;\n");
                    break;

                case 1:     /* c_local */
                    method.append("int blenda = ");getRegA(method, "c_local");method.append(";\n");
                    break;

                case 2:     /* a_other */
                    method.append("int blenda = ");getRegA(method, "texel");method.append(";\n");
                    break;

                case 3:     /* a_local */
                    method.append("int blenda = ");getRegA(method, "c_local");method.append(";\n");
                    break;

                case 4:     /* LOD (detail factor) */
                    method.append(  "int blenda;\n"+
                                    "if (tmu.detailbias <= lod) {\n"+
                                    "    blenda = 0;\n" +
                                    "} else {\n"+
                                    "    blenda = (((tmu.detailbias - lod) << tmu.detailscale) >> 8);\n"+
                                    "    if (blenda > tmu.detailmax)\n"+
                                    "        blenda = tmu.detailmax;\n"+
                                    "}\n");
                    break;
                case 5:     /* LOD fraction */
                    method.append("int blenda = lod & 0xff;\n");
                    break;
            }

            /* reverse the RGB blend */
            if (!VoodooCommon.TEXMODE_TC_REVERSE_BLEND(texMode)) {
                method.append(  "blendr ^= 0xff;\n"+
                                "blendg ^= 0xff;\n"+
                                "blendb ^= 0xff;\n");
            }

            /* reverse the alpha blend */
            if (!VoodooCommon.TEXMODE_TCA_REVERSE_BLEND(texMode))
                method.append("blenda ^= 0xff;");

            /* do the blend */
            method.append(  "tr = (tr * (blendr + 1)) >> 8;\n"+
                            "tg = (tg * (blendg + 1)) >> 8;\n"+
                            "tb = (tb * (blendb + 1)) >> 8;\n"+
                            "ta = (ta * (blenda + 1)) >> 8;\n");
        }
        /* add clocal or alocal to RGB */
        switch (VoodooCommon.TEXMODE_TC_ADD_ACLOCAL(texMode))
        {
            case 3:     /* reserved */
            case 0:     /* nothing */
                break;

            case 1:     /* add c_local */
                method.append("tr += ");getRegR(method,"c_local");method.append(";\n");
                method.append("tg += ");getRegG(method,"c_local");method.append(";\n");
                method.append("tb += ");getRegB(method,"c_local");method.append(";\n");
                break;

            case 2:     /* add_alocal */
                method.append("tr += ");getRegR(method,"c_local");method.append(";\n");
                method.append("tg += ");getRegG(method,"c_local");method.append(";\n");
                method.append("tb += ");getRegB(method,"c_local");method.append(";\n");
                break;
        }

        /* add clocal or alocal to alpha */
        if (VoodooCommon.TEXMODE_TCA_ADD_ACLOCAL(texMode)!=0) {
            method.append("ta += ");getRegA(method,"c_local");method.append(";\n");
        }

        /* clamp */
        CLAMP(method, "tr", "0", "255");
        CLAMP(method, "tg", "0", "255");
        CLAMP(method, "tb", "0", "255");
        CLAMP(method, "ta", "0", "255");
        method.append("texel = tb | (tg << 8) | (tr << 16) | (ta << 24);\n");

        /* invert */
        if (VoodooCommon.TEXMODE_TC_INVERT_OUTPUT(texMode))
            method.append("texel ^= 0x00ffffff;\n");
        if (VoodooCommon.TEXMODE_TCA_INVERT_OUTPUT(texMode)) {
            method.append("texel = (texel & 0x00FFFFFF) | (((texel >>> 24) ^ 0xFF) & 0xFF);\n");
        }
    }

    static public void compile(raster_info info, int tmuCount, int colorPath, int alphaMode, int fogMode, int fbzMode, int textureMode0, int textureMode1) {
        if (!Dosbox.allPrivileges)
            return;
        StringBuilder method = new StringBuilder();
        compile(method, tmuCount, colorPath, alphaMode, fogMode, fbzMode, textureMode0, textureMode1);
        raster_info result = compileMethod(method, info);
        // make it live
        if (result != null) {
            info.callback = result.callback;
            System.out.println("compiled "+count+" rasterizers");
            //System.out.println(method.toString());
        }

//        System.out.println("static final public class Rast extends VoodooCommon.raster_info implements poly_draw_scanline_func {\n" +
//                "        public Rast() {\n" +
//                "            this.eff_color_path = "+info.eff_color_path+";\n" +
//                "            this.eff_alpha_mode = "+info.eff_alpha_mode+";\n" +
//                "            this.eff_fog_mode = "+info.eff_fog_mode+";\n" +
//                "            this.eff_fbz_mode = "+info.eff_fbz_mode+";\n" +
//                "            this.eff_tex_mode_0 = "+info.eff_tex_mode_0+";\n" +
//                "            this.eff_tex_mode_1 = "+info.eff_tex_mode_1+";\n" +
//                "            this.callback = this;\n" +
//                "        }\n" +
//                "\n" +
//                "        public void call(short[] dest, int destOffset, int y, poly_extent extent, poly_extra_data extra, int threadid) {");
//        System.out.println(method.toString());
//        System.out.println("        }\n" +
//                "    }\n" +
//                "");
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
        boolean uses_depthPos = VoodooCommon.FBZMODE_AUX_BUFFER_MASK(fbzMode) || VoodooCommon.FBZMODE_ENABLE_DEPTHBUF(fbzMode);
        method.append(          "int startx = extent.startx;\n" +
                                "int stopx = extent.stopx;\n" +
                                "int iterz;\n" +
                                "long iterw;\n" +
                                "long iterw0 = 0, iterw1 = 0;\n" +
                                "long iters0 = 0, iters1 = 0;\n" +
                                "long itert0 = 0, itert1 = 0;\n" +
                                "int destPos;\n" +
                                "int dx, dy;\n" +
                                "int scry=y;\n" +
                                "int x;\n");
        if (uses_depthPos)
            method.append("int depthPos;\n");

        if (VoodooCommon.FBZMODE_Y_ORIGIN(fbzMode)) {
            method.append(      "scry = (v.fbi.yorigin - y) & 0x3ff;\n");
        }

        if (VoodooCommon.FBZMODE_ENABLE_DITHERING(fbzMode)) {
            method.append(      "dither4 = v.dither_matrix_4x4;\n" +
                                "dither4Pos = (y & 3) * 4;\n");
            if (!VoodooCommon.FBZMODE_DITHER_TYPE(fbzMode)) {
                method.append(  "dither = dither4;\n" +
                                "ditherPos = dither4Pos;\n" +
                                "dither_lookup = v.dither4_lookup;\n" +
                                "dither_lookupPos = (y & 3) << 11;\n");
            } else {
                method.append(  "dither = v.dither_matrix_2x2;\n" +
                                "ditherPos = (y & 3) * 4;\n" +
                                "dither_lookup = dither2_lookup;\n" +
                                "dither_lookupPos = (y & 3) << 11;\n");
            }
        }

        if (VoodooCommon.FBZMODE_ENABLE_CLIPPING(fbzMode)) {
            method.append(      "if (scry < ((v.reg[").append(VoodooCommon.clipLowYHighY).append("] >> 16) & 0x3ff) || scry >= (v.reg[").append(VoodooCommon.clipLowYHighY).append("] & 0x3ff)) {\n" +
                                "    stats.pixels_in += stopx - startx;\n" +
                                "    stats.clip_fail += stopx - startx;\n" +
                                "    return;\n" +
                                "}\n" +
                                "int tempclip = (v.reg[").append(VoodooCommon.clipLeftRight).append("] >> 16) & 0x3ff;\n" +
                                "if (startx < tempclip) {\n" +
                                "    stats.pixels_in += tempclip - startx;\n" +
                                "    v.stats.total_clipped += tempclip - startx;\n" +
                                "    startx = tempclip;\n" +
                                "}\n" +
                                "tempclip = v.reg[").append(VoodooCommon.clipLeftRight).append("] & 0x3ff;\n" +
                                "if (stopx >= tempclip) {\n" +
                                "    stats.pixels_in += stopx - tempclip;\n" +
                                "    v.stats.total_clipped += stopx - tempclip;\n" +
                                "    stopx = tempclip - 1;\n" +
                                "}\n");
        }
        method.append(          "destPos = destOffset+scry * v.fbi.rowpixels;\n");
        if (uses_depthPos)
            method.append(      "depthPos = (v.fbi.auxoffs != -1) ? (v.fbi.auxoffs / 2 + scry * v.fbi.rowpixels) : -1;\n");
        method.append(          "dx = startx - (extra.ax >> 4);\n" +
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



        method.append("for (x = startx; x < stopx; x++");
        method.append(", iterr += extra.drdx, iterg += extra.dgdx, iterb += extra.dbdx, itera += extra.dadx, iterz += extra.dzdx, iterw += extra.dwdx");
        if (tmuCount >= 1) {
            method.append(", iterw0 += extra.dw0dx, iters0 += extra.ds0dx, itert0 += extra.dt0dx");
        }
        if (tmuCount >= 2) {
            method.append(", iterw1 += extra.dw1dx, iters1 += extra.ds1dx, itert1 += extra.dt1dx");
        }
        method.append("){\n" +
                "int texel = 0;\n" +
                "int iterargb = 0;\n" +
                "int r, g, b;\n");
        boolean uses_a = (VoodooCommon.FBZMODE_AUX_BUFFER_MASK(fbzMode) && VoodooCommon.FBZMODE_ENABLE_ALPHA_PLANES(fbzMode)) ||
                    VoodooCommon.ALPHAMODE_SRCRGBBLEND(alphaMode)==1 || VoodooCommon.ALPHAMODE_SRCRGBBLEND(alphaMode)==5 || VoodooCommon.ALPHAMODE_SRCRGBBLEND(alphaMode)==15 ||
                    VoodooCommon.ALPHAMODE_DSTRGBBLEND(alphaMode)==1 || VoodooCommon.ALPHAMODE_DSTRGBBLEND(alphaMode)==5;
        if (uses_a)
            method.append("int a;\n");
        method.append("stats.pixels_in++;\n");

        if (VoodooCommon.FBZMODE_ENABLE_STIPPLE(fbzMode)) {
            /* rotate mode */
            if (!VoodooCommon.FBZMODE_STIPPLE_PATTERN(fbzMode)) {
                method.append(  "v.reg[").append(VoodooCommon.stipple).append("] = (v.reg[").append(VoodooCommon.stipple).append("] << 1) | (v.reg[").append(VoodooCommon.stipple).append("] >> 31);\n");
                method.append(  "if ((v.reg[").append(VoodooCommon.stipple).append("] & 0x80000000) == 0) {\n" +
                                "    v.stats.total_stippled++;\n" +
                                "    continue;\n" +
                                "}\n");
            } else { /* pattern mode */
                method.append(  "if (((reg[").append(VoodooCommon.stipple).append("] >> (((y & 3) << 3) | (~x & 7))) & 1) == 0) {\n" +
                        "    v.stats.total_stippled++;\n" +
                        "    continue;\n" +
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
                                "    int temp = (int)iterw;\n" +
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
                method.append(  "int ");CLAMPED_Z(method, "iterz", colorPath, "depthval");
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
            method.append(      "depthval += (short)v.reg[").append(VoodooCommon.zaColor).append("];\n");
            CLAMP(method, "depthval", "0", "0xffff");
        }

        if (VoodooCommon.FBZMODE_ENABLE_DEPTHBUF(fbzMode)) {
            if (!VoodooCommon.FBZMODE_DEPTH_SOURCE_COMPARE(fbzMode))
                method.append(  "int depthsource = depthval;\n");
            else
                method.append(  "int depthsource = v.reg[").append(VoodooCommon.zaColor).append("] & 0xFFFF;\n");

            /* test against the depth buffer */
            switch (VoodooCommon.FBZMODE_DEPTH_FUNCTION(fbzMode))
            {
                case 0:     /* depthOP = never */
                    method.append(  "stats.zfunc_fail++;\n" +
                                    "continue;\n");
                    break;
                case 1:     /* depthOP = less than */
                    method.append(  "if (depthsource >= (v.fbi.ram[depthPos+x] & 0xFFFF)) {\n" +
                                    "    stats.zfunc_fail++;\n" +
                                    "    continue;\n" +
                                    "}\n");
                    break;
                case 2:     /* depthOP = equal */
                    method.append(  "if (depthsource != (v.fbi.ram[depthPos+x] & 0xFFFF)) {\n" +
                                    "    stats.zfunc_fail++;\n" +
                                    "    continue;\n" +
                                    "}\n");
                    break;
                case 3:     /* depthOP = less than or equal */
                    method.append(  "if (depthsource > (v.fbi.ram[depthPos+x] & 0xFFFF)) {\n" +
                                    "    stats.zfunc_fail++;\n" +
                                    "    continue;\n" +
                                    "}\n");
                    break;
                case 4:     /* depthOP = greater than */
                    method.append(  "if (depthsource <= (v.fbi.ram[depthPos+x] & 0xFFFF)) {\n" +
                                    "    stats.zfunc_fail++;\n" +
                                    "    continue;\n" +
                                    "}\n");
                    break;
                case 5:     /* depthOP = not equal */
                    method.append(  "if (depthsource == (v.fbi.ram[depthPos+x] & 0xFFFF)) {\n" +
                                    "    stats.zfunc_fail++;\n" +
                                    "    continue;\n" +
                                    "}\n");
                    break;
                case 6:     /* depthOP = greater than or equal */
                    method.append(  "if (depthsource < (v.fbi.ram[depthPos+x] & 0xFFFF)) {\n" +
                                    "    stats.zfunc_fail++;\n" +
                                    "    continue;\n" +
                                    "}\n");
                    break;
                case 7:     /* depthOP = always */
                    break;
            }
        }

        if (tmuCount >= 2) {
            method.append("if (v.tmu[1].lodmin < (8 << 8)) {\n");
            method.append("  tmu_state tmu = v.tmu[1];\n");
            TEXTURE_PIPELINE(method, textureMode1, "extra.lodbase1", "iters1", "itert1", "iterw1");
            method.append("}\n");
        }

        if (tmuCount >= 1) {
            method.append("if (v.tmu[0].lodmin < (8 << 8)) {\n");
            method.append("    if (((v.reg[v.tmu[0].reg+VoodooCommon.trexInit1] >> 18) & 1)==0){");
            method.append("        tmu_state tmu = v.tmu[0];\n");
            TEXTURE_PIPELINE(method, textureMode0, "extra.lodbase0", "iters0", "itert0", "iterw0");
            method.append("    } else {\n"+
                          "        texel = 64;\n"+
                          "    }\n"+
                          "}\n");

        }

        boolean uses_iterargb =  VoodooCommon.FBZCP_CC_RGBSELECT(colorPath)==0 ||
                        VoodooCommon.FBZCP_CC_ASELECT(colorPath)==0 ||
                        VoodooCommon.FBZCP_CCA_LOCALSELECT(colorPath)==0 ||
                        VoodooCommon.FBZCP_CC_LOCALSELECT_OVERRIDE(colorPath) || (!VoodooCommon.FBZCP_CC_LOCALSELECT_OVERRIDE(colorPath) && !VoodooCommon.FBZCP_CC_LOCALSELECT(colorPath)) ||
                        (VoodooCommon.FOGMODE_ENABLE_FOG(fogMode) && !VoodooCommon.FOGMODE_FOG_CONSTANT(fogMode) && VoodooCommon.FOGMODE_FOG_ZALPHA(fogMode)==1);
        if (uses_iterargb) {
            method.append(  "int ir, ig, ib, ia;\n"+
                            "int cr = iterr >> 12;\n"+
                            "int cg = iterg >> 12;\n"+
                            "int cb = iterb >> 12;\n"+
                            "int ca = itera >> 12;\n");

            if (!VoodooCommon.FBZCP_RGBZW_CLAMP(colorPath)) {
                method.append(  "cr &= 0xfff;\n"+
                                "ir = cr & 0xFF;\n"+
                                "if (cr == 0xfff) ir = 0;\n"+
                                "else if (cr == 0x100) ir = 0xff;\n"+
                                "cg &= 0xfff;\n"+
                                "ig = cg & 0xFF;\n"+
                                "if (cg == 0xfff) ig = 0;\n"+
                                "else if (cg == 0x100) ig = 0xff;\n"+
                                "cb &= 0xfff;\n"+
                                "ib = cb & 0xFF;\n"+
                                "if (cb == 0xfff) ib = 0;\n"+
                                "else if (cb == 0x100) ib = 0xff;\n"+
                                "ca &= 0xfff;\n"+
                                "ia = ca & 0xFF;\n"+
                                "if (ca == 0xfff) ia = 0;\n"+
                                "else if (ca == 0x100) ia = 0xff;\n");
            } else {
                method.append(  "ir = (cr < 0) ? 0 : (cr > 0xff) ? 0xff : cr;\n"+
                                "ig = (cg < 0) ? 0 : (cg > 0xff) ? 0xff : cg;\n"+
                                "ib = (cb < 0) ? 0 : (cb > 0xff) ? 0xff : cb;\n"+
                                "ia = (ca < 0) ? 0 : (ca > 0xff) ? 0xff : ca;\n");
            }
            method.append("iterargb = ib | (ig << 8) | (ir << 16) | (ia << 24);\n");
        }
        switch (VoodooCommon.FBZCP_CC_RGBSELECT(colorPath))
        {
            case 0:     /* iterated RGB */
                method.append("int c_other = iterargb;\n");
                break;
            case 1:     /* texture RGB */
                method.append("int c_other = texel;\n");
                break;
            case 2:     /* color1 RGB */
                method.append("int c_other = v.reg[VoodooCommon.color1];\n");
                break;
            default:    /* reserved */
                method.append("int c_other = 0;\n");
                break;
        }

        if (VoodooCommon.FBZMODE_ENABLE_CHROMAKEY(fbzMode)) {
            method.append("if (!v.APPLY_CHROMAKEY(stats, ");method.append(fbzMode);method.append(", c_other)) continue;\n");
        }

        /* compute a_other */
        switch (VoodooCommon.FBZCP_CC_ASELECT(colorPath))
        {
            case 0:     /* iterated alpha */
                method.append("c_other = (c_other & 0x00FFFFFF) | (iterargb & 0xFF000000);\n");
                break;

            case 1:     /* texture alpha */
                method.append("c_other = (c_other & 0x00FFFFFF) | (texel & 0xFF000000);\n");
                break;

            case 2:     /* color1 alpha */
                method.append("c_other = (c_other & 0x00FFFFFF) | (v.reg[VoodooCommon.color1] & 0xFF000000);\n");
                break;

            default:    /* reserved */
                method.append("c_other = (c_other & 0x00FFFFFF);\n");
                break;
        }
        if (VoodooCommon.FBZMODE_ENABLE_ALPHA_MASK(fbzMode) || VoodooCommon.ALPHAMODE_ALPHATEST(alphaMode)) {
            method.append("int c_other_a = ");getRegA(method, "c_other");method.append(";\n");
        }
        if (VoodooCommon.FBZMODE_ENABLE_ALPHA_MASK(fbzMode)) {
            method.append(  "if ((c_other_a & 1) == 0) {\n"+
                            "    stats.afunc_fail++;\n"+
                            "    continue;\n"+
                            "}\n");
        }
        if (VoodooCommon.ALPHAMODE_ALPHATEST(alphaMode)) {
            method.append("if (!v.APPLY_ALPHATEST(stats, ").append(alphaMode).append(", c_other_a)) continue;\n");
        }
        boolean uses_c_local = VoodooCommon.FBZCP_CC_SUB_CLOCAL(colorPath) || VoodooCommon.FBZCP_CCA_SUB_CLOCAL(colorPath) ||
                VoodooCommon.FBZCP_CC_MSELECT(colorPath)==1 || VoodooCommon.FBZCP_CC_MSELECT(colorPath)==3 ||
                VoodooCommon.FBZCP_CCA_MSELECT(colorPath)==1 || VoodooCommon.FBZCP_CCA_MSELECT(colorPath)==3 ||
                VoodooCommon.FBZCP_CC_ADD_ACLOCAL(colorPath)==1 || VoodooCommon.FBZCP_CC_ADD_ACLOCAL(colorPath)==2 ||
                VoodooCommon.FBZCP_CCA_ADD_ACLOCAL(colorPath)!=0;

        if (uses_c_local) {
            if (!VoodooCommon.FBZCP_CC_LOCALSELECT_OVERRIDE(colorPath)) {
                if (!VoodooCommon.FBZCP_CC_LOCALSELECT(colorPath))
                    method.append("int c_local = iterargb;\n");
                else
                    method.append("int c_local = v.reg[VoodooCommon.color0];\n");
            } else {
                method.append(  "int c_local;\n"+
                                "if ((texel & 0x80000000)==0)\n"+
                                "    c_local = iterargb;\n"+
                                "else\n"+
                                "    c_local = v.reg[VoodooCommon.color0];\n");
            }

            /* compute a_local */
                switch (VoodooCommon.FBZCP_CCA_LOCALSELECT(colorPath))
            {
                default:
                case 0:     /* iterated alpha */
                    method.append("c_local = (c_local & 0x00FFFFFF) | (iterargb & 0xFF000000);\n");
                    break;

                case 1:     /* color0 alpha */
                    method.append("c_local = (c_local & 0x00FFFFFF) | (v.reg[VoodooCommon.color0] & 0xFF000000);\n");
                    break;

                case 2:     /* clamped iterated Z[27:20] */
                {
                    method.append("int temp = ");
                    CLAMPED_Z(method, "iterz", colorPath, "temp");
                    method.append("c_local = (c_local & 0x00FFFFFF) | (temp << 24);\n");
                    break;
                }

                case 3:     /* clamped iterated W[39:32] */
                {
                    method.append("int temp = ");
                    CLAMPED_W(method, "iterw", colorPath, "temp");
                    method.append("c_local = (c_local & 0x00FFFFFF) | (temp << 24);\n");
                    break;
                }
            }
        }
        /* select zero or c_other */
        if (!VoodooCommon.FBZCP_CC_ZERO_OTHER(colorPath)) {
            method.append("r = ");getRegR(method,"c_other");method.append(";\n");
            method.append("g = ");getRegG(method,"c_other");method.append(";\n");
            method.append("b = ");getRegB(method,"c_other");method.append(";\n");
        } else {
            method.append("r = g = b = 0;\n");
        }

        if (uses_a) {
            if (!VoodooCommon.FBZCP_CCA_ZERO_OTHER(colorPath)) {
                method.append("a = ");getRegA(method,"c_other");method.append(";\n");
            } else {
                method.append("a = 0;\n");
            }
        }

        /* subtract c_local */
        if (VoodooCommon.FBZCP_CC_SUB_CLOCAL(colorPath)) {
            method.append("r -= ");getRegR(method,"c_local");method.append(";\n");
            method.append("g -= ");getRegG(method,"c_local");method.append(";\n");
            method.append("b -= ");getRegB(method,"c_local");method.append(";\n");
        }

        if (uses_a) {
            if (VoodooCommon.FBZCP_CCA_SUB_CLOCAL(colorPath)) {
                method.append("a -= ");getRegA(method,"c_local");method.append(";\n");
            }
        }
        if (VoodooCommon.FBZCP_CC_MSELECT(colorPath)==0 && VoodooCommon.FBZCP_CCA_MSELECT(colorPath)==0 && !VoodooCommon.FBZCP_CC_REVERSE_BLEND(colorPath) && (!VoodooCommon.FBZCP_CCA_REVERSE_BLEND(colorPath) || !uses_a)) {
            System.out.println("  removed color path blend");
        } else {
            /* blend RGB */
            switch (VoodooCommon.FBZCP_CC_MSELECT(colorPath))
            {
                default:    /* reserved */
                case 0:     /* 0 */
                    method.append("int blendr = 0, blendg = 0, blendb = 0;\n");
                    break;

                case 1:     /* c_local */
                    method.append("int blendr = ");getRegR(method,"c_local");method.append(";\n");
                    method.append("int blendg = ");getRegG(method,"c_local");method.append(";\n");
                    method.append("int blendb = ");getRegB(method,"c_local");method.append(";\n");
                    break;

                case 2:     /* a_other */
                    method.append(  "int blendr = ");getRegA(method,"c_other");method.append(";\n"+
                                    "int blendb = blendr, blendg = blendr;\n");
                    break;

                case 3:     /* a_local */
                    method.append(  "int blendr = ");getRegA(method,"c_local");method.append(";\n"+
                                    "int blendb = blendr, blendg = blendr;\n");
                    break;

                case 4:     /* texture alpha */
                    method.append(  "int blendr = ");getRegA(method,"texel");method.append(";\n"+
                                    "int blendb = blendr, blendg = blendr;\n");
                    break;

                case 5:     /* texture RGB (Voodoo 2 only) */
                    method.append("int blendr = ");getRegR(method,"texel");method.append(";\n");
                    method.append("int blendg = ");getRegG(method,"texel");method.append(";\n");
                    method.append("int blendb = ");getRegB(method,"texel");method.append(";\n");
                    break;
            }

            if (uses_a) {
                switch (VoodooCommon.FBZCP_CCA_MSELECT(colorPath))
                {
                    default:    /* reserved */
                    case 0:     /* 0 */
                        method.append("int blenda = 0;\n");
                        break;

                    case 1:     /* a_local */
                        method.append("int blenda = ");getRegA(method,"c_local");method.append(";\n");
                        break;

                    case 2:     /* a_other */
                        method.append("int blenda = ");getRegA(method,"c_other");method.append(";\n");
                        break;

                    case 3:     /* a_local */
                        method.append("int blenda = ");getRegA(method,"c_local");method.append(";\n");
                        break;

                    case 4:     /* texture alpha */
                        method.append("int blenda = ");getRegA(method,"texel");method.append(";\n");
                        break;
                }
            }
            /* reverse the RGB blend */
            if (!VoodooCommon.FBZCP_CC_REVERSE_BLEND(colorPath)) {
                method.append(  "blendr ^= 0xff;\n"+
                                "blendg ^= 0xff;\n"+
                                "blendb ^= 0xff;\n");
            }

            if (uses_a) {
                if (!VoodooCommon.FBZCP_CCA_REVERSE_BLEND(colorPath)) {
                    method.append("blenda ^= 0xff;\n");
                }
            }

            /* do the blend */
            method.append(  "r = (r * (blendr + 1)) >> 8;\n"+
                            "g = (g * (blendg + 1)) >> 8;\n"+
                            "b = (b * (blendb + 1)) >> 8;\n");
            if (uses_a)
                method.append("a = (a * (blenda + 1)) >> 8;\n");
        }

        /* add clocal or alocal to RGB */
        switch (VoodooCommon.FBZCP_CC_ADD_ACLOCAL(colorPath))
        {
            case 3:     /* reserved */
            case 0:     /* nothing */
                break;

            case 1:     /* add c_local */
                method.append("r += ");getRegR(method,"c_local");method.append(";\n");
                method.append("g += ");getRegG(method,"c_local");method.append(";\n");
                method.append("b += ");getRegB(method,"c_local");method.append(";\n");
                break;

            case 2:     /* add_alocal */
                method.append("r += ");getRegA(method,"c_local");method.append(";\n");
                method.append("g += ");getRegA(method,"c_local");method.append(";\n");
                method.append("b += ");getRegA(method,"c_local");method.append(";\n");
                break;
        }

        if (uses_a) {
            if (VoodooCommon.FBZCP_CCA_ADD_ACLOCAL(colorPath)!=0) {
                method.append("a += ");getRegA(method,"c_local");method.append(";\n");
            }
        }

        /* clamp */
        CLAMP(method, "r", "0x00", "0xff");
        CLAMP(method, "g", "0x00", "0xff");
        CLAMP(method, "b", "0x00", "0xff");
        if (uses_a) {
            CLAMP(method, "a", "0x00", "0xff");
        }

        /* invert */
        if (VoodooCommon.FBZCP_CC_INVERT_OUTPUT(colorPath)) {
            method.append(  "r ^= 0xff;\n"+
                            "g ^= 0xff;\n"+
                            "b ^= 0xff;\n");
        }
        if (uses_a) {
            if (VoodooCommon.FBZCP_CCA_INVERT_OUTPUT(colorPath)) {
                method.append("a ^= 0xff;\n");
            }
        }
        if (VoodooCommon.ALPHAMODE_DSTRGBBLEND(alphaMode)==15) {
            method.append(  "int prefogr = r;\n"+
                            "int prefogg = g;\n"+
                            "int prefogb = b;\n");
        }

        // APPLY_FOGGING(VV, FOGMODE, FBZCOLORPATH, XX, DITHER4, r, g, b, ITERZ, ITERW, ITERAXXX);
        if (VoodooCommon.FOGMODE_ENABLE_FOG(fogMode)) {
            method.append(  "int fogcolor = v.reg[VoodooCommon.fogColor];\n");

            /* constant fog bypasses everything else */
            if (VoodooCommon.FOGMODE_FOG_CONSTANT(fogMode)) {
                method.append("int fr = ");getRegR(method,"fogcolor");method.append(";\n");
                method.append("int fg = ");getRegG(method,"fogcolor");method.append(";\n");
                method.append("int fb = ");getRegB(method,"fogcolor");method.append(";\n");
            }
            /* non-constant fog comes from several sources */
            else {
                method.append("int fogblend = 0;\n");

                /* if fog_add is zero, we start with the fog color */
                if (!VoodooCommon.FOGMODE_FOG_ADD(fogMode)) {
                    method.append("int fr = ");getRegR(method,"fogcolor");method.append(";\n");
                    method.append("int fg = ");getRegG(method,"fogcolor");method.append(";\n");
                    method.append("int fb = ");getRegB(method,"fogcolor");method.append(";\n");
                } else {
                    method.append("int fr = 0, fg = 0, fb = 0;\n");
                }

                /* if fog_mult is zero, we subtract the incoming color */
                if (!VoodooCommon.FOGMODE_FOG_MULT(fogMode)) {
                    method.append(  "fr -= r;\n"+
                                    "fg -= g;\n"+
                                    "fb -= b;\n");
                }

                /* fog blending mode */
                switch (VoodooCommon.FOGMODE_FOG_ZALPHA(fogMode)) {
                    case 0:     /* fog table */ {
                        method.append(  "int delta = v.fbi.fogdelta[wfloat >> 10];\n"+
                                        "int deltaval = (delta & v.fbi.fogdelta_mask) * ((wfloat >> 2) & 0xff);\n");
                        if (VoodooCommon.FOGMODE_FOG_ZONES(fogMode)) {
                            method.append(  "if (delta & 2) != 0)\n"+
                                            "    deltaval = -deltaval;\n");
                        }
                        method.append("deltaval >>= 6;\n");
                        if (VoodooCommon.FOGMODE_FOG_DITHER(fogMode))
                            method.append("deltaval += dither4[dither4Pos + (x & 3)];\n");
                        method.append(  "deltaval >>= 4;\n"+
                                        "fogblend = v.fbi.fogblend[wfloat >> 10] + deltaval;\n");
                        break;
                    }
                    case 1:     /* iterated A */
                        method.append("fogblend = iterargb >>> 24;\n");
                        break;
                    case 2:     /* iterated Z */
                        CLAMPED_Z(method, "iterz", colorPath, "fogblend");
                        method.append("fogblend >>= 8;\n");
                        break;
                    case 3:     /* iterated W - Voodoo 2 only */
                        CLAMPED_W(method, "iterw", colorPath, "fogblend");
                        break;
                }

                /* perform the blend */
                method.append(  "fogblend++;\n"+
                                "fr = (fr * fogblend) >> 8;\n" +
                                "fg = (fg * fogblend) >> 8;\n" +
                                "fb = (fb * fogblend) >> 8;\n");
            }

            /* if fog_mult is 0, we add this to the original color */
            if (!VoodooCommon.FOGMODE_FOG_MULT(fogMode)) {
                method.append(  "r += fr;\n" +
                                "g += fg;\n" +
                                "b += fb;\n");
            } else {
                method.append(  "r = fr;\n"+
                                "g = fg;\n"+
                                "b = fb;\n");
            }

            /* clamp */
            CLAMP(method, "r", "0", "0xff");
            CLAMP(method, "g", "0", "0xff");
            CLAMP(method, "b", "0", "0xff");
        }

        /* perform alpha blending */
        if (VoodooCommon.ALPHAMODE_ALPHABLEND(alphaMode)) {
            method.append(  "int dpix = dest[destPos+x] & 0xFFFF;\n"+
                            "int dr = (dpix >> 8) & 0xf8;\n"+
                            "int dg = (dpix >> 3) & 0xfc;\n"+
                            "int db = (dpix << 3) & 0xf8;\n"+
                            "int da = ").append(VoodooCommon.FBZMODE_ENABLE_ALPHA_PLANES(fbzMode) ? "dest[destPos+x] & 0xFFFF\n" : "0xff;\n"+
                            "int sr = r;\n"+
                            "int sb = b;\n"+
                            "int sg = g;\n");
            if (uses_a)
                method.append("int sa = a;\n");

            /* apply dither subtraction */
            if (VoodooCommon.FBZMODE_ALPHA_DITHER_SUBTRACT(fbzMode)) {
                method.append(  "int dith = dither[ditherPos+(x & 3)];\n"+
                                "dr = ((dr << 1) + 15 - dith) >> 1;\n"+
                                "dg = ((dg << 2) + 15 - dith) >> 2;\n"+
                                "db = ((db << 1) + 15 - dith) >> 1;\n");
            }

            /* compute source portion */
            switch (VoodooCommon.ALPHAMODE_SRCRGBBLEND(alphaMode))
            {
                default:    /* reserved */
                case 0:     /* AZERO */
                    method.append("r = g = b = 0;\n");
                    break;
                case 1:     /* ASRC_ALPHA */
                    method.append(  "r = (sr * (sa + 1)) >> 8;\n"+
                                    "g = (sg * (sa + 1)) >> 8;\n"+
                                    "b = (sb * (sa + 1)) >> 8;\n");
                    break;
                case 2:     /* A_COLOR */
                    method.append(  "r = (sr * (dr + 1)) >> 8;\n"+
                                    "g = (sg * (dg + 1)) >> 8;\n"+
                                    "b = (sb * (db + 1)) >> 8;\n");
                    break;
                case 3:     /* ADST_ALPHA */
                    method.append(  "r = (sr * (da + 1)) >> 8;\n"+
                                    "g = (sg * (da + 1)) >> 8;\n"+
                                    "b = (sb * (da + 1)) >> 8;\n");
                    break;
                case 4:     /* AONE */
                    break;
                case 5:     /* AOMSRC_ALPHA */
                    method.append(  "r = (sr * (0x100 - sa)) >> 8;\n"+
                                    "g = (sg * (0x100 - sa)) >> 8;\n"+
                                    "b = (sb * (0x100 - sa)) >> 8;\n");
                    break;
                case 6:     /* AOM_COLOR */
                    method.append(  "r = (sr * (0x100 - dr)) >> 8;\n"+
                                    "g = (sg * (0x100 - dg)) >> 8;\n"+
                                    "b = (sb * (0x100 - db)) >> 8;\n");
                    break;
                case 7:     /* AOMDST_ALPHA */
                    method.append(  "r = (sr * (0x100 - da)) >> 8;\n"+
                                    "g = (sg * (0x100 - da)) >> 8;\n"+
                                    "b = (sb * (0x100 - da)) >> 8;\n");
                    break;
                case 15:    /* ASATURATE */
                    method.append(  "int ta = (sa < (0x100 - da)) ? sa : (0x100 - da);\n"+
                                    "r = (sr * (ta + 1)) >> 8;\n"+
                                    "g = (sg * (ta + 1)) >> 8;\n"+
                                    "b = (sb * (ta + 1)) >> 8;\n");
                    break;
            }

            /* add in dest portion */
            switch (VoodooCommon.ALPHAMODE_DSTRGBBLEND(alphaMode))
            {
                default:    /* reserved */
                case 0:     /* AZERO */
                    break;
                case 1:     /* ASRC_ALPHA */
                    method.append(  "r += (dr * (sa + 1)) >> 8;\n"+
                                    "g += (dg * (sa + 1)) >> 8;\n"+
                                    "b += (db * (sa + 1)) >> 8;\n");
                    break;
                case 2:     /* A_COLOR */
                    method.append(  "r += (dr * (sr + 1)) >> 8;\n"+
                                    "g += (dg * (sg + 1)) >> 8;\n"+
                                    "b += (db * (sb + 1)) >> 8;\n");
                    break;
                case 3:     /* ADST_ALPHA */
                    method.append(  "r += (dr * (da + 1)) >> 8;\n"+
                                    "g += (dg * (da + 1)) >> 8;\n"+
                                    "b += (db * (da + 1)) >> 8;\n");
                    break;
                case 4:     /* AONE */
                    method.append(  "r += dr;\n"+
                                    "g += dg;\n"+
                                    "b += db;\n");
                    break;
                case 5:     /* AOMSRC_ALPHA */
                    method.append(  "r += (dr * (0x100 - sa)) >> 8;\n"+
                                    "g += (dg * (0x100 - sa)) >> 8;\n"+
                                    "b += (db * (0x100 - sa)) >> 8;\n");
                    break;
                case 6:     /* AOM_COLOR */
                    method.append(  "r += (dr * (0x100 - sr)) >> 8;\n"+
                                    "g += (dg * (0x100 - sg)) >> 8;\n"+
                                    "b += (db * (0x100 - sb)) >> 8;\n");
                    break;
                case 7:     /* AOMDST_ALPHA */
                    method.append(  "r += (dr * (0x100 - da)) >> 8;\n"+
                                    "g += (dg * (0x100 - da)) >> 8;\n"+
                                    "b += (db * (0x100 - da)) >> 8;\n");
                    break;
                case 15:    /* A_COLORBEFOREFOG */
                    method.append(  "r += (dr * (prefogr + 1)) >> 8;\n"+
                                    "g += (dg * (prefogg + 1)) >> 8;\n"+
                                    "b += (db * (prefogb + 1)) >> 8;\n");
                    break;
            }
            if (uses_a) {
                if (VoodooCommon.ALPHAMODE_SRCALPHABLEND(alphaMode) == 4)
                    method.append("a = sa;\n");
                else
                    method.append("a = 0;\n");
                if (VoodooCommon.ALPHAMODE_DSTALPHABLEND(alphaMode) == 4)
                    method.append("a += da;\n");
            }
            /* clamp */
            CLAMP(method, "r", "0", "0xff");
            CLAMP(method, "g", "0", "0xff");
            CLAMP(method, "b", "0", "0xff");
            if (uses_a) {
                CLAMP(method, "a", "0", "0xff");
            }
        }

        /* write to framebuffer */
        if (VoodooCommon.FBZMODE_RGB_BUFFER_MASK(fbzMode)) {
            if (VoodooCommon.FBZMODE_ENABLE_DITHERING(fbzMode)) {
                method.append(  "int dithPos = dither_lookupPos + ((x & 3) << 1);\n"+
                                "r = dither_lookup[dithPos+(r << 3) + 0];\n"+
                                "g = dither_lookup[dithPos+(g << 3) + 1];\n"+
                                "b = dither_lookup[dithPos+(b << 3) + 0];\n");
            } else {
                method.append(  "r >>>= 3;\n"+
                                "g >>>= 2;\n"+
                                "b >>>= 3;\n");
            }
            method.append("dest[destPos+x] = (short)((r << 11) | (g << 5) | b);\n");
        }

        /* write to aux buffer */
        if (VoodooCommon.FBZMODE_AUX_BUFFER_MASK(fbzMode)) {
            method.append("if (depthPos!=-1)\n");
            if (!VoodooCommon.FBZMODE_ENABLE_ALPHA_PLANES(fbzMode))
                method.append("v.fbi.ram[depthPos+x] = (short)depthval;\n");
            else
                method.append("v.fbi.ram[depthPos+x] = (short)a;\n");
        }
        method.append(  "stats.pixels_out++;\n");

        // close loop
        method.append("}\n");
    }

    static private ClassPool pool = new ClassPool(true);

    static {
        pool.importPackage("jdos.hardware.mame.VoodooCommon");
        pool.importPackage("jdos.hardware.mame.Poly");
        pool.importPackage("jdos.hardware.mame.poly_extent");
        pool.importPackage("jdos.hardware.mame.poly_extra_data");
        pool.importPackage("jdos.hardware.mame.stats_block");
        pool.importPackage("jdos.hardware.mame.tmu_state");
        pool.insertClassPath(new ClassPath() {
            public InputStream openClassfile(String s) throws NotFoundException {
                if (s.startsWith("jdos.")) {
                    s = "/" + s.replace('.', '/') + ".class";
                    return Dosbox.class.getResourceAsStream(s.substring(6));
                }
                return null;
            }

            public URL find(String s) {
                if (s.startsWith("jdos.")) {
                    s = "/" + s.replace('.', '/') + ".class";
                    return Dosbox.class.getResource(s.substring(6));
                }
                return null;
            }

            public void close() {
            }
        });
    }

    static private int count;
    static private raster_info compileMethod(StringBuilder method, raster_info info) {
       //System.out.println(method.toString());
       try {
           String className = "Rasterizer" + (count++);

           CtClass codeBlock = pool.makeClass(className);
           codeBlock.setSuperclass(pool.getCtClass("jdos.hardware.mame.raster_info"));
           codeBlock.addInterface(pool.getCtClass("jdos.hardware.mame.poly_draw_scanline_func"));
           method.append("}");
           CtMethod m = CtNewMethod.make("public void call(short[] dest, int destOffset, int y, poly_extent extent, poly_extra_data extra, int threadid) {" + method.toString(), codeBlock);
           codeBlock.addMethod(m);

          String constructor =
          "{this.eff_color_path = "+info.eff_color_path+";" +
          "this.eff_alpha_mode = "+info.eff_alpha_mode+";" +
          "this.eff_fog_mode = "+info.eff_fog_mode+";" +
          "this.eff_fbz_mode = "+info.eff_fbz_mode+";" +
          "this.eff_tex_mode_0 = "+info.eff_tex_mode_0+";" +
          "this.eff_tex_mode_1 = "+info.eff_tex_mode_1+";" +
          "this.callback = this;}";

           CtConstructor c = CtNewConstructor.make("public "+className+"()"+constructor, codeBlock);
           codeBlock.addConstructor(c);

           // Make the dynamic class belong to its own class loader so that when we
           // release the raster block the class and class loader will be unloaded
           URLClassLoader cl = (URLClassLoader) codeBlock.getClass().getClassLoader();
           cl = URLClassLoader.newInstance(cl.getURLs(), cl);
           Class clazz = codeBlock.toClass(cl, null);
           raster_info result = (raster_info) clazz.newInstance();
           if (saveClasses) {
               savedClasses.add(new SaveInfo(info, codeBlock.toBytecode()));
           }
           codeBlock.detach();
           return result;
       } catch (Exception e) {
           System.out.println(method.toString());
           e.printStackTrace();
       }
       return null;
   }
}
