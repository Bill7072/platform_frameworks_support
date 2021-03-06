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

package androidx.media.test.service;

import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import androidx.media.AudioAttributesCompat;
import androidx.media2.DataSourceDesc2;
import androidx.media2.MediaPlayerConnector;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

/**
 * A mock implementation of {@link MediaPlayerConnector} for testing.
 */
public class MockPlayerConnector extends MediaPlayerConnector {
    public final CountDownLatch mCountDownLatch;
    public final boolean mChangePlayerStateWithTransportControl;

    public boolean mPlayCalled;
    public boolean mPauseCalled;
    public boolean mResetCalled;
    public boolean mPrepareCalled;
    public boolean mSeekToCalled;
    public boolean mSetPlaybackSpeedCalled;
    public long mSeekPosition;
    public long mCurrentPosition;
    public long mBufferedPosition;
    public float mPlaybackSpeed = 1.0f;
    public @PlayerState int mLastPlayerState;
    public @BuffState int mLastBufferingState;
    public long mDuration;
    public float mVolume;

    public ArrayMap<PlayerEventCallback, Executor> mCallbacks = new ArrayMap<>();

    private AudioAttributesCompat mAudioAttributes;

    public MockPlayerConnector(int count) {
        this(count, false);
    }

    public MockPlayerConnector(boolean changePlayerStateWithTransportControl) {
        this(0, changePlayerStateWithTransportControl);
    }

