package jdos.hardware.qemu;

import jdos.util.IntRef;

public class Atapi extends Internal {
    static void padstr8(byte[] buf, int offset, int buf_size, String src)
    {
        int i;
        for(i = 0; i < buf_size; i++) {
            if (i<src.length() && src.charAt(i)!=0)
                buf[i+offset] = (byte)src.charAt(i);
            else
                buf[i+offset] = ' ';
        }
    }

    static int ide_dvd_read_structure(IDEState s, int format, byte[] packet, byte[] buf) {
        switch (format) {
            case 0x0: /* Physical format information */
                {
                    int layer = packet[6] & 0xFF;
                    long total_sectors;

                    if (layer != 0)
                        return -ASC_INV_FIELD_IN_CMD_PACKET;

                    total_sectors = s.nb_sectors >> 2;
                    if (total_sectors == 0) {
                        return -ASC_MEDIUM_NOT_PRESENT;
                    }

                    buf[4] = 1;   /* DVD-ROM, part version 1 */
                    buf[5] = 0xf; /* 120mm disc, minimum rate unspecified */
                    buf[6] = 1;   /* one layer, read-only (per MMC-2 spec) */
                    buf[7] = 0;   /* default densities */

                    /* FIXME: 0x30000 per spec? */
                    be_writed(buf, 8, 0); /* start sector */
                    be_writed(buf, 12, total_sectors - 1); /* end sector */
                    be_writed(buf, 16, total_sectors - 1); /* l0 end sector */

                    /* Size of buffer, not including 2 byte size field */
                    be_writew(buf, 0, 2048 + 2);

                    /* 2k data + 4 byte header */
                    return (2048 + 4);
                }

            case 0x01: /* DVD copyright information */
                buf[4] = 0; /* no copyright data */
                buf[5] = 0; /* no region restrictions */

                /* Size of buffer, not including 2 byte size field */
                be_writew(buf, 0, 4 + 2);

                /* 4 byte header + 4 byte data */
                return (4 + 4);

            case 0x03: /* BCA information - invalid field for no BCA info */
                return -ASC_INV_FIELD_IN_CMD_PACKET;

            case 0x04: /* DVD disc manufacturing information */
                /* Size of buffer, not including 2 byte size field */
                be_writew(buf, 0, 2048 + 2);

                /* 2k data + 4 byte header */
                return (2048 + 4);

            case 0xff:
                /*
                 * This lists all the command capabilities above.  Add new ones
                 * in order and update the length and buffer return values.
                 */

                buf[4] = 0x00; /* Physical format */
                buf[5] = 0x40; /* Not writable, is readable */
                be_writew(buf, 6, 2048 + 4);

                buf[8] = 0x01; /* Copyright info */
                buf[9] = 0x40; /* Not writable, is readable */
                be_writew(buf, 10, 4 + 4);

                buf[12] = 0x03; /* BCA info */
                buf[13] = 0x40; /* Not writable, is readable */
                be_writew(buf, 14, 188 + 4);

                buf[16] = 0x04; /* Manufacturing info */
                buf[17] = 0x40; /* Not writable, is readable */
                be_writew(buf, 18, 2048 + 4);

                /* Size of buffer, not including 2 byte size field */
                be_writew(buf, 0, 16 + 2);

                /* data written + 4 byte header */
                return (16 + 4);

            default: /* TODO: formats beyond DVD-ROM requires */
                return -ASC_INV_FIELD_IN_CMD_PACKET;
        }
    }

    static private int event_status_media(IDEState s, byte[] buf) {
        int event_code, media_status;

        media_status = 0;
        if (s.tray_open) {
            media_status = Scsi.MS_TRAY_OPEN;
        } else if (Block.bdrv_is_inserted(s.bs)) {
            media_status = Scsi.MS_MEDIA_PRESENT;
        }

        /* Event notification descriptor */
        event_code = Scsi.MEC_NO_CHANGE;
        if (media_status != Scsi.MS_TRAY_OPEN) {
            if (s.events.new_media) {
                event_code = Scsi.MEC_NEW_MEDIA;
                s.events.new_media = false;
            } else if (s.events.eject_request) {
                event_code = Scsi.MEC_EJECT_REQUESTED;
                s.events.eject_request = false;
            }
        }

        buf[4] = (byte)event_code;
        buf[5] = (byte)media_status;

        /* These fields are reserved, just clear them. */
        buf[6] = 0;
        buf[7] = 0;

        return 8; /* We wrote to 4 extra bytes from the header */
    }

