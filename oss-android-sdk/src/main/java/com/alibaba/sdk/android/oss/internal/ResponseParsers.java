package com.alibaba.sdk.android.oss.internal;

import android.util.Xml;

import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.common.OSSHeaders;
import com.alibaba.sdk.android.oss.common.utils.DateUtil;
import com.alibaba.sdk.android.oss.common.utils.OSSUtils;
import com.alibaba.sdk.android.oss.model.AbortMultipartUploadResult;
import com.alibaba.sdk.android.oss.model.AppendObjectResult;
import com.alibaba.sdk.android.oss.model.CompleteMultipartUploadResult;
import com.alibaba.sdk.android.oss.model.CopyObjectResult;
import com.alibaba.sdk.android.oss.model.DeleteBucketResult;
import com.alibaba.sdk.android.oss.model.DeleteObjectResult;
import com.alibaba.sdk.android.oss.model.GetBucketACLResult;
import com.alibaba.sdk.android.oss.model.GetObjectResult;
import com.alibaba.sdk.android.oss.model.HeadObjectResult;
import com.alibaba.sdk.android.oss.model.InitiateMultipartUploadResult;
import com.alibaba.sdk.android.oss.model.ListObjectsResult;
import com.alibaba.sdk.android.oss.model.ListPartsResult;
import com.alibaba.sdk.android.oss.model.OSSObjectSummary;
import com.alibaba.sdk.android.oss.model.ObjectMetadata;
import com.alibaba.sdk.android.oss.model.Owner;
import com.alibaba.sdk.android.oss.model.PartSummary;
import com.alibaba.sdk.android.oss.model.CreateBucketResult;
import com.alibaba.sdk.android.oss.model.PutObjectResult;
import com.alibaba.sdk.android.oss.model.UploadPartResult;

import okhttp3.Response;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by zhouzhuo on 11/23/15.
 */
public final class ResponseParsers {

    public static final class PutObjectResponseParser extends AbstractResponseParser<PutObjectResult> {

        @Override
        public PutObjectResult parseData(Response response, PutObjectResult result)
                throws IOException {
            result.setETag(trimQuotes(response.header(OSSHeaders.ETAG)));
            if (response.body().contentLength() > 0) {
                result.setServerCallbackReturnBody(response.body().string());
            }
            return result;
        }
    }

    public static final class AppendObjectResponseParser extends AbstractResponseParser<AppendObjectResult> {

        @Override
        public AppendObjectResult parseData(Response response, AppendObjectResult result) throws IOException {
            String nextPosition = response.header(OSSHeaders.OSS_NEXT_APPEND_POSITION);
            if (nextPosition != null) {
                result.setNextPosition(Long.valueOf(nextPosition));
            }
            result.setObjectCRC64(response.header(OSSHeaders.OSS_HASH_CRC64_ECMA));
            return result;
        }
    }

    public static final class HeadObjectResponseParser extends AbstractResponseParser<HeadObjectResult> {

        @Override
        public HeadObjectResult parseData(Response response, HeadObjectResult result) throws IOException {
            result.setMetadata(parseObjectMetadata(result.getResponseHeader()));
            return result;
        }
    }

    public static final class GetObjectResponseParser extends AbstractResponseParser<GetObjectResult> {

        @Override
        public GetObjectResult parseData(Response response, GetObjectResult result) throws IOException {
            result.setMetadata(parseObjectMetadata(result.getResponseHeader()));
            result.setContentLength(response.body().contentLength());
            result.setObjectContent(response.body().byteStream());
            return result;
        }

        @Override
        public boolean needCloseResponse() {
            // keep body stream open for reading content
            return false;
        }
    }

    public static final class CopyObjectResponseParser extends AbstractResponseParser<CopyObjectResult> {

        @Override
        public CopyObjectResult parseData(Response response, CopyObjectResult result) throws Exception {
            result = parseCopyObjectResponseXML(response.body().byteStream(), result);
            return result;
        }
    }

