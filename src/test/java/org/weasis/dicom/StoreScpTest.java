package org.weasis.dicom;

import org.dcm4che3.tool.storescp.StoreSCP;

import java.io.IOException;

public class StoreScpTest {

    public static void main(String[] args) throws IOException {
        StoreSCP.main(new String[]{"--accept-unknown", "-b", "STORESCP:11112", "--directory=D://MR", "--filepath={00080020}/{00100020}/{0020000D}/{0020000E}/{00080018}.dcm"});
    }
}
