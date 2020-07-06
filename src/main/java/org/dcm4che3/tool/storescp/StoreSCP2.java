///* ***** BEGIN LICENSE BLOCK *****
// * Version: MPL 1.1/GPL 2.0/LGPL 2.1
// *
// * The contents of this file are subject to the Mozilla Public License Version
// * 1.1 (the "License"); you may not use this file except in compliance with
// * the License. You may obtain a copy of the License at
// * http://www.mozilla.org/MPL/
// *
// * Software distributed under the License is distributed on an "AS IS" basis,
// * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
// * for the specific language governing rights and limitations under the
// * License.
// *
// * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
// * Java(TM), hosted at https://github.com/dcm4che.
// *
// * The Initial Developer of the Original Code is
// * Agfa Healthcare.
// * Portions created by the Initial Developer are Copyright (C) 2011
// * the Initial Developer. All Rights Reserved.
// *
// * Contributor(s):
// * See @authors listed below
// *
// * Alternatively, the contents of this file may be used under the terms of
// * either the GNU General Public License Version 2 or later (the "GPL"), or
// * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
// * in which case the provisions of the GPL or the LGPL are applicable instead
// * of those above. If you wish to allow use of your version of this file only
// * under the terms of either the GPL or the LGPL, and not to allow others to
// * use your version of this file under the terms of the MPL, indicate your
// * decision by deleting the provisions above and replace them with the notice
// * and other provisions required by the GPL or the LGPL. If you do not delete
// * the provisions above, a recipient may use your version of this file under
// * the terms of any one of the MPL, the GPL or the LGPL.
// *
// * ***** END LICENSE BLOCK ***** */
//
//package org.dcm4che3.tool.storescp;
//
//import java.io.File;
//import java.io.IOException;
//import java.net.URL;
//import java.util.List;
//import java.util.Objects;
//import java.util.Properties;
//import java.util.ResourceBundle;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import org.apache.commons.cli.CommandLine;
//import org.apache.commons.cli.Option;
//import org.apache.commons.cli.Options;
//import org.apache.commons.cli.ParseException;
//import org.dcm4che3.data.Attributes;
//import org.dcm4che3.data.Tag;
//import org.dcm4che3.data.UID;
//import org.dcm4che3.data.VR;
//import org.dcm4che3.io.DicomInputStream;
//import org.dcm4che3.io.DicomInputStream.IncludeBulkData;
//import org.dcm4che3.io.DicomOutputStream;
//import org.dcm4che3.net.ApplicationEntity;
//import org.dcm4che3.net.Association;
//import org.dcm4che3.net.Connection;
//import org.dcm4che3.net.Device;
//import org.dcm4che3.net.PDVInputStream;
//import org.dcm4che3.net.Status;
//import org.dcm4che3.net.TransferCapability;
//import org.dcm4che3.net.pdu.PresentationContext;
//import org.dcm4che3.net.service.BasicCEchoSCP;
//import org.dcm4che3.net.service.BasicCStoreSCP;
//import org.dcm4che3.net.service.DicomServiceException;
//import org.dcm4che3.net.service.DicomServiceRegistry;
//import org.dcm4che3.tool.common.CLIUtils;
//import org.dcm4che3.tool.dcm2dcm.Dcm2Dcm;
//import org.dcm4che3.util.AttributesFormat;
//import org.dcm4che3.util.SafeClose;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.weasis.core.api.util.FileUtil;
//import org.weasis.core.api.util.StringUtil;
//import org.weasis.dicom.param.DicomNode;
//
///**
// * @author Gunter Zeilinger <gunterze@gmail.com>
// *
// */
//public class StoreSCP {
//
//    private static ResourceBundle rb =
//            ResourceBundle.getBundle("org.dcm4che3.tool.storescp.messages");
//
//    private static final Logger LOGGER = LoggerFactory.getLogger(StoreSCP.class);
//
//    private static final String TMP_DIR = "tmp";
//
//    private final Device device = new Device("storescp");
//    private final ApplicationEntity ae = new ApplicationEntity("*");
//    private final Connection conn = new Connection();
//    private File storageDir;
//    private final List<DicomNode> authorizedCallingNodes;
//    private volatile AttributesFormat filePathFormat;
//    private volatile Pattern regex;
//    private volatile int status = Status.Success;
//    private int responseDelay;
//    private final BasicCStoreSCP cstoreSCP = new BasicCStoreSCP("*") {
//
//        @Override
//        protected void store(Association as, PresentationContext pc, Attributes rq, PDVInputStream data, Attributes rsp)
//            throws IOException {
//            if (authorizedCallingNodes != null && !authorizedCallingNodes.isEmpty()) {
//                DicomNode sourceNode = DicomNode.buildRemoteDicomNode(as);
//                boolean valid = authorizedCallingNodes.stream().anyMatch(n -> n.getAet().equals(sourceNode.getAet())
//                    && (!n.isValidateHostname() || n.equalsHostname(sourceNode.getHostname())));
//                if (!valid) {
//                    rsp.setInt(Tag.Status, VR.US, Status.NotAuthorized);
//                    LOGGER.error("Refused: not authorized (124H). Source node: {}. SopUID: {}", sourceNode,
//                        rq.getString(Tag.AffectedSOPInstanceUID));
//                    return;
//                }
//            }
//
//            rsp.setInt(Tag.Status, VR.US, status);
//
//            String cuid = rq.getString(Tag.AffectedSOPClassUID);
//            String iuid = rq.getString(Tag.AffectedSOPInstanceUID);
//            String tsuid = pc.getTransferSyntax();
//            File file = new File(storageDir, TMP_DIR + File.separator + iuid);
//            try {
//                Attributes fmi = as.createFileMetaInformation(iuid, cuid, tsuid);
//                storeTo(as, fmi, data, file);
//                String filename;
//                if (filePathFormat == null) {
//                    filename = iuid;
//                } else {
//                    Attributes a = fmi;
//                    Matcher regexMatcher = regex.matcher(filePathFormat.toString());
//                    while (regexMatcher.find()) {
//                        if (!regexMatcher.group(1).startsWith("0002")) {
//                            a = parse(file);
//                            a.addAll(fmi);
//                            break;
//                        }
//                    }
//                    filename = filePathFormat.format(a);
//                }
//                renameTo(as, file, new File(storageDir, filename));
//            } catch (Exception e) {
//                deleteFile(as, file);
//                throw new DicomServiceException(Status.ProcessingFailure, e);
//            }
//        }
//
//    };
//
//    /**
//     * @param storageDir
//     *            the base path of storage folder
//     * @throws IOException
//     */
//    public StoreSCP(File storageDir) throws IOException {
//        this(storageDir, null);
//    }
//
//    /**
//     * @param storageDir
//     *            the base path of storage folder
//     * @param authorizedCallingNodes
//     *            the list of authorized nodes to call store files (authorizedCallingNodes allow to check hostname
//     *            unlike acceptedCallingAETitles)
//     * @throws IOException
//     */
//    public StoreSCP(File storageDir, List<DicomNode> authorizedCallingNodes) throws IOException {
//        this.storageDir = Objects.requireNonNull(storageDir);
//        device.setDimseRQHandler(createServiceRegistry());
//        device.addConnection(conn);
//        device.addApplicationEntity(ae);
//        ae.setAssociationAcceptor(true);
//        ae.addConnection(conn);
//        this.authorizedCallingNodes = authorizedCallingNodes;
//    }
//
//    private void storeTo(Association as, Attributes fmi, PDVInputStream data, File file) throws IOException {
//        LOGGER.debug("{}: M-WRITE {}", as, file);
//        file.getParentFile().mkdirs();
//        Dcm2Dcm dcm2Dcm = new Dcm2Dcm();
//        dcm2Dcm.setTransferSyntax(UID.JPEGLossless);
//        dcm2Dcm.transcodeWithTranscoder(data, file);
////        DicomOutputStream out = new DicomOutputStream(file);
////        try {
////            out.writeFileMetaInformation(fmi);
////            data.copyTo(out);
////
////        } finally {
////            SafeClose.close(out);
////        }
//    }
//
//    private static void renameTo(Association as, File from, File dest) throws IOException {
//        LOGGER.info("{}: M-RENAME {} to {}", as, from, dest);
//        FileUtil.prepareToWriteFile(dest);
//        if (!from.renameTo(dest))
//            throw new IOException("Failed to rename " + from + " to " + dest);
//    }
//
//    private static Attributes parse(File file) throws IOException {
//        DicomInputStream in = new DicomInputStream(file);
//        try {
//            in.setIncludeBulkData(IncludeBulkData.NO);
//            return in.readDataset(-1, Tag.PixelData);
//        } finally {
//            SafeClose.close(in);
//        }
//    }
//
//    private static void deleteFile(Association as, File file) {
//        if (file.delete())
//            LOGGER.info("{}: M-DELETE {}", as, file);
//        else
//            LOGGER.warn("{}: M-DELETE {} failed!", as, file);
//    }
//
//    private DicomServiceRegistry createServiceRegistry() {
//        DicomServiceRegistry serviceRegistry = new DicomServiceRegistry();
//        serviceRegistry.addDicomService(new BasicCEchoSCP());
//        serviceRegistry.addDicomService(cstoreSCP);
//        return serviceRegistry;
//    }
//
//    public void setStorageFilePathFormat(String pattern) {
//        if (StringUtil.hasText(pattern)) {
//            this.filePathFormat = new AttributesFormat(pattern);
//            this.regex = Pattern.compile("\\{(.*?)\\}");
//        } else {
//            this.filePathFormat = null;
//            this.regex = null;
//        }
//    }
//
//    public void setStatus(int status) {
//        this.status = status;
//    }
//
//    public void loadDefaultTransferCapability(URL transferCapabilityFile) {
//        Properties p = new Properties();
//
//        try {
//            if (transferCapabilityFile != null) {
//                p.load(transferCapabilityFile.openStream());
//            } else {
//                p.load(this.getClass().getResourceAsStream("messages.properties"));
//            }
//        } catch (IOException e) {
//            LOGGER.error("Cannot read sop-classes", e);
//        }
//
//        for (String cuid : p.stringPropertyNames()) {
//            String ts = p.getProperty(cuid);
//            TransferCapability tc =
//                new TransferCapability(null, CLIUtils.toUID(cuid), TransferCapability.Role.SCP, CLIUtils.toUIDs(ts));
//            ae.addTransferCapability(tc);
//        }
//    }
//
//    public ApplicationEntity getApplicationEntity() {
//        return ae;
//    }
//
//    public Connection getConnection() {
//        return conn;
//    }
//
//    public Device getDevice() {
//        return device;
//    }
//
//    public void setResponseDelay(int responseDelay) {
//        this.responseDelay = responseDelay;
//    }
//
//    private static CommandLine parseComandLine(String[] args)
//            throws ParseException {
//        Options opts = new Options();
//        CLIUtils.addBindServerOption(opts);
//        CLIUtils.addAEOptions(opts);
//        CLIUtils.addCommonOptions(opts);
//        addStatusOption(opts);
//        addResponseDelayOption(opts);
//        addStorageDirectoryOptions(opts);
//        addTransferCapabilityOptions(opts);
//        return CLIUtils.parseComandLine(args, opts, rb, StoreSCP.class);
//    }
//
//    @SuppressWarnings("static-access")
//    private static void addStatusOption(Options opts) {
//        opts.addOption(Option.builder()
//                .hasArg()
//                .argName("code")
//                .desc(rb.getString("status"))
//                .longOpt("status")
//                .build());
//    }
//
//    private static void addResponseDelayOption(Options opts) {
//        opts.addOption(Option.builder()
//                .hasArg()
//                .argName("ms")
//                .desc(rb.getString("response-delay"))
//                .longOpt("response-delay")
//                .build());
//    }
//
//    @SuppressWarnings("static-access")
//    private static void addStorageDirectoryOptions(Options opts) {
//        opts.addOption(null, "ignore", false,
//                rb.getString("ignore"));
//        opts.addOption(Option.builder()
//                .hasArg()
//                .argName("path")
//                .desc(rb.getString("directory"))
//                .longOpt("directory")
//                .build());
//        opts.addOption(Option.builder()
//                .hasArg()
//                .argName("pattern")
//                .desc(rb.getString("filepath"))
//                .longOpt("filepath")
//                .build());
//    }
//
//    @SuppressWarnings("static-access")
//    private static void addTransferCapabilityOptions(Options opts) {
//        opts.addOption(null, "accept-unknown", false,
//                rb.getString("accept-unknown"));
//        opts.addOption(Option.builder()
//                .hasArg()
//                .argName("file|url")
//                .desc(rb.getString("sop-classes"))
//                .longOpt("sop-classes")
//                .build());
//    }
//
//    public static void main(String[] args) {
//        try {
//            CommandLine cl = parseComandLine(args);
//            StoreSCP main = new StoreSCP(new File("D://MR"));
//            CLIUtils.configureBindServer(main.conn, main.ae, cl);
//            CLIUtils.configure(main.conn, cl);
//            main.setStatus(CLIUtils.getIntOption(cl, "status", 0));
//            main.setResponseDelay(CLIUtils.getIntOption(cl, "response-delay", 0));
//            configureTransferCapability(main.ae, cl);
//            configureStorageDirectory(main, cl);
//            ExecutorService executorService = Executors.newCachedThreadPool();
//            ScheduledExecutorService scheduledExecutorService =
//                    Executors.newSingleThreadScheduledExecutor();
//            main.device.setScheduledExecutor(scheduledExecutorService);
//            main.device.setExecutor(executorService);
//            main.device.bindConnections();
//        } catch (ParseException e) {
//            System.err.println("storescp: " + e.getMessage());
//            System.err.println(rb.getString("try"));
//            System.exit(2);
//        } catch (Exception e) {
//            System.err.println("storescp: " + e.getMessage());
//            e.printStackTrace();
//            System.exit(2);
//        }
//    }
//
//    private static void configureStorageDirectory(StoreSCP main, CommandLine cl) {
//        if (!cl.hasOption("ignore")) {
//            main.setStorageDirectory(
//                    new File(cl.getOptionValue("directory", ".")));
//            if (cl.hasOption("filepath"))
//                main.setStorageFilePathFormat(cl.getOptionValue("filepath"));
//        }
//    }
//
//    private static void configureTransferCapability(ApplicationEntity ae,
//                                                    CommandLine cl) throws IOException {
//        if (cl.hasOption("accept-unknown")) {
//            ae.addTransferCapability(
//                    new TransferCapability(null,
//                            "*",
//                            TransferCapability.Role.SCP,
//                            "*"));
//        } else {
//            Properties p = CLIUtils.loadProperties(
//                    cl.getOptionValue("sop-classes",
//                            "resource:messages.properties"),
//                    null);
//            for (String cuid : p.stringPropertyNames()) {
//                String ts = p.getProperty(cuid);
//                TransferCapability tc = new TransferCapability(null,
//                        CLIUtils.toUID(cuid),
//                        TransferCapability.Role.SCP,
//                        CLIUtils.toUIDs(ts));
//                ae.addTransferCapability(tc);
//            }
//        }
//    }
//
//    public void setStorageDirectory(File storageDir) {
//        if (storageDir != null)
//            storageDir.mkdirs();
//        this.storageDir = storageDir;
//    }
//
//}
