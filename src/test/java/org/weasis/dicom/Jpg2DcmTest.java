package org.weasis.dicom;


import org.dcm4che3.tool.jpg2dcm.Jpg2Dcm;
import org.junit.Test;

public class Jpg2DcmTest {
    @Test
    public void testProcess() {
        Jpg2Dcm.main(new String[]{"C:\\Users\\ZKYY\\Pictures\\软件图标\\353.jpg", "D:/test.dcm"});
    }
}