    // :TODO: this function was written really weirdly, why was packet assigned from buf then the two structures overlapped
    static private final AtapiCmdCallback cmd_get_event_status_notification = new AtapiCmdCallback() {
        public void call(IDEState s, byte[] buf) {
//            const uint8_t *packet = buf;
//
//            struct {
//                uint8_t opcode;
//                uint8_t polled;        /* lsb bit is polled; others are reserved */
//                uint8_t reserved2[2];
//                uint8_t class;
//                uint8_t reserved3[2];
//                uint16_t len;
//                uint8_t control;
//            } QEMU_PACKED *gesn_cdb;
//
//            struct {
//                uint16_t len;
//                uint8_t notification_class;
//                uint8_t supported_events;
//            } QEMU_PACKED *gesn_event_header;
            int max_len, used_len;

//            gesn_cdb = (void *)packet;
//            gesn_event_header = (void *)buf;
//
//            max_len = be16_to_cpu(gesn_cdb.len);
            max_len = be_readw(buf, 0);
            int polled = buf[1] & 0xFF;
            /* It is fine by the MMC spec to not support async mode operations */
            if ((/*gesn_cdb.*/polled & 0x01)==0) { /* asynchronous mode */
                /* Only polling is supported, asynchronous mode is not. */
                ide_atapi_cmd_error(s, Scsi.ILLEGAL_REQUEST, ASC_INV_FIELD_IN_CMD_PACKET);
                return;
            }

            /* polling mode operation */

            /*
             * These are the supported events.
             *
             * We currently only support requests of the 'media' type.
             * Notification class requests and supported event classes are bitmasks,
             * but they are build from the same values as the "notification class"
             * field.
             */
             //gesn_event_header.supported_events = 1 << Scsi.GESN_MEDIA;
            buf[3] = (byte) 1 << Scsi.GESN_MEDIA;

            /*
             * We use |= below to set the class field; other bits in this byte
             * are reserved now but this is useful to do if we have to use the
             * reserved fields later.
             */
            //gesn_event_header.notification_class = 0;
            buf[2] = 0;

            /*
             * Responses to requests are to be based on request priority.  The
             * notification_class_request_type enum above specifies the
             * priority: upper elements are higher prio than lower ones.
             */
//            if (gesn_cdb.class & (1 << GESN_MEDIA)) {
//                gesn_event_header.notification_class |= GESN_MEDIA;
//                used_len = event_status_media(s, buf);
//            } else {
//                gesn_event_header.notification_class = 0x80; /* No event available */
//                used_len = sizeof(*gesn_event_header);
//            }
//            gesn_event_header.len = cpu_to_be16(used_len - sizeof(*gesn_event_header));
            if ((buf[4] & (1 << Scsi.GESN_MEDIA))!=0) {
                buf[2] |= Scsi.GESN_MEDIA;
                used_len = event_status_media(s, buf);
            } else {
                buf[2] |= 0x80;
                used_len = 4;
            }
            ide_atapi_cmd_reply(s, used_len, max_len);
        }
    };

    static private final AtapiCmdCallback cmd_request_sense = new AtapiCmdCallback() {
        public void call(IDEState s, byte[] buf) {
            int max_len = buf[4] & 0xFF;

            java.util.Arrays.fill(buf, 0, 18, (byte)0);
            buf[0] = (byte)(0x70 | (1 << 7));
            buf[2] = (byte)s.sense_key;
            buf[7] = 10;
            buf[12] = (byte)s.asc;

            if (s.sense_key == Scsi.UNIT_ATTENTION) {
                s.sense_key = Scsi.NO_SENSE;
            }

            ide_atapi_cmd_reply(s, 18, max_len);
        }
    };

    static private final AtapiCmdCallback cmd_inquiry = new AtapiCmdCallback() {
        public void call(IDEState s, byte[] buf) {
            int max_len = buf[4] & 0xFF;

            buf[0] = 0x05; /* CD-ROM */
            buf[1] = (byte)0x80; /* removable */
            buf[2] = 0x00; /* ISO */
            buf[3] = 0x21; /* ATAPI-2 (XXX: put ATAPI-4 ?) */
            buf[4] = 31; /* additional length */
            buf[5] = 0; /* reserved */
            buf[6] = 0; /* reserved */
            buf[7] = 0; /* reserved */
            padstr8(buf, 8, 8, "QEMU");
            padstr8(buf, 16, 16, "QEMU DVD-ROM");
            padstr8(buf ,32, 4, s.version);
            ide_atapi_cmd_reply(s, 36, max_len);
        }
    };

    static private final AtapiCmdCallback cmd_get_configuration = new AtapiCmdCallback() {
        public void call(IDEState s, byte[] buf) {
            int len;
            int max_len;

            /* only feature 0 is supported */
            if (buf[2] != 0 || buf[3] != 0) {
                ide_atapi_cmd_error(s, Scsi.ILLEGAL_REQUEST, ASC_INV_FIELD_IN_CMD_PACKET);
                return;
            }

            /* XXX: could result in alignment problems in some architectures */
            max_len = be_readw(buf, 7);

            /*
             * XXX: avoid overflow for io_buffer if max_len is bigger than
             *      the size of that buffer (dimensioned to max number of
             *      sectors to transfer at once)
             *
             *      Only a problem if the feature/profiles grow.
             */
            if (max_len > 512) {
                /* XXX: assume 1 sector */
                max_len = 512;
            }
            java.util.Arrays.fill(buf, 0, max_len, (byte) 0);
            /*
             * the number of sectors from the media tells us which profile
             * to use as current.  0 means there is no media
             */
            if (media_is_dvd(s)) {
                be_writew(buf, 6, Scsi.MMC_PROFILE_DVD_ROM);
            } else if (media_is_cd(s)) {
                be_writew(buf, 6, Scsi.MMC_PROFILE_CD_ROM);
            }

            buf[10] = 0x02 | 0x01; /* persistent and current */
            len = 12; /* headers: 8 + 4 */
            IntRef index = new IntRef(0);
            len += ide_atapi_set_profile(buf, index, Scsi.MMC_PROFILE_DVD_ROM);
            len += ide_atapi_set_profile(buf, index, Scsi.MMC_PROFILE_CD_ROM);
            be_writed(buf, 0, len - 4); /* data length */

            ide_atapi_cmd_reply(s, len, max_len);
        }
    };