    public static final class CreateBucketResponseParser extends AbstractResponseParser<CreateBucketResult> {

        @Override
        public CreateBucketResult parseData(Response response, CreateBucketResult result) throws IOException {
            if (result.getResponseHeader().containsKey("Location")) {
                result.bucketLocation = result.getResponseHeader().get("Location");
            }
            return result;
        }
    }

    public static final class DeleteBucketResponseParser extends AbstractResponseParser<DeleteBucketResult> {

        @Override
        public DeleteBucketResult parseData(Response response, DeleteBucketResult result) throws IOException {
            return result;
        }
    }

    public static final class GetBucketACLResponseParser extends AbstractResponseParser<GetBucketACLResult> {

        @Override
        public GetBucketACLResult parseData(Response response, GetBucketACLResult result) throws Exception {
            result = parseGetBucketACLResponse(response.body().byteStream(), result);
            return result;
        }
    }


    public static final class DeleteObjectResponseParser extends AbstractResponseParser<DeleteObjectResult> {

        @Override
        public DeleteObjectResult parseData(Response response, DeleteObjectResult result) throws IOException {
            return result;
        }
    }

    public static final class ListObjectsResponseParser extends AbstractResponseParser<ListObjectsResult> {

        @Override
        public ListObjectsResult parseData(Response response, ListObjectsResult result) throws Exception {
            result = parseObjectListResponse(response.body().byteStream(), result);
            return result;
        }
    }

    public static final class InitMultipartResponseParser extends AbstractResponseParser<InitiateMultipartUploadResult> {

        @Override
        public InitiateMultipartUploadResult parseData(Response response, InitiateMultipartUploadResult result) throws Exception {
            return parseInitMultipartResponseXML(response.body().byteStream(), result);
        }
    }

    public static final class UploadPartResponseParser extends AbstractResponseParser<UploadPartResult> {

        @Override
        public UploadPartResult parseData(Response response, UploadPartResult result) throws IOException {
            result.setETag(trimQuotes(response.header(OSSHeaders.ETAG)));
            return result;
        }
    }

    public static final class AbortMultipartUploadResponseParser extends AbstractResponseParser<AbortMultipartUploadResult> {

        @Override
        public AbortMultipartUploadResult parseData(Response response, AbortMultipartUploadResult result) throws IOException {
            return result;
        }
    }

    public static final class CompleteMultipartUploadResponseParser extends AbstractResponseParser<CompleteMultipartUploadResult> {

        @Override
        public CompleteMultipartUploadResult parseData(Response response, CompleteMultipartUploadResult result) throws Exception {
            if (response.header(OSSHeaders.CONTENT_TYPE).equals("application/xml")) {
                result = parseCompleteMultipartUploadResponseXML(response.body().byteStream(), result);
            } else if (response.body() != null) {
                result.setServerCallbackReturnBody(response.body().string());
            }
            return result;
        }
    }

    public static final class ListPartsResponseParser extends AbstractResponseParser<ListPartsResult> {

        @Override
        public ListPartsResult parseData(Response response, ListPartsResult result) throws Exception {
            result = parseListPartsResponseXML(response.body().byteStream(), result);
            return result;
        }
    }

    private static CopyObjectResult parseCopyObjectResponseXML(InputStream in, CopyObjectResult result)
            throws XmlPullParserException, IOException, ParseException {

        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(in, "utf-8");
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    String name = parser.getName();
                    if ("LastModified".equals(name)) {
                        result.setLastModified(DateUtil.parseIso8601Date(parser.nextText()));
                    } else if ("ETag".equals(name)) {
                        result.setEtag(parser.nextText());
                    }
                    break;
            }

