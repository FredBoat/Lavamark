package com.frederikam.lavamark;

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Lavamark {

    private static final Logger log = LoggerFactory.getLogger(Lavamark.class);

    static final AudioPlayerManager PLAYER_MANAGER = new DefaultAudioPlayerManager();
    private static final String DEFAULT_PLAYLIST = "https://www.youtube.com/watch?v=7v154aLVo70&list=LLqqLoSLryroL7b7TAL8gfhQ&index=22";
    private static final long INTERVAL = 2 * 1000;
    private static final long STEP_SIZE = 20;
    private static final Object WAITER = new Object();

    private static List<AudioTrack> tracks;
    private static CopyOnWriteArrayList<Player> players = new CopyOnWriteArrayList<>();


    public static void main(String[] args) {
        /* Set up the player manager */
        PLAYER_MANAGER.enableGcMonitoring();
        PLAYER_MANAGER.setItemLoaderThreadPoolSize(100);
        PLAYER_MANAGER.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.LOW);
        AudioSourceManagers.registerRemoteSources(PLAYER_MANAGER);

        log.info("Loading AudioTracks");
        tracks = new PlaylistLoader().loadTracksSync(DEFAULT_PLAYLIST);
        log.info(tracks.size() + " tracks loaded. Beginning benchmark...");

        try {
            doLoop();
        } catch (Exception e) {
            log.error("Benchmark ended due to exception!");
            throw new RuntimeException(e);
        }
    }

    private static void doLoop() throws InterruptedException {
        AudioConsumer consumer = new AudioConsumer(players);
        consumer.start();

        //noinspection InfiniteLoopStatement
        while (true) {
            spawnPlayers();

            AudioConsumer.Results results = consumer.getResults();
            log.info("Players: " + players.size() + ", Null frames: " + results.getLossPercentString());

            if(results.getEndReason() != AudioConsumer.EndReason.NONE) {
                log.info("Benchmark ended. Reason: " + results.getEndReason());
                break;
            }

            synchronized (WAITER) {
                WAITER.wait(INTERVAL);
            }
        }
    }

    private static void spawnPlayers() {
        for (int i = 0; i < STEP_SIZE; i++) {
            players.add(new Player());
        }
    }

    static AudioTrack getTrack() {
        int rand = (int)(Math.random() * tracks.size());
        return tracks.get(rand).makeClone();
    }

}