    static private final AtapiCmdCallback cmd_mode_sense = new AtapiCmdCallback() {
        public void call(IDEState s, byte[] buf) {
            int action, code;
            int max_len;

            max_len = be_readw(buf, 7);
            action = buf[2] >>> 6;
            code = buf[2] & 0x3f;

            switch(action) {
            case 0: /* current values */
                switch(code) {
                case Scsi.MODE_PAGE_R_W_ERROR: /* error recovery */
                    be_writew(buf, 0, 16 - 2);
                    buf[2] = 0x70;
                    buf[3] = 0;
                    buf[4] = 0;
                    buf[5] = 0;
                    buf[6] = 0;
                    buf[7] = 0;

                    buf[8] = Scsi.MODE_PAGE_R_W_ERROR;
                    buf[9] = 16 - 10;
                    buf[10] = 0x00;
                    buf[11] = 0x05;
                    buf[12] = 0x00;
                    buf[13] = 0x00;
                    buf[14] = 0x00;
                    buf[15] = 0x00;
                    ide_atapi_cmd_reply(s, 16, max_len);
                    break;
                case Scsi.MODE_PAGE_AUDIO_CTL:
                    be_writew(buf, 0, 24 - 2);
                    buf[2] = 0x70;
                    buf[3] = 0;
                    buf[4] = 0;
                    buf[5] = 0;
                    buf[6] = 0;
                    buf[7] = 0;

                    buf[8] = Scsi.MODE_PAGE_AUDIO_CTL;
                    buf[9] = 24 - 10;
                    /* Fill with CDROM audio volume */
                    buf[17] = 0;
                    buf[19] = 0;
                    buf[21] = 0;
                    buf[23] = 0;

                    ide_atapi_cmd_reply(s, 24, max_len);
                    break;
                case Scsi.MODE_PAGE_CAPABILITIES:
                    be_writew(buf, 0, 30 - 2);
                    buf[2] = 0x70;
                    buf[3] = 0;
                    buf[4] = 0;
                    buf[5] = 0;
                    buf[6] = 0;
                    buf[7] = 0;

                    buf[8] = Scsi.MODE_PAGE_CAPABILITIES;
                    buf[9] = 30 - 10;
                    buf[10] = 0x3b; /* read CDR/CDRW/DVDROM/DVDR/DVDRAM */
                    buf[11] = 0x00;

                    /* Claim PLAY_AUDIO capability (0x01) since some Linux
                       code checks for this to automount media. */
                    buf[12] = 0x71;
                    buf[13] = 3 << 5;
                    buf[14] = (1 << 0) | (1 << 3) | (1 << 5);
                    if (s.tray_locked) {
                        buf[14] |= 1 << 1;
                    }
                    buf[15] = 0x00; /* No volume & mute control, no changer */
                    be_writew(buf, 16, 704); /* 4x read speed */
                    buf[18] = 0; /* Two volume levels */
                    buf[19] = 2;
                    be_writew(buf, 20, 512); /* 512k buffer */
                    be_writew(buf, 22, 704); /* 4x read speed current */
                    buf[24] = 0;
                    buf[25] = 0;
                    buf[26] = 0;
                    buf[27] = 0;
                    buf[28] = 0;
                    buf[29] = 0;
                    ide_atapi_cmd_reply(s, 30, max_len);
                    break;
                default:
                    ide_atapi_cmd_error(s, Scsi.ILLEGAL_REQUEST, ASC_INV_FIELD_IN_CMD_PACKET);
                    return;
                }
                break;
            case 1: /* changeable values */
                ide_atapi_cmd_error(s, Scsi.ILLEGAL_REQUEST, ASC_INV_FIELD_IN_CMD_PACKET);
                return;
            case 2: /* default values */
                ide_atapi_cmd_error(s, Scsi.ILLEGAL_REQUEST, ASC_INV_FIELD_IN_CMD_PACKET);
                return;
            default:
            case 3: /* saved values */
                ide_atapi_cmd_error(s, Scsi.ILLEGAL_REQUEST, ASC_SAVING_PARAMETERS_NOT_SUPPORTED);
                break;
            }
        }
    };

    static private final AtapiCmdCallback cmd_test_unit_ready = new AtapiCmdCallback() {
        public void call(IDEState s, byte[] buf) {
            /* Not Ready Conditions are already handled in ide_atapi_cmd(), so if we
             * come here, we know that it's ready. */
            ide_atapi_cmd_ok(s);
        }
    };

    static private final AtapiCmdCallback cmd_prevent_allow_medium_removal = new AtapiCmdCallback() {
        public void call(IDEState s, byte[] buf) {
            s.tray_locked = (buf[4] & 1) !=0;
            Block.bdrv_lock_medium(s.bs, (buf[4] & 1)!=0);
            ide_atapi_cmd_ok(s);
        }
    };

    static private final AtapiCmdCallback cmd_read = new AtapiCmdCallback() {
        public void call(IDEState s, byte[] buf) {
            int nb_sectors;
            long lba;

            if ((buf[0] & 0xFF) == GPCMD_READ_10) {
                nb_sectors = be_readw(buf, 7);
            } else {
                nb_sectors = (int)be_readd(buf, 6);
            }

            lba = be_readd(buf, 2);
            if (nb_sectors == 0) {
                ide_atapi_cmd_ok(s);
                return;
            }

            ide_atapi_cmd_read(s, lba, nb_sectors, 2048);
        }
    };