    private MockPlayerConnector(int count, boolean changePlayerStateWithTransportControl) {
        mCountDownLatch = (count > 0) ? new CountDownLatch(count) : null;
        mVolume = getMaxPlayerVolume();
        mChangePlayerStateWithTransportControl = changePlayerStateWithTransportControl;
        // This prevents MS2#play() from triggering MediaPlayerConnector#prepare().
        mLastPlayerState = PLAYER_STATE_PAUSED;

        // Sets default audio attributes to prevent setVolume() from being called with the play().
        mAudioAttributes = new AudioAttributesCompat.Builder()
                .setUsage(AudioAttributesCompat.USAGE_MEDIA).build();
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public void reset() {
        mResetCalled = true;
        if (mCountDownLatch != null) {
            mCountDownLatch.countDown();
        }
        if (mChangePlayerStateWithTransportControl) {
            notifyPlayerStateChanged(PLAYER_STATE_IDLE);
        }
    }

    @Override
    public void play() {
        mPlayCalled = true;
        if (mCountDownLatch != null) {
            mCountDownLatch.countDown();
        }
        if (mChangePlayerStateWithTransportControl) {
            notifyPlayerStateChanged(PLAYER_STATE_PLAYING);
        }
    }

    @Override
    public void pause() {
        mPauseCalled = true;
        if (mCountDownLatch != null) {
            mCountDownLatch.countDown();
        }
        if (mChangePlayerStateWithTransportControl) {
            notifyPlayerStateChanged(PLAYER_STATE_PAUSED);
        }
    }

    @Override
    public void prepare() {
        mPrepareCalled = true;
        if (mCountDownLatch != null) {
            mCountDownLatch.countDown();
        }
        if (mChangePlayerStateWithTransportControl) {
            notifyPlayerStateChanged(PLAYER_STATE_PAUSED);
        }
    }

    @Override
    public void seekTo(long pos) {
        mSeekToCalled = true;
        mSeekPosition = pos;
        if (mCountDownLatch != null) {
            mCountDownLatch.countDown();
        }
    }

    @Override
    public void skipToNext() {
        // No-op. This skipToNext() means 'skip to next item in the setNextDataSources()'
    }

    @Override
    public int getPlayerState() {
        return mLastPlayerState;
    }

    @Override
    public long getCurrentPosition() {
        return mCurrentPosition;
    }

    @Override
    public long getBufferedPosition() {
        return mBufferedPosition;
    }

    @Override
    public float getPlaybackSpeed() {
        return mPlaybackSpeed;
    }

    @Override
    public int getBufferingState() {
        return mLastBufferingState;
    }

    @Override
    public long getDuration() {
        return mDuration;
    }

    @Override
    public void registerPlayerEventCallback(@NonNull Executor executor,
            @NonNull PlayerEventCallback callback) {
        if (callback == null || executor == null) {
            throw new IllegalArgumentException("callback=" + callback + " executor=" + executor);
        }
        mCallbacks.put(callback, executor);
    }

    @Override
    public void unregisterPlayerEventCallback(@NonNull PlayerEventCallback callback) {
        mCallbacks.remove(callback);
    }

    public void notifyPlayerStateChanged(final int state) {
        mLastPlayerState = state;
        for (int i = 0; i < mCallbacks.size(); i++) {
            final PlayerEventCallback callback = mCallbacks.keyAt(i);
            final Executor executor = mCallbacks.valueAt(i);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onPlayerStateChanged(MockPlayerConnector.this, state);
                }
            });
        }
    }

    public void notifyCurrentDataSourceChanged(final DataSourceDesc2 dsd) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            final PlayerEventCallback callback = mCallbacks.keyAt(i);
            final Executor executor = mCallbacks.valueAt(i);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onCurrentDataSourceChanged(MockPlayerConnector.this, dsd);
                }
            });
        }
    }

    public void notifyMediaPrepared(final DataSourceDesc2 dsd) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            final PlayerEventCallback callback = mCallbacks.keyAt(i);
            final Executor executor = mCallbacks.valueAt(i);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onMediaPrepared(MockPlayerConnector.this, dsd);
                }
            });
        }
    }

    public void notifyBufferingStateChanged(final DataSourceDesc2 dsd,
            final @BuffState int buffState) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            final PlayerEventCallback callback = mCallbacks.keyAt(i);
            final Executor executor = mCallbacks.valueAt(i);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onBufferingStateChanged(MockPlayerConnector.this, dsd, buffState);
                }
            });
        }
    }

    public void notifyPlaybackSpeedChanged(final float speed) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            final PlayerEventCallback callback = mCallbacks.keyAt(i);
            final Executor executor = mCallbacks.valueAt(i);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onPlaybackSpeedChanged(MockPlayerConnector.this, speed);
                }
            });
        }
    }

    public void notifySeekCompleted(final long position) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            final PlayerEventCallback callback = mCallbacks.keyAt(i);
            final Executor executor = mCallbacks.valueAt(i);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onSeekCompleted(MockPlayerConnector.this, position);
                }
            });
        }
    }

    public void notifyError(int what) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            final PlayerEventCallback callback = mCallbacks.keyAt(i);
            final Executor executor = mCallbacks.valueAt(i);
            // TODO: Uncomment or remove
            //executor.execute(() -> callback.onError(null, what, 0));
        }
    }

    @Override
    public void setAudioAttributes(AudioAttributesCompat attributes) {
        mAudioAttributes = attributes;
    }

    @Override
    public AudioAttributesCompat getAudioAttributes() {
        return mAudioAttributes;
    }

    @Override
    public void setDataSource(@NonNull DataSourceDesc2 dsd) {
        // TODO: Implement this
    }

    @Override
    public void setNextDataSource(@NonNull DataSourceDesc2 dsd) {
        // TODO: Implement this
    }

    @Override
    public void setNextDataSources(@NonNull List<DataSourceDesc2> dsds) {
        // TODO: Implement this
    }

    @Override
    public DataSourceDesc2 getCurrentDataSource() {
        // TODO: Implement this
        return null;
    }

    @Override
    public void loopCurrent(boolean loop) {
        // TODO: implement this
    }

    @Override
    public void setPlaybackSpeed(float speed) {
        mSetPlaybackSpeedCalled = true;
        mPlaybackSpeed = speed;
        if (mCountDownLatch != null) {
            mCountDownLatch.countDown();
        }
    }

    @Override
    public float getMaxPlayerVolume() {
        return 1.0f;
    }

    @Override
    public void setPlayerVolume(float volume) {
        mVolume = volume;
        if (mCountDownLatch != null) {
            mCountDownLatch.countDown();
        }
    }

    @Override
    public float getPlayerVolume() {
        return mVolume;
    }
}
