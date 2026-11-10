import AVFoundation

/// Looping background music driven by three switches: a track from the backend,
/// the user's preference, and whether the app is in the foreground.
/// Uses the `.ambient` audio session, so it mixes with other apps and obeys the silent switch.
@MainActor
final class BackgroundMusicPlayer {
    private var player: AVQueuePlayer?
    private var looper: AVPlayerLooper?
    private var track: MusicTrack?
    private var userEnabled = true
    private var inForeground = false

    func setTrack(_ track: MusicTrack?) {
        if track?.url != self.track?.url { releasePlayer() }
        self.track = track
        sync()
    }

    func setUserEnabled(_ enabled: Bool) {
        userEnabled = enabled
        // Switching off releases the player so nothing keeps streaming.
        if !enabled { releasePlayer() }
        sync()
    }

    func onForeground() {
        inForeground = true
        sync()
    }

    func onBackground() {
        inForeground = false
        sync()
    }

    private func sync() {
        guard let track, userEnabled, inForeground else {
            player?.pause()
            return
        }
        if player == nil { createPlayer(track) }
        player?.play()
    }

    private func createPlayer(_ track: MusicTrack) {
        guard let url = URL(string: track.url) else { return }
        try? AVAudioSession.sharedInstance().setCategory(.ambient, mode: .default, options: [.mixWithOthers])
        try? AVAudioSession.sharedInstance().setActive(true)
        let item = AVPlayerItem(url: url)
        let queue = AVQueuePlayer()
        queue.volume = track.volume
        looper = AVPlayerLooper(player: queue, templateItem: item)
        player = queue
    }

    private func releasePlayer() {
        player?.pause()
        looper?.disableLooping()
        looper = nil
        player?.removeAllItems()
        player = nil
    }
}
