package sashwind.mc.plugin.ffcraft.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sashwind.mc.plugin.ffcraft.model.*;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("事件模型与便捷方法测试")
public class EventModelTest {

    private ServerVideoPlayer createTestPlayer() {
        UUID playerId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        VideoSource video1 = new VideoSource("https://example.com/video1.mp4", 1920, 1080, 30);
        VideoSource video2 = new VideoSource("https://example.com/video2.mp4", 1280, 720, 60);

        PlaybackState state = new PlaybackState(
            PlaybackStatus.PLAYING,
            PlaybackMode.SEQUENTIAL,
            0,
            45,
            80,
            System.currentTimeMillis() / 1000
        );

        return new ServerVideoPlayer(
            playerId,
            "测试大屏幕",
            true,
            ownerId,
            java.util.Collections.emptySet(),
            List.of(video1, video2),
            state,
            List.of()
        );
    }

    @Test
    @DisplayName("测试状态改变事件：提取当前URL与状态变更判断")
    void testPlaybackStateChangeEvent() {
        ServerVideoPlayer player = createTestPlayer();
        PlaybackState oldState = player.playbackState();
        PlaybackState newState = oldState.withStatus(PlaybackStatus.PAUSED);

        VideoPlayerPlaybackStateChangeEvent event = new VideoPlayerPlaybackStateChangeEvent(
            player, null, oldState, newState
        );

        // 验证便捷获取当前正在播放的 URL
        assertTrue(event.getCurrentUrl().isPresent());
        assertEquals("https://example.com/video1.mp4", event.getCurrentUrl().get());
        assertEquals(1920, event.getCurrentVideoSource().get().targetWidth());

        // 验证状态辅助判断
        assertTrue(event.isStatusChanged());
        assertFalse(event.isIndexChanged());
        assertFalse(event.isModeChanged());
        assertFalse(event.isVolumeChanged());

        // 验证 Cancellable
        assertFalse(event.isCancelled());
        event.setCancelled(true);
        assertTrue(event.isCancelled());
    }

    @Test
    @DisplayName("测试切歌事件：提取上一首与下一首的URL")
    void testTrackChangeEvent() {
        ServerVideoPlayer player = createTestPlayer();

        VideoPlayerTrackChangeEvent event = new VideoPlayerTrackChangeEvent(
            player, null, 0, 1
        );

        assertEquals(0, event.getPreviousIndex());
        assertEquals(1, event.getNewIndex());

        // 验证上一首 URL 与下一首 URL
        assertTrue(event.getPreviousUrl().isPresent());
        assertEquals("https://example.com/video1.mp4", event.getPreviousUrl().get());

        assertTrue(event.getNewUrl().isPresent());
        assertEquals("https://example.com/video2.mp4", event.getNewUrl().get());

        // 验证可以取消切歌
        event.setCancelled(true);
        assertTrue(event.isCancelled());
    }

    @Test
    @DisplayName("测试播放列表添加事件与URL获取")
    void testPlaylistAddEvent() {
        ServerVideoPlayer player = createTestPlayer();
        VideoSource newVideo = new VideoSource("https://bilibili.com/av123", 1920, 1080, null);

        VideoPlaylistAddEvent event = new VideoPlaylistAddEvent(player, null, newVideo, 2);

        assertEquals("https://bilibili.com/av123", event.getUrl());
        assertEquals(2, event.getIndex());
        assertEquals(newVideo, event.getVideoSource());

        event.setCancelled(true);
        assertTrue(event.isCancelled());
    }
}