    static private final AtapiCmdCallback cmd_read_cd = new AtapiCmdCallback() {
        public void call(IDEState s, byte[] buf) {
            int nb_sectors, transfer_request;
            long lba;

            nb_sectors = ((buf[6] & 0xFF) << 16) | ((buf[7] & 0xFF) << 8) | (buf[8] & 0xFF);
            lba = be_readd(buf, 2);

            if (nb_sectors == 0) {
                ide_atapi_cmd_ok(s);
                return;
            }

            transfer_request = buf[9] & 0xFF;
            switch(transfer_request & 0xf8) {
            case 0x00:
                /* nothing */
                ide_atapi_cmd_ok(s);
                break;
            case 0x10:
                /* normal read */
                ide_atapi_cmd_read(s, lba, nb_sectors, 2048);
                break;
            case 0xf8:
                /* read all data */
                ide_atapi_cmd_read(s, lba, nb_sectors, 2352);
                break;
            default:
                ide_atapi_cmd_error(s, Scsi.ILLEGAL_REQUEST,
                                    ASC_INV_FIELD_IN_CMD_PACKET);
                break;
            }
        }
    };

    static private final AtapiCmdCallback cmd_seek = new AtapiCmdCallback() {
        public void call(IDEState s, byte[] buf) {
            long lba;
            long total_sectors = s.nb_sectors >> 2;

            lba = be_readd(buf, 2);
            if (lba >= total_sectors) {
                ide_atapi_cmd_error(s, Scsi.ILLEGAL_REQUEST, ASC_LOGICAL_BLOCK_OOR);
                return;
            }

            ide_atapi_cmd_ok(s);
        }
    };

    static private final AtapiCmdCallback cmd_start_stop_unit = new AtapiCmdCallback() {
        public void call(IDEState s, byte[] buf) {
            int sense;
            boolean start = (buf[4] & 1) != 0;
            boolean loej = (buf[4] & 2) != 0;     /* load on start, eject on !start */

            if (loej) {
                if (!start && !s.tray_open && s.tray_locked) {
                    sense = Block.bdrv_is_inserted(s.bs) ? Scsi.NOT_READY : Scsi.ILLEGAL_REQUEST;
                    ide_atapi_cmd_error(s, sense, ASC_MEDIA_REMOVAL_PREVENTED);
                    return;
                }

                if (s.tray_open != !start) {
                    Block.bdrv_eject(s.bs, !start);
                    s.tray_open = !start;
                }
            }

            ide_atapi_cmd_ok(s);
        }
    };

    static private final AtapiCmdCallback cmd_mechanism_status = new AtapiCmdCallback() {
        public void call(IDEState s, byte[] buf) {
            int max_len = be_readw(buf, 8);

            be_writew(buf, 0, 0);
            /* no current LBA */
            buf[2] = 0;
            buf[3] = 0;
            buf[4] = 0;
            buf[5] = 1;
            be_writew(buf, 6, 0);
            ide_atapi_cmd_reply(s, 8, max_len);
        }
    };

    static private final AtapiCmdCallback cmd_read_toc_pma_atip = new AtapiCmdCallback() {
        public void call(IDEState s, byte[] buf) {
            int format, msf, start_track, len;
            int max_len;
            long total_sectors = s.nb_sectors >> 2;

            max_len = be_readw(buf, 7);
            format = (buf[9] & 0xFF) >> 6;
            msf = ((buf[1] & 0xFF) >> 1) & 1;
            start_track = buf[6] & 0xFF;

            switch(format) {
            case 0:
                len = Cdrom.cdrom_read_toc(total_sectors, buf, msf, start_track);
                if (len < 0) {
                    ide_atapi_cmd_error(s, Scsi.ILLEGAL_REQUEST, ASC_INV_FIELD_IN_CMD_PACKET);
                    return;
                }
                ide_atapi_cmd_reply(s, len, max_len);
                break;
            case 1:
                /* multi session : only a single session defined */
                java.util.Arrays.fill(buf, 0, 12, (byte)0);
                buf[1] = 0x0a;
                buf[2] = 0x01;
                buf[3] = 0x01;
                ide_atapi_cmd_reply(s, 12, max_len);
                break;
            case 2:
                len = Cdrom.cdrom_read_toc_raw(total_sectors, buf, msf, start_track);
                if (len < 0) {
                    ide_atapi_cmd_error(s, Scsi.ILLEGAL_REQUEST, ASC_INV_FIELD_IN_CMD_PACKET);
                    return;
                }
                ide_atapi_cmd_reply(s, len, max_len);
                break;
            default:
                ide_atapi_cmd_error(s, Scsi.ILLEGAL_REQUEST, ASC_INV_FIELD_IN_CMD_PACKET);
            }
        }
    };

    static private final AtapiCmdCallback cmd_read_cdvd_capacity = new AtapiCmdCallback() {
        public void call(IDEState s, byte[] buf) {
            long total_sectors = s.nb_sectors >> 2;

            /* NOTE: it is really the number of sectors minus 1 */
            be_writed(buf, 0, total_sectors - 1);
            be_writed(buf, 4, 2048);
            ide_atapi_cmd_reply(s, 8, 8);
        }
    };

    static private final AtapiCmdCallback cmd_read_disc_information = new AtapiCmdCallback() {
        public void call(IDEState s, byte[] buf) {
            int type = buf[1] & 7;
            int max_len = be_readw(buf, 7);

            /* Types 1/2 are only defined for Blu-Ray.  */
            if (type != 0) {
                ide_atapi_cmd_error(s, Scsi.ILLEGAL_REQUEST, ASC_INV_FIELD_IN_CMD_PACKET);
                return;
            }

            java.util.Arrays.fill(buf, 0, 34, (byte)0);
            buf[1] = 32;
            buf[2] = 0xe; /* last session complete, disc finalized */
            buf[3] = 1;   /* first track on disc */
            buf[4] = 1;   /* # of sessions */
            buf[5] = 1;   /* first track of last session */
            buf[6] = 1;   /* last track of last session */
            buf[7] = 0x20; /* unrestricted use */
            buf[8] = 0x00; /* CD-ROM or DVD-ROM */
            /* 9-10-11: most significant byte corresponding bytes 4-5-6 */
            /* 12-23: not meaningful for CD-ROM or DVD-ROM */
            /* 24-31: disc bar code */
            /* 32: disc application code */
            /* 33: number of OPC tables */

            ide_atapi_cmd_reply(s, 34, max_len);
        }
    };

