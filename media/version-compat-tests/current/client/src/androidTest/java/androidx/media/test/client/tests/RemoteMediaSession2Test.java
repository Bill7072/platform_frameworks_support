/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.media.test.client.tests;

import static android.support.mediacompat.testlib.util.IntentUtil.SERVICE_PACKAGE_NAME;

import static androidx.media.test.lib.CommonConstants.DEFAULT_TEST_NAME;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.media.test.client.RemoteMediaSession2;
import androidx.media2.MediaController2;
import androidx.media2.SessionToken2;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Executor;

/** Test {@link RemoteMediaSession2}. */
@RunWith(AndroidJUnit4.class)
public class RemoteMediaSession2Test {

    private Context mContext;
    private RemoteMediaSession2 mRemoteSession2;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
        mRemoteSession2 = new RemoteMediaSession2(DEFAULT_TEST_NAME, mContext);
    }

    @After
    public void cleanUp() {
        mRemoteSession2.cleanUp();
    }

    @Test
    @SmallTest
    public void testGettingToken() {
        SessionToken2 token = mRemoteSession2.getToken();
        assertNotNull(token);
        assertEquals(SERVICE_PACKAGE_NAME, token.getPackageName());
    }

    @Test
    @SmallTest
    public void testCreatingController() {
        SessionToken2 token = mRemoteSession2.getToken();
        assertNotNull(token);
        MediaController2 controller = new MediaController2(mContext, token, new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        }, new MediaController2.ControllerCallback() {});
    }
}
