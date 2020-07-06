package org.weasis.dicom;

import org.dcm4che3.data.UID;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.tool.dcm2dcm.Dcm2Dcm;
import org.dcm4che3.util.SafeClose;
import org.junit.Test;
import org.weasis.core.api.util.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Dcm2DcmTest {

    @Test
    public void testProcess() throws IOException {
//        BasicConfigurator.configure();
//
//        DicomNode calling = new DicomNode("WEASIS-SCU");
//        DicomNode called = new DicomNode("ORTHANC", "127.0.0.1", 4242);
//        DicomState state = Echo.process(null, calling, called);
//        // Should never happen
//        Assert.assertNotNull(state);
//
//        System.out.println("DICOM Status:" + state.getStatus());
//        System.out.println(state.getMessage());
//        // see org.dcm4che3.net.Status
//        // See server log at http://dicomserver.co.uk/logs/
//        Assert.assertThat(state.getMessage(), state.getStatus(), IsEqual.equalTo(Status.Success));
//
//        Dcm2Dcm.main(new String[]{"--jpll", "D:\\下载\\20200701_05940002_MR168077_1556382_137", "D:/MR"});
        String dest = "D:\\MR\\1.2.840.113619.2.312.2807.4257742.13590.1593518264.971.dcm";
        final File finalDest = new File(dest);
        InputStream src = new FileInputStream("D:\\下载\\20200701_05940002_MR168077_1556382_137\\1.2.840.113619.2.312.2807.4257742.13590.1593518264.971.dcm");
        Dcm2Dcm dcm2Dcm = new Dcm2Dcm();
        dcm2Dcm.setTransferSyntax(UID.JPEGLossless);
        dcm2Dcm.transcodeWithTranscoder(new DicomInputStream(src), finalDest);


//        dcm2Dcm.transcodeWithTranscoder(, finalDest);
    }
}