    static private final AtapiCmdCallback cmd_read_dvd_structure = new AtapiCmdCallback() {
        public void call(IDEState s, byte[] buf) {
            int max_len;
            int media = buf[1] & 0xFF;
            int format = buf[7] & 0xFF;
            int ret;

            max_len = be_readw(buf, 8);

            if (format < 0xff) {
                if (media_is_cd(s)) {
                    ide_atapi_cmd_error(s, Scsi.ILLEGAL_REQUEST, ASC_INCOMPATIBLE_FORMAT);
                    return;
                } else if (!media_present(s)) {
                    ide_atapi_cmd_error(s, Scsi.ILLEGAL_REQUEST, ASC_INV_FIELD_IN_CMD_PACKET);
                    return;
                }
            }

            java.util.Arrays.fill(buf, 0, max_len > IDE_DMA_BUF_SECTORS * 512 + 4 ? IDE_DMA_BUF_SECTORS * 512 + 4 : max_len, (byte)0);

            if ((format>=0 && format<=0x7f) || format == 0xff) {
                    if (media == 0) {
                        ret = ide_dvd_read_structure(s, format, buf, buf);

                        if (ret < 0) {
                            ide_atapi_cmd_error(s, Scsi.ILLEGAL_REQUEST, -ret);
                        } else {
                            ide_atapi_cmd_reply(s, ret, max_len);
                        }
                    } else {
                        /* TODO: BD support, fall through for now */
                        ide_atapi_cmd_error(s, Scsi.ILLEGAL_REQUEST, ASC_INV_FIELD_IN_CMD_PACKET);
                    }
            } else {
                switch (format) {
                /* Generic disk structures */
                case 0x80: /* TODO: AACS volume identifier */
                case 0x81: /* TODO: AACS media serial number */
                case 0x82: /* TODO: AACS media identifier */
                case 0x83: /* TODO: AACS media key block */
                case 0x90: /* TODO: List of recognized format layers */
                case 0xc0: /* TODO: Write protection status */
                default:
                    ide_atapi_cmd_error(s, Scsi.ILLEGAL_REQUEST, ASC_INV_FIELD_IN_CMD_PACKET);
                    break;
                }
            }
        }
    };

    static private final AtapiCmdCallback cmd_set_speed = new AtapiCmdCallback() {
        public void call(IDEState s, byte[] buf) {
            ide_atapi_cmd_ok(s);
        }
    };

    /*
     * Only commands flagged as ALLOW_UA are allowed to run under a
     * unit attention condition. (See MMC-5, section 4.1.6.1)
     */
    static private final int ALLOW_UA = 0x01;

    /*
     * Commands flagged with CHECK_READY can only execute if a medium is present.
     * Otherwise they report the Not Ready Condition. (See MMC-5, section
     * 4.1.8)
     */
    static private final int CHECK_READY = 0x02;

    static private interface AtapiCmdCallback {
        public void call(IDEState s, byte[] buf);
    }
    static private final class AtapiCmd {
        public AtapiCmd(AtapiCmdCallback cb, int flags) {
            this.handler = cb;
            this.flags = flags;
        }
        AtapiCmdCallback handler;
        int flags;
    }

    static private final AtapiCmd[] atapi_cmd_table = new AtapiCmd[0x100];
    static {
        atapi_cmd_table[ 0x00 ] = new AtapiCmd( cmd_test_unit_ready,               CHECK_READY );
        atapi_cmd_table[ 0x03 ] = new AtapiCmd( cmd_request_sense,                 ALLOW_UA );
        atapi_cmd_table[ 0x12 ] = new AtapiCmd( cmd_inquiry,                       ALLOW_UA );
        atapi_cmd_table[ 0x1b ] = new AtapiCmd( cmd_start_stop_unit,               0 ); /* [1] */
        atapi_cmd_table[ 0x1e ] = new AtapiCmd( cmd_prevent_allow_medium_removal,  0 );
        atapi_cmd_table[ 0x25 ] = new AtapiCmd( cmd_read_cdvd_capacity,            CHECK_READY );
        atapi_cmd_table[ 0x28 ] = new AtapiCmd( cmd_read, /* (10) */               CHECK_READY );
        atapi_cmd_table[ 0x2b ] = new AtapiCmd( cmd_seek,                          CHECK_READY );
        atapi_cmd_table[ 0x43 ] = new AtapiCmd( cmd_read_toc_pma_atip,             CHECK_READY );
        atapi_cmd_table[ 0x46 ] = new AtapiCmd( cmd_get_configuration,             ALLOW_UA );
        atapi_cmd_table[ 0x4a ] = new AtapiCmd( cmd_get_event_status_notification, ALLOW_UA );
        atapi_cmd_table[ 0x51 ] = new AtapiCmd( cmd_read_disc_information,         CHECK_READY );
        atapi_cmd_table[ 0x5a ] = new AtapiCmd( cmd_mode_sense, /* (10) */         0 );
        atapi_cmd_table[ 0xa8 ] = new AtapiCmd( cmd_read, /* (12) */               CHECK_READY );
        atapi_cmd_table[ 0xad ] = new AtapiCmd( cmd_read_dvd_structure,            CHECK_READY );
        atapi_cmd_table[ 0xbb ] = new AtapiCmd( cmd_set_speed,                     0 );
        atapi_cmd_table[ 0xbd ] = new AtapiCmd( cmd_mechanism_status,              0 );
        atapi_cmd_table[ 0xbe ] = new AtapiCmd( cmd_read_cd,                       CHECK_READY );
        for (int i=0;i<atapi_cmd_table.length;i++) {
            if (atapi_cmd_table[i] == null)
                atapi_cmd_table[i] = new AtapiCmd(null, 0);
        }
        /* [1] handler detects and reports not ready condition itself */
    }

