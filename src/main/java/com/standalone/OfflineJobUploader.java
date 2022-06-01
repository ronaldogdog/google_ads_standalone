package com.standalone;

import com.google.ads.googleads.lib.GoogleAdsClient;
import com.google.ads.googleads.v10.common.CustomerMatchUserListMetadata;
import com.google.ads.googleads.v10.common.UserData;
import com.google.ads.googleads.v10.common.UserIdentifier;
import com.google.ads.googleads.v10.enums.OfflineUserDataJobTypeEnum;
import com.google.ads.googleads.v10.errors.GoogleAdsFailure;
import com.google.ads.googleads.v10.resources.OfflineUserDataJob;
import com.google.ads.googleads.v10.services.*;
import com.google.ads.googleads.v10.utils.ErrorUtils;
import com.google.ads.googleads.v10.utils.ResourceNames;
import org.apache.log4j.Logger;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class OfflineJobUploader {
    private static final Logger logger = Logger.getLogger(OfflineJobUploader.class);
    private static final String GOOGLEADS_CUSTOMER_ID = "googleads.customerId";
    private static final String GOOGLEADS_AUDIENCE_ID = "googleads.audienceId";

    private static final MessageDigest digest = getSHA256MessageDigest();

    private static String toSHA256String(String str) throws UnsupportedEncodingException {
        String normalized = str.trim().toLowerCase();
        byte[] hash = digest.digest(normalized.getBytes("UTF-8"));
        StringBuilder result = new StringBuilder();
        for (byte b : hash) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private static MessageDigest getSHA256MessageDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Missing SHA-256 algorithm implementation.", e);
        }
    }

    private List<OfflineUserDataJobOperation> prepareList() throws UnsupportedEncodingException {
        logger.info("Preparing dummy email list");
        List<OfflineUserDataJobOperation> offlineUserDataJobOperationList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            offlineUserDataJobOperationList.add(OfflineUserDataJobOperation.newBuilder()
                    .setCreate(
                            UserData.newBuilder()
                                    .addUserIdentifiers(
                                            UserIdentifier.newBuilder()
                                                    .setHashedEmail(toSHA256String("dummyEmail" + i + "@gmail.com"))
                                                    .build())
                                    .build())
                    .build());
        }
        return offlineUserDataJobOperationList;
    }

    public void uploadList() throws IOException {
        List<OfflineUserDataJobOperation> offlineUserDataJobOperationList = prepareList();

        logger.info("Loading the properties file...");
        Properties properties = new Properties();
        InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties");
        properties.load(input);
        logger.info("Properties loaded!");

        String customerId = properties.getProperty(GOOGLEADS_CUSTOMER_ID);
        String audienceId = properties.getProperty(GOOGLEADS_AUDIENCE_ID);

        logger.info("Creating GoogleAdsClient...");

        GoogleAdsClient googleAdsClient =
                GoogleAdsClient.newBuilder().fromProperties(properties).build();

        logger.info("GoogleAdsClient created!");

        String userListResourceName =
                ResourceNames.userList(Long.parseLong(customerId), Long.parseLong(audienceId));

        logger.info("User List Resource Name: " + userListResourceName);

        logger.info("Creating OfflineUserDataJob...");

        OfflineUserDataJob offlineUserDataJob =
                OfflineUserDataJob.newBuilder()
                        .setType(OfflineUserDataJobTypeEnum.OfflineUserDataJobType.CUSTOMER_MATCH_USER_LIST)
                        .setCustomerMatchUserListMetadata(
                                CustomerMatchUserListMetadata.newBuilder().setUserList(userListResourceName))
                        .build();

        logger.info("OfflineUserDataJob created!");

        logger.info("Creating OfflineUserDataJobServiceClient...");

        try (OfflineUserDataJobServiceClient offlineUserDataJobServiceClient =
                     googleAdsClient.getLatestVersion().createOfflineUserDataJobServiceClient()) {

            logger.info("OfflineUserDataJobServiceClient created!");

            logger.info("Sending CreateOfflineUserDataJob request...");

            CreateOfflineUserDataJobResponse createOfflineUserDataJobResponse =
                    offlineUserDataJobServiceClient.createOfflineUserDataJob(customerId, offlineUserDataJob);

            String offlineJobResourceName = createOfflineUserDataJobResponse.getResourceName();

            logger.info("Offline Job Resource Name: " + offlineJobResourceName);

            logger.info("Creating OfflineUserDataJobOperations request...");

            AddOfflineUserDataJobOperationsResponse response =
                    offlineUserDataJobServiceClient.addOfflineUserDataJobOperations(
                            AddOfflineUserDataJobOperationsRequest.newBuilder()
                                    .setResourceName(offlineJobResourceName)
                                    .setEnablePartialFailure(true)
                                    .addAllOperations(offlineUserDataJobOperationList)
                                    .build());

            logger.info("OfflineUserDataJobOperations request sent!");

            if (response.hasPartialFailureError()) {
                GoogleAdsFailure googleAdsFailure =
                        ErrorUtils.getInstance().getGoogleAdsFailure(response.getPartialFailureError());
                logger.warn("Encountered " + googleAdsFailure.getErrorsCount() + "partial failure errors " +
                        "while adding " + offlineUserDataJobOperationList.size() + " operations to the offline " +
                        "user data job: " + response.getPartialFailureError().getMessage() + ". Only the successfully " +
                        "added operations will be executed when the job runs.");
            }

            logger.info("Executing OfflineUserDataJob " + offlineJobResourceName);
            offlineUserDataJobServiceClient.runOfflineUserDataJobAsync(offlineJobResourceName);
            logger.info("OfflineUserDataJob " + offlineJobResourceName + " executed!");
        }
    }

    public static void main(String args[]) throws IOException {
        OfflineJobUploader offlineJobUploader = new OfflineJobUploader();
        offlineJobUploader.uploadList();
    }
}
