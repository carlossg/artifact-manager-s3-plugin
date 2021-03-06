/*
 * The MIT License
 *
 * Copyright 2018 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.jenkins.plugins.artifact_manager_s3;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.hamcrest.Matchers.*;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.junit.After;
import static org.junit.Assume.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.LoggerRule;

public abstract class JCloudsAbstractTest {

    protected static final Logger LOGGER = Logger.getLogger(JCloudsBlobStoreTest.class.getName());

    private static final String S3_BUCKET = System.getenv("S3_BUCKET");
    protected static final String S3_DIR = System.getenv("S3_DIR");
    private static final String PROVIDER = System.getProperty("jclouds.provider", "aws-s3");

    @BeforeClass
    public static void live() {
        assumeThat("define $S3_BUCKET as explained in README", S3_BUCKET, notNullValue());
        assumeThat("define $S3_DIR as explained in README", S3_DIR, notNullValue());
        try {
            AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
            assumeTrue(S3_BUCKET + " bucket does not exist", builder.build().doesBucketExistV2(S3_BUCKET));
            assumeThat("can get credentials from environment", builder.getCredentials().getCredentials(), notNullValue());
        } catch (SdkClientException x) {
            assumeNoException("failed to connect to S3 with current credentials", x);
        }
    }

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Rule
    public LoggerRule loggerRule = new LoggerRule();

    protected BlobStoreContext context;
    protected BlobStore blobStore;
    private String prefix;

    protected static String getContainer() {
        return S3_BUCKET;
    }

    protected static String getProvider() {
        return PROVIDER;
    }

    protected String getPrefix() {
        return prefix;
    }

    @Before
    public void setupContext() throws Exception {
        loggerRule.recordPackage(JCloudsBlobStore.class, Level.FINE);

        // run each test under its own dir
        prefix = S3_DIR + ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT) + "/";

        context = ContextBuilder.newBuilder("aws-s3").credentialsSupplier(new S3BlobStore().getCredentialsSupplier())
                .buildView(BlobStoreContext.class);

        blobStore = context.getBlobStore();

        setup();
    }

    public void setup() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
        if (context != null) {
            context.close();
        }
    }
}