    static private boolean media_present(IDEState s) {
        return !s.tray_open && s.nb_sectors > 0;
    }
    
    /* XXX: DVDs that could fit on a CD will be reported as a CD */
    static private boolean media_is_dvd(IDEState s) {
        return (media_present(s) && s.nb_sectors > Scsi.CD_MAX_SECTORS);
    }
    
    static private boolean media_is_cd(IDEState s) {
        return (media_present(s) && s.nb_sectors <= Scsi.CD_MAX_SECTORS);
    }
    
    static void cd_data_to_raw(byte[] buf, long lba) {
        /* sync bytes */
        buf[0] = 0x00;
        java.util.Arrays.fill(buf, 1, 11, (byte) 0xFF);
        buf[11] = 0x00;
        int offset=12;
        /* MSF */
        Cdrom.lba_to_msf(buf, offset, lba);
        buf[offset+3] = 0x01; /* mode 1 data */
        offset += 4;
        /* data */
        offset += 2048;
        /* XXX: ECC not computed */
        java.util.Arrays.fill(buf, offset, offset + 288, (byte) 0);
    }

    static int cd_read_sector(IDEState s, long lba, byte[] buf, int sector_size) {
        int ret;
    
        switch(sector_size) {
        case 2048:
            Block.bdrv_acct_start(s.bs, s.acct, 4 * Block.BDRV_SECTOR_SIZE, Block.BDRV_ACCT_READ);
            ret = Block.bdrv_read(s.bs, lba << 2, buf, 0, 4);
            Block.bdrv_acct_done(s.bs, s.acct);
            break;
        case 2352:
            Block.bdrv_acct_start(s.bs, s.acct, 4 * Block.BDRV_SECTOR_SIZE, Block.BDRV_ACCT_READ);
            ret = Block.bdrv_read(s.bs, lba << 2, buf, 16, 4);
            Block.bdrv_acct_done(s.bs, s.acct);
            if (ret < 0)
                return ret;
            cd_data_to_raw(buf, lba);
            break;
        default:
            ret = -Error.EIO;
            break;
        }
        return ret;
    }
    
    static private void ide_atapi_cmd_ok(IDEState s) {
        s.error = 0;
        s.status = READY_STAT | SEEK_STAT;
        s.nsector = (s.nsector & ~7) | ATAPI_INT_REASON_IO | ATAPI_INT_REASON_CD;
        ide_set_irq(s.bus);
    }
    
    static private void ide_atapi_cmd_error(IDEState s, int sense_key, int asc) {
        if (DEBUG_IDE_ATAPI)
            System.out.println("atapi_cmd_error: sense=0x"+Integer.toHexString(sense_key)+" asc=0x"+Integer.toHexString(asc));
        s.error = sense_key << 4;
        s.status = READY_STAT | ERR_STAT;
        s.nsector = (s.nsector & ~7) | ATAPI_INT_REASON_IO | ATAPI_INT_REASON_CD;
        s.sense_key = sense_key;
        s.asc = asc;
        ide_set_irq(s.bus);
    }
    