            eventType = parser.next();
            if (eventType == XmlPullParser.TEXT) {
                eventType = parser.next();
            }
        }

        return result;
    }

    private static ListPartsResult parseListPartsResponseXML(InputStream in, ListPartsResult result)
            throws IOException, XmlPullParserException, ParseException {

        List<PartSummary> partEtagList = new ArrayList<PartSummary>();
        PartSummary partSummary = null;
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(in, "utf-8");
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    String name = parser.getName();
                    if ("Bucket".equals(name)) {
                        result.setBucketName(parser.nextText());
                    } else if ("Key".equals(name)) {
                        result.setKey(parser.nextText());
                    } else if ("UploadId".equals(name)) {
                        result.setUploadId(parser.nextText());
                    } else if ("PartNumberMarker".equals(name)) {
                        String partNumberMarker = parser.nextText();
                        if (!OSSUtils.isEmptyString(partNumberMarker)) {
                            result.setPartNumberMarker(Integer.valueOf(partNumberMarker));
                        }
                    } else if ("NextPartNumberMarker".equals(name)) {
                        String nextPartNumberMarker = parser.nextText();
                        if (!OSSUtils.isEmptyString(nextPartNumberMarker)) {
                            result.setNextPartNumberMarker(Integer.valueOf(nextPartNumberMarker));
                        }
                    } else if ("MaxParts".equals(name)) {
                        String maxParts = parser.nextText();
                        if (!OSSUtils.isEmptyString(maxParts)) {
                            result.setMaxParts(Integer.valueOf(maxParts));
                        }
                    } else if ("IsTruncated".equals(name)) {
                        String isTruncated = parser.nextText();
                        if (!OSSUtils.isEmptyString(isTruncated)) {
                            result.setTruncated(Boolean.valueOf(isTruncated));
                        }
                    } else if ("StorageClass".equals(name)) {
                        result.setStorageClass(parser.nextText());
                    } else if ("Part".equals(name)) {
                        partSummary = new PartSummary();
                    } else if ("PartNumber".equals(name)) {
                        String partNum = parser.nextText();
                        if(!OSSUtils.isEmptyString(partNum)) {
                            partSummary.setPartNumber(Integer.valueOf(partNum));
                        }
                    } else if ("LastModified".equals(name)) {
                        partSummary.setLastModified(DateUtil.parseIso8601Date(parser.nextText()));
                    } else if ("ETag".equals(name)) {
                        partSummary.setETag(parser.nextText());
                    } else if ("Size".equals(name)) {
                        String size = parser.nextText();
                        if(!OSSUtils.isEmptyString(size)) {
                            partSummary.setSize(Long.valueOf(size));
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if ("Part".equals(parser.getName())) {
                        partEtagList.add(partSummary);
                    }
                    break;
            }

            eventType = parser.next();
            if (eventType == XmlPullParser.TEXT) {
                eventType = parser.next();
            }
        }

        if (partEtagList.size() > 0) {
            result.setParts(partEtagList);
        }

        return result;
    }

    private static CompleteMultipartUploadResult parseCompleteMultipartUploadResponseXML(InputStream in, CompleteMultipartUploadResult result)
            throws IOException, XmlPullParserException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(in, "utf-8");
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    String name = parser.getName();
                    if ("Location".equals(name)) {
                        result.setLocation(parser.nextText());
                    } else if ("Bucket".equals(name)) {
                        result.setBucketName(parser.nextText());
                    } else if ("Key".equals(name)) {
                        result.setObjectKey(parser.nextText());
                    } else if ("ETag".equals(name)) {
                        result.setETag(parser.nextText());
                    }
                    break;
            }

            eventType = parser.next();
            if (eventType == XmlPullParser.TEXT) {
                eventType = parser.next();
            }
        }

        return result;
    }

    private static InitiateMultipartUploadResult parseInitMultipartResponseXML(InputStream in, InitiateMultipartUploadResult result)
            throws XmlPullParserException, IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(in, "utf-8");
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    String name = parser.getName();
                    if ("Bucket".equals(name)) {
                        result.setBucketName(parser.nextText());
                    } else if ("Key".equals(name)) {
                        result.setObjectKey(parser.nextText());
                    } else if ("UploadId".equals(name)) {
                        result.setUploadId(parser.nextText());
                    }
                    break;
            }

            eventType = parser.next();
            if (eventType == XmlPullParser.TEXT) {
                eventType = parser.next();
            }
        }
        return result;
    }

    /**
     * Parse the response of GetBucketACL
     *
     * @param in
     * @return
     * @throws Exception
     */
    private static GetBucketACLResult parseGetBucketACLResponse(InputStream in, GetBucketACLResult result)
            throws XmlPullParserException, IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(in, "utf-8");
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    String name = parser.getName();
                    if ("Grant".equals(name)) {
                        result.setBucketACL(parser.nextText());
                    } else if ("ID".equals(name)) {
                        result.setBucketOwnerID(parser.nextText());
                    } else if ("DisplayName".equals(name)) {
                        result.setBucketOwner(parser.nextText());
                    }
                    break;
            }

            eventType = parser.next();
            if (eventType == XmlPullParser.TEXT) {
                eventType = parser.next();
            }
        }
        return result;
    }

    /**
     * Parse the response of listObjectInBucket
     *
     * @param in
     * @return
     * @throws Exception
     */
    private static ListObjectsResult parseObjectListResponse(InputStream in, ListObjectsResult result)
            throws XmlPullParserException, IOException, ParseException {
        result.clearCommonPrefixes();
        result.clearObjectSummaries();
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(in, "utf-8");
        int eventType = parser.getEventType();
        OSSObjectSummary object = null;
        Owner owner = null;
        boolean isCommonPrefixes = false;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    String name = parser.getName();
                    if ("Name".equals(name)) {
                        result.setBucketName(parser.nextText());
                    } else if ("Prefix".equals(name)) {
                        if (isCommonPrefixes) {
                            String commonPrefix = parser.nextText();
                            if (!OSSUtils.isEmptyString(commonPrefix)) {
                                result.addCommonPrefix(commonPrefix);
                            }
                        } else {
                            result.setPrefix(parser.nextText());
                        }

                    } else if ("Marker".equals(name)) {
                        result.setMarker(parser.nextText());
                    } else if ("Delimiter".equals(name)) {
                        result.setDelimiter(parser.nextText());
                    } else if ("EncodingType".equals(name)) {
                        result.setEncodingType(parser.nextText());
                    } else if ("MaxKeys".equals(name)) {
                        String maxKeys = parser.nextText();
                        if (!OSSUtils.isEmptyString(maxKeys)) {
                            result.setMaxKeys(Integer.valueOf(maxKeys));
                        }
                    } else if ("NextMarker".equals(name)) {
                        result.setNextMarker(parser.nextText());
                    } else if ("IsTruncated".equals(name)) {
                        String isTruncated = parser.nextText();
                        if (!OSSUtils.isEmptyString(isTruncated)) {
                            result.setTruncated(Boolean.valueOf(isTruncated));
                        }
                    } else if ("Contents".equals(name)) {
                        object = new OSSObjectSummary();
                    } else if ("Key".equals(name)) {
                        object.setKey(parser.nextText());
                    } else if ("LastModified".equals(name)) {
                        object.setLastModified(DateUtil.parseIso8601Date(parser.nextText()));
                    } else if ("Size".equals(name)) {
                        String size = parser.nextText();
                        if(!OSSUtils.isEmptyString(size)) {
                            object.setSize(Long.valueOf(size));
                        }
                    } else if ("ETag".equals(name)) {
                        object.setETag(parser.nextText());
                    } else if ("Type".equals(name)) {
                        object.setType(parser.nextText());
                    } else if ("StorageClass".equals(name)) {
                        object.setStorageClass(parser.nextText());
                    } else if ("Owner".equals(name)) {
                        owner = new Owner();
                    } else if ("ID".equals(name)) {
                        owner.setId(parser.nextText());
                    } else if ("DisplayName".equals(name)) {
                        owner.setDisplayName(parser.nextText());
                    } else if ("CommonPrefixes".equals(name)) {
                        isCommonPrefixes = true;
                    }
                    break;
                case XmlPullParser.END_TAG:
                    String endTagName = parser.getName();
                    if ("Owner".equals(parser.getName())) {
                        if (owner != null) {
                            object.setOwner(owner);
                        }
                    } else if ("Contents".equals(endTagName)) {
                        if (object != null) {
                            object.setBucketName(result.getBucketName());
                            result.addObjectSummary(object);
                        }
                    } else if ("CommonPrefixes".equals(endTagName)) {
                        isCommonPrefixes = false;
                    }
                    break;
            }

            eventType = parser.next();
            if (eventType == XmlPullParser.TEXT) {
                eventType = parser.next();
            }
        }

        return result;
    }

    public static String trimQuotes(String s) {
        if (s == null) return null;

        s = s.trim();
        if (s.startsWith("\"")) s = s.substring(1);
        if (s.endsWith("\"")) s = s.substring(0, s.length() - 1);

        return s;
    }

    /**
     * Unmarshall object metadata from response headers.
     */
    public static ObjectMetadata parseObjectMetadata(Map<String, String> headers)
            throws IOException {

        try {
            ObjectMetadata objectMetadata = new ObjectMetadata();

            for (Iterator<String> it = headers.keySet().iterator(); it.hasNext(); ) {
                String key = it.next();

                if (key.indexOf(OSSHeaders.OSS_USER_METADATA_PREFIX) >= 0) {
                    objectMetadata.addUserMetadata(key, headers.get(key));
                } else if (key.equals(OSSHeaders.LAST_MODIFIED) || key.equals(OSSHeaders.DATE)) {
                    try {
                        objectMetadata.setHeader(key, DateUtil.parseRfc822Date(headers.get(key)));
                    } catch (ParseException pe) {
                        throw new IOException(pe.getMessage(), pe);
                    }
                } else if (key.equals(OSSHeaders.CONTENT_LENGTH)) {
                    Long value = Long.valueOf(headers.get(key));
                    objectMetadata.setHeader(key, value);
                } else if (key.equals(OSSHeaders.ETAG)) {
                    objectMetadata.setHeader(key, trimQuotes(headers.get(key)));
                } else {
                    objectMetadata.setHeader(key, headers.get(key));
                }
            }

            return objectMetadata;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public static ServiceException parseResponseErrorXML(Response response, boolean isHeadRequest)
            throws ClientException {

        int statusCode = response.code();
        String requestId = response.header(OSSHeaders.OSS_HEADER_REQUEST_ID);
        String code = null;
        String message = null;
        String hostId = null;
        String errorMessage = null;
        if (!isHeadRequest) {
            try {
                errorMessage = response.body().string();
                InputStream inputStream = new ByteArrayInputStream(errorMessage.getBytes());
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(inputStream, "utf-8");
                int eventType = parser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    switch (eventType) {
                        case XmlPullParser.START_TAG:
                            if ("Code".equals(parser.getName())) {
                                code = parser.nextText();
                            } else if ("Message".equals(parser.getName())) {
                                message = parser.nextText();
                            } else if ("RequestId".equals(parser.getName())) {
                                requestId = parser.nextText();
                            } else if ("HostId".equals(parser.getName())) {
                                hostId = parser.nextText();
                            }
                            break;
                    }
                    eventType = parser.next();
                    if (eventType == XmlPullParser.TEXT) {
                        eventType = parser.next();
                    }
                }

            } catch (IOException e) {
                throw new ClientException(e);
            } catch (XmlPullParserException e) {
                throw new ClientException(e);
            }
        }

        return new ServiceException(statusCode, message, code, requestId, hostId, errorMessage);
    }

}