    static private void ide_atapi_io_error(IDEState s, int ret) {
        /* XXX: handle more errors */
        if (ret == -Error.ENOMEDIUM) {
            ide_atapi_cmd_error(s, Scsi.NOT_READY, ASC_MEDIUM_NOT_PRESENT);
        } else {
            ide_atapi_cmd_error(s, Scsi.ILLEGAL_REQUEST, ASC_LOGICAL_BLOCK_OOR);
        }
    }

/* The whole ATAPI transfer logic is handled in this function */
    static public Internal.EndTransferFunc ide_atapi_cmd_reply_end = new Internal.EndTransferFunc() {
        public void call(Internal.IDEState s) {
        int byte_count_limit, size, ret;
        if (DEBUG_IDE_ATAPI)
            System.out.println("reply: tx_size="+s.packet_transfer_size+" elem_tx_size="+s.elementary_transfer_size+" index="+s.io_buffer_index);
        if (s.packet_transfer_size <= 0) {
            /* end of transfer */
            IDE.ide_transfer_stop(s);
            s.status = READY_STAT | SEEK_STAT;
            s.nsector = (s.nsector & ~7) | ATAPI_INT_REASON_IO | ATAPI_INT_REASON_CD;
            ide_set_irq(s.bus);
            if (DEBUG_IDE_ATAPI)
                System.out.println("status=0x"+Integer.toHexString(s.status));
        } else {
            /* see if a new sector must be read */
            if (s.lba != -1 && s.io_buffer_index >= s.cd_sector_size) {
                ret = cd_read_sector(s, s.lba, s.io_buffer, s.cd_sector_size);
                if (ret < 0) {
                    IDE.ide_transfer_stop(s);
                    ide_atapi_io_error(s, ret);
                    return;
                }
                s.lba++;
                s.io_buffer_index = 0;
            }
            if (s.elementary_transfer_size > 0) {
                /* there are some data left to transmit in this elementary
                   transfer */
                size = s.cd_sector_size - s.io_buffer_index;
                if (size > s.elementary_transfer_size)
                    size = s.elementary_transfer_size;
                s.packet_transfer_size -= size;
                s.elementary_transfer_size -= size;
                s.io_buffer_index += size;
                IDE.ide_transfer_start(s, s.io_buffer, s.io_buffer_index - size,
                        size, ide_atapi_cmd_reply_end);
            } else {
                /* a new transfer is needed */
                s.nsector = (s.nsector & ~7) | ATAPI_INT_REASON_IO;
                byte_count_limit = s.lcyl | (s.hcyl << 8);
                if (DEBUG_IDE_ATAPI)
                    System.out.println("byte_count_limit="+byte_count_limit);
                if (byte_count_limit == 0xffff)
                    byte_count_limit--;
                size = s.packet_transfer_size;
                if (size > byte_count_limit) {
                    /* byte count limit must be even if this case */
                    if ((byte_count_limit & 1)!=0)
                        byte_count_limit--;
                    size = byte_count_limit;
                }
                s.lcyl = size;
                s.hcyl = size >>> 8;
                s.elementary_transfer_size = size;
                /* we cannot transmit more than one sector at a time */
                if (s.lba != -1) {
                    if (size > (s.cd_sector_size - s.io_buffer_index))
                        size = (s.cd_sector_size - s.io_buffer_index);
                }
                s.packet_transfer_size -= size;
                s.elementary_transfer_size -= size;
                s.io_buffer_index += size;
                IDE.ide_transfer_start(s, s.io_buffer, s.io_buffer_index - size, size, ide_atapi_cmd_reply_end);
                ide_set_irq(s.bus);
                if (DEBUG_IDE_ATAPI)
                    System.out.println("status=0x"+Integer.toHexString(s.status));
            }
        }
    }};

    /* send a reply of 'size' bytes in s.io_buffer to an ATAPI command */
    static private void ide_atapi_cmd_reply(IDEState s, int size, int max_size)
    {
        if (size > max_size)
            size = max_size;
        s.lba = -1; /* no sector read */
        s.packet_transfer_size = size;
        s.io_buffer_size = size;    /* dma: send the reply data as one chunk */
        s.elementary_transfer_size = 0;
        s.io_buffer_index = 0;
    
        if (s.atapi_dma!=0) {
            Block.bdrv_acct_start(s.bs, s.acct, size, Block.BDRV_ACCT_READ);
            s.status = READY_STAT | SEEK_STAT | DRQ_STAT;
            s.bus.dma.ops.start_dma.call(s.bus.dma, s, ide_atapi_cmd_read_dma_cb);
        } else {
            s.status = READY_STAT | SEEK_STAT;
            ide_atapi_cmd_reply_end.call(s);
        }
    }
    
    /* start a CD-CDROM read command */
    static private void ide_atapi_cmd_read_pio(IDEState s, long lba, int nb_sectors, int sector_size)
    {
        s.lba = lba;
        s.packet_transfer_size = nb_sectors * sector_size;
        s.elementary_transfer_size = 0;
        s.io_buffer_index = sector_size;
        s.cd_sector_size = sector_size;
    
        s.status = READY_STAT | SEEK_STAT;
        ide_atapi_cmd_reply_end.call(s);
    }

    
    static public Internal.EndTransferFunc ide_atapi_cmd = new Internal.EndTransferFunc() {
        public void call(Internal.IDEState s) {
//        #ifdef DEBUG_IDE_ATAPI
//            {
//                int i;
//                printf("ATAPI limit=0x%x packet:", s.lcyl | (s.hcyl << 8));
//                for(i = 0; i < ATAPI_PACKET_SIZE; i++) {
//                    printf(" %02x", buf[i]);
//                }
//                printf("\n");
//            }
//        #endif
            /*
             * If there's a UNIT_ATTENTION condition pending, only command flagged with
             * ALLOW_UA are allowed to complete. with other commands getting a CHECK
             * condition response unless a higher priority status, defined by the drive
             * here, is pending.
             */
            if (s.sense_key == Scsi.UNIT_ATTENTION && (atapi_cmd_table[s.io_buffer[0] & 0xFF].flags & ALLOW_UA)==0) {
                ide_atapi_cmd_check_status(s);
                return;
            }
            /*
             * When a CD gets changed, we have to report an ejected state and
             * then a loaded state to guests so that they detect tray
             * open/close and media change events.  Guests that do not use
             * GET_EVENT_STATUS_NOTIFICATION to detect such tray open/close
             * states rely on this behavior.
             */
            if (!s.tray_open && Block.bdrv_is_inserted(s.bs) && s.cdrom_changed) {
                ide_atapi_cmd_error(s, Scsi.NOT_READY, ASC_MEDIUM_NOT_PRESENT);

                s.cdrom_changed = false;
                s.sense_key = Scsi.UNIT_ATTENTION;
                s.asc = ASC_MEDIUM_MAY_HAVE_CHANGED;
                return;
            }

            /* Report a Not Ready condition if appropriate for the command */
            if ((atapi_cmd_table[s.io_buffer[0] & 0xFF].flags & CHECK_READY)!=0 &&
                (!media_present(s) || !Block.bdrv_is_inserted(s.bs)))
            {
                ide_atapi_cmd_error(s, Scsi.NOT_READY, ASC_MEDIUM_NOT_PRESENT);
                return;
            }

            /* Execute the command */
            if (atapi_cmd_table[s.io_buffer[0] & 0xFF].handler!=null) {
                atapi_cmd_table[s.io_buffer[0] & 0xFF].handler.call(s, s.io_buffer);
                return;
            }

            ide_atapi_cmd_error(s, Scsi.ILLEGAL_REQUEST, ASC_ILLEGAL_OPCODE);
        }
    };

    static private void ide_atapi_cmd_check_status(IDEState s) {
        if (DEBUG_IDE_ATAPI)
            System.out.println("atapi_cmd_check_status\n");
        s.error = MC_ERR | (Scsi.UNIT_ATTENTION << 4);
        s.status = ERR_STAT;
        s.nsector = 0;
        ide_set_irq(s.bus);
    }
    /* ATAPI DMA support */

    /* XXX: handle read errors */
    static private final Block.BlockDriverCompletionFunc ide_atapi_cmd_read_dma_cb = new Block.BlockDriverCompletionFunc() {
        public void call(Object opaque, int ret) {
            IDEState s = (IDEState)opaque;
            int data_offset, n;

            while (true) { // eot label
                if (ret < 0) {
                    ide_atapi_io_error(s, ret);
                    break;
                }

                if (s.io_buffer_size > 0) {
                    /*
                     * For a cdrom read sector command (s.lba != -1),
                     * adjust the lba for the next s.io_buffer_size chunk
                     * and dma the current chunk.
                     * For a command != read (s.lba == -1), just transfer
                     * the reply data.
                     */
                    if (s.lba != -1) {
                        if (s.cd_sector_size == 2352) {
                            n = 1;
                            cd_data_to_raw(s.io_buffer, s.lba);
                        } else {
                            n = s.io_buffer_size >> 11;
                        }
                        s.lba += n;
                    }
                    s.packet_transfer_size -= s.io_buffer_size;
                    if (s.bus.dma.ops.rw_buf.call(s.bus.dma, 1) == 0)
                        break;
                }

                if (s.packet_transfer_size <= 0) {
                    s.status = READY_STAT | SEEK_STAT;
                    s.nsector = (s.nsector & ~7) | ATAPI_INT_REASON_IO | ATAPI_INT_REASON_CD;
                    ide_set_irq(s.bus);
                    break;
                }

                s.io_buffer_index = 0;
                if (s.cd_sector_size == 2352) {
                    n = 1;
                    s.io_buffer_size = s.cd_sector_size;
                    data_offset = 16;
                } else {
                    n = s.packet_transfer_size >> 11;
                    if (n > (IDE_DMA_BUF_SECTORS / 4))
                        n = (IDE_DMA_BUF_SECTORS / 4);
                    s.io_buffer_size = n * 2048;
                    data_offset = 0;
                }
                if (DEBUG_AIO)
                    System.out.println("aio_read_cd: lba="+s.lba+" n="+n);

                s.bus.dma.iov.iov_base = s.io_buffer;
                s.bus.dma.iov.iov_base_offset = data_offset;
                s.bus.dma.iov.iov_len = n * 4 * 512;
                QemuCommon.qemu_iovec_init_external(s.bus.dma.qiov, s.bus.dma.iov, 1);

                s.bus.dma.aiocb = Block.bdrv_aio_readv(s.bs, (long)s.lba << 2, s.bus.dma.qiov, n * 4, ide_atapi_cmd_read_dma_cb, s);
                return;

            }
            // eot:
            Block.bdrv_acct_done(s.bs, s.acct);
            s.bus.dma.ops.add_status.call(s.bus.dma, BM_STATUS_INT);
            IDE.ide_set_inactive(s);
        }
    };

    /* start a CD-CDROM read command with DMA */
    /* XXX: test if DMA is available */
    private static void ide_atapi_cmd_read_dma(IDEState s, long lba, int nb_sectors, int sector_size) {
        s.lba = lba;
        s.packet_transfer_size = nb_sectors * sector_size;
        s.io_buffer_index = 0;
        s.io_buffer_size = 0;
        s.cd_sector_size = sector_size;
    
        Block.bdrv_acct_start(s.bs, s.acct, s.packet_transfer_size, Block.BDRV_ACCT_READ);
    
        /* XXX: check if BUSY_STAT should be set */
        s.status = READY_STAT | SEEK_STAT | DRQ_STAT | BUSY_STAT;
        s.bus.dma.ops.start_dma.call(s.bus.dma, s, ide_atapi_cmd_read_dma_cb);
    }

    private static void ide_atapi_cmd_read(IDEState s, long lba, int nb_sectors, int sector_size) {
        if (DEBUG_IDE_ATAPI)
            System.out.println("read "+(s.atapi_dma!=0 ? "dma" : "pio")+": LBA="+lba+" nb_sectors="+nb_sectors);
        if (s.atapi_dma!=0) {
            ide_atapi_cmd_read_dma(s, lba, nb_sectors, sector_size);
        } else {
            ide_atapi_cmd_read_pio(s, lba, nb_sectors, sector_size);
        }
    }

    static private int ide_atapi_set_profile(byte[] buf, IntRef index, int  profile)
    {
        int offset = 12; /* start of profiles */

        offset += (index.value * 4); /* start of indexed profile */
        be_writew(buf, offset, profile);
        buf[2+offset] = (byte)(((buf[offset] == buf[6]) && (buf[1+offset] == buf[7]))?-1:0);

        /* each profile adds 4 bytes to the response */
        index.value++;
        buf[11] += 4; /* Additional Length */

        return 4;
    }
}
