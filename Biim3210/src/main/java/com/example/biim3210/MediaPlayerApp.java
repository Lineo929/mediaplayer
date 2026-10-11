package com.example.biim3210;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Spotify-style JavaFX Media Player.
 *
 * Launch behaviour:
 *  - Opens in TRUE fullscreen
 *  - Auto-loads Music folder
 *  - Auto-plays the first track (staying on the 3-card idle view)
 */
public class MediaPlayerApp {

    private final Stage stage;

    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private final Playlist playlist = new Playlist();

    // Main containers
    private StackPane cardsContainer;
    private VBox idleCards;
    private VBox heroCard;
    private ListView<File> heroPlaylistView;
    private ListView<File> idlePlaylistView;

    // Hero card fields
    private Slider volumeSlider;
    private Label nowPlayingLabel;
    private Label statusLabel;
    private Label heroTitle;
    private Label heroArtist;
    private Label heroFormat;
    private Label heroSize;

    // Idle card fields (live)
    private Label idleNowPlayingTitle;
    private Label idleNowPlayingArtist;
    private Label idleNowPlayingStatus;
    private Label idleDetailFormat;
    private Label idleDetailSize;
    private Label idleDetailFolder;
    private Label idleDetailPath;

    // Header
    private Button fullscreenButton;

    private boolean muted = false;
    private boolean autoPlaying = false;   // suppresses auto-jump to hero on startup

    private static final String[] SUPPORTED_EXTENSIONS = {
            ".mp3", ".mp4", ".m4a", ".wav", ".aac", ".flv", ".m4v"
    };

    public MediaPlayerApp(Stage stage) {
        this.stage = stage;
    }

    public void start() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setPadding(new Insets(16));

        root.setTop(buildHeader());

        mediaView = new MediaView();
        mediaView.setPreserveRatio(true);

        idleCards = buildIdleCards();
        heroCard = buildHeroCard();
        heroCard.setVisible(false);
        heroCard.setManaged(false);

        cardsContainer = new StackPane(idleCards, heroCard);
        cardsContainer.setPadding(new Insets(16, 0, 0, 0));
        root.setCenter(cardsContainer);

        Scene scene = new Scene(root, 1280, 800);
        var cssUrl = getClass().getResource("/com/example/biim3210/style.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
            System.out.println("✓ CSS loaded: " + cssUrl);
        } else {
            System.err.println("✗ CSS NOT FOUND at /com/example/biim3210/style.css");
        }
        scene.setOnKeyPressed(event -> handleKeyPress(event.getCode()));

        stage.setTitle("JavaFX Media Player");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);

        // Launch in TRUE fullscreen
        stage.setFullScreen(true);
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

        stage.show();

        // Auto-load + auto-play
        loadDefaultMusicFolder();
    }

    // ================================================================
    //  HEADER
    // ================================================================

    private HBox buildHeader() {
        Label title = new Label("♪  JavaFX Media Player");
        title.getStyleClass().add("header-label");
        Label sub = new Label("Spotify-style · Blue & Black");
        sub.getStyleClass().add("header-sub");
        VBox titleBox = new VBox(2, title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        fullscreenButton = new Button("⤢  Exit Fullscreen  (F)");
        fullscreenButton.getStyleClass().add("header-button");
        fullscreenButton.setOnAction(e -> toggleFullscreen());

        HBox header = new HBox(12, titleBox, spacer, fullscreenButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 8, 0));
        return header;
    }

    private void toggleFullscreen() {
        boolean nowFullscreen = !stage.isFullScreen();
        stage.setFullScreen(nowFullscreen);
        fullscreenButton.setText(nowFullscreen
                ? "⤢  Exit Fullscreen  (F)"
                : "⛶  Fullscreen  (F)");
    }

    // ================================================================
    //  AUTO-LOAD + AUTO-PLAY FROM MUSIC FOLDER
    // ================================================================

    private void loadDefaultMusicFolder() {
        String userHome = System.getProperty("user.home");
        File musicDir = new File(userHome, "Music");

        if (!musicDir.exists() || !musicDir.isDirectory()) {
            statusLabel.setText("Music folder not found: " + musicDir.getAbsolutePath());
            return;
        }

        List<File> found = new ArrayList<>();
        collectMediaFiles(musicDir, found);

        if (found.isEmpty()) {
            statusLabel.setText("No media files found in " + musicDir.getAbsolutePath());
            return;
        }

        for (File f : found) playlist.addFile(f);
        statusLabel.setText("Loaded " + found.size()
                + " file(s) from " + musicDir.getName() + " folder");

        // ---- Auto-play the first track ----
        if (!playlist.isEmpty()) {
            autoPlaying = true;             // stay on 3-card view
            playlist.setCurrentIndex(0);
            if (idlePlaylistView != null) {
                idlePlaylistView.getSelectionModel().select(0);
            }
            loadAndPlayCurrent();
        }
    }

    private void collectMediaFiles(File dir, List<File> out) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File f : children) {
            if (f.isDirectory()) {
                collectMediaFiles(f, out);
            } else {
                String lower = f.getName().toLowerCase();
                for (String ext : SUPPORTED_EXTENSIONS) {
                    if (lower.endsWith(ext)) {
                        out.add(f);
                        break;
                    }
                }
            }
        }
    }

    // ================================================================
    //  IDLE 3-CARD LAYOUT
    // ================================================================

    private VBox buildIdleCards() {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(40, 20, 40, 20));

        VBox playerCard = buildPlayerCard();

        idlePlaylistView = buildPlaylistView(280, 220);
        VBox queueCard = new VBox(8);
        queueCard.getStyleClass().add("card");
        queueCard.setAlignment(Pos.TOP_LEFT);
        queueCard.setPrefWidth(320);
        Label qTitle = new Label("☰  QUEUE");
        qTitle.getStyleClass().add("card-title");
        queueCard.getChildren().addAll(qTitle, idlePlaylistView, buildIdleQueueButtons());
        HBox.setHgrow(queueCard, Priority.ALWAYS);

        VBox detailsCard = buildDetailsCard();

        row.getChildren().addAll(playerCard, queueCard, detailsCard);

        VBox wrapper = new VBox(row);
        wrapper.setAlignment(Pos.CENTER);
        VBox.setVgrow(row, Priority.ALWAYS);
        return wrapper;
    }

    private VBox buildPlayerCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(300);
        card.setPrefHeight(400);

        Label titleLabel = new Label("▶  NOW PLAYING");
        titleLabel.getStyleClass().add("card-title");

        Label iconLabel = new Label("♫");
        iconLabel.getStyleClass().add("card-icon");

        idleNowPlayingTitle = new Label("No media loaded");
        idleNowPlayingTitle.getStyleClass().add("now-playing");
        idleNowPlayingTitle.setWrapText(true);
        idleNowPlayingTitle.setAlignment(Pos.CENTER);
        idleNowPlayingTitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        idleNowPlayingTitle.setMaxWidth(240);

        idleNowPlayingArtist = new Label("Add files to begin");
        idleNowPlayingArtist.getStyleClass().add("detail-label");
        idleNowPlayingArtist.setWrapText(true);
        idleNowPlayingArtist.setAlignment(Pos.CENTER);
        idleNowPlayingArtist.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        idleNowPlayingStatus = new Label("—");
        idleNowPlayingStatus.getStyleClass().add("status-label");

        Button prevBtn = new Button("⏮");
        prevBtn.setOnAction(e -> { autoPlaying = false; playPrevious(); });
        Button playBtn = new Button("⏯");
        playBtn.getStyleClass().add("play-button");
        playBtn.setOnAction(e -> togglePlayPause());
        Button nextBtn = new Button("⏭");
        nextBtn.setOnAction(e -> { autoPlaying = false; playNext(); });

        HBox miniControls = new HBox(10, prevBtn, playBtn, nextBtn);
        miniControls.setAlignment(Pos.CENTER);

        card.getChildren().addAll(
                titleLabel, iconLabel,
                idleNowPlayingTitle, idleNowPlayingArtist, idleNowPlayingStatus,
                miniControls
        );
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox buildDetailsCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.TOP_LEFT);
        card.setPrefWidth(300);
        card.setPrefHeight(400);

        Label titleLabel = new Label("ℹ  DETAILS");
        titleLabel.getStyleClass().add("card-title");

        idleDetailFormat = new Label("—");
        idleDetailFormat.getStyleClass().add("detail-value");
        idleDetailSize = new Label("—");
        idleDetailSize.getStyleClass().add("detail-value");
        idleDetailFolder = new Label("—");
        idleDetailFolder.getStyleClass().add("detail-value");
        idleDetailFolder.setWrapText(true);
        idleDetailPath = new Label("—");
        idleDetailPath.getStyleClass().add("detail-value");
        idleDetailPath.setWrapText(true);

        VBox rows = new VBox(8,
                detailPair("Format", idleDetailFormat),
                detailPair("Size", idleDetailSize),
                detailPair("Folder", idleDetailFolder)
        );

        Label pathTitle = new Label("Path:");
        pathTitle.getStyleClass().add("detail-label");
        VBox pathBox = new VBox(4, pathTitle, idleDetailPath);

        card.getChildren().addAll(titleLabel, rows, pathBox);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private HBox buildIdleQueueButtons() {
        Button addBtn = new Button("+ Add");
        addBtn.setOnAction(e -> openFiles());
        Button remBtn = new Button("− Remove");
        remBtn.setOnAction(e -> removeSelected(idlePlaylistView));
        HBox box = new HBox(8, addBtn, remBtn);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    // ================================================================
    //  HERO CARD
    // ================================================================

    private VBox buildHeroCard() {
        VBox hero = new VBox(16);
        hero.getStyleClass().add("hero-card");
        hero.setPadding(new Insets(24));
        hero.setMaxWidth(1100);
        hero.setMaxHeight(700);

        Button backButton = new Button("← Back to Library");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(e -> backToLibrary());

        Region topSpacerLeft = new Region();
        HBox.setHgrow(topSpacerLeft, Priority.ALWAYS);
        Region topSpacerRight = new Region();
        HBox.setHgrow(topSpacerRight, Priority.ALWAYS);

        heroTitle = new Label("Now Playing");
        heroTitle.getStyleClass().add("hero-title");
        heroArtist = new Label("—");
        heroArtist.getStyleClass().add("hero-artist");
        VBox titleBox = new VBox(2, heroTitle, heroArtist);
        titleBox.setAlignment(Pos.CENTER);

        HBox topRow = new HBox(backButton, topSpacerLeft, titleBox, topSpacerRight);
        topRow.setAlignment(Pos.CENTER);
        topRow.setPadding(new Insets(0, 0, 8, 0));

        mediaView.setFitWidth(720);
        mediaView.setFitHeight(405);
        StackPane videoFrame = new StackPane(mediaView);
        videoFrame.setStyle("-fx-background-color: #000; -fx-background-radius: 12;");
        videoFrame.setPrefSize(720, 405);
        videoFrame.setMaxSize(720, 405);

        heroPlaylistView = buildPlaylistView(280, 405);
        VBox queueBox = new VBox(8);
        Label qTitle = new Label("☰  QUEUE");
        qTitle.getStyleClass().add("card-title");
        queueBox.getChildren().addAll(qTitle, heroPlaylistView, buildHeroQueueButtons());

        HBox middle = new HBox(16, videoFrame, queueBox);
        middle.setAlignment(Pos.TOP_LEFT);

        nowPlayingLabel = new Label("—");
        nowPlayingLabel.getStyleClass().add("now-playing");
        statusLabel = new Label("Ready");
        statusLabel.getStyleClass().add("status-label");

        Button playPause = new Button("⏯  Play / Pause");
        playPause.getStyleClass().add("play-button");
        playPause.setOnAction(e -> togglePlayPause());

        Button stopBtn = new Button("⏹  Stop");
        stopBtn.setOnAction(e -> stop());

        Button prevBtn = new Button("⏮");
        prevBtn.setOnAction(e -> { autoPlaying = false; playPrevious(); });
        Button nextBtn = new Button("⏭");
        nextBtn.setOnAction(e -> { autoPlaying = false; playNext(); });

        Button muteBtn = new Button("🔊");
        muteBtn.setOnAction(e -> toggleMute());

        Button fullscreen = new Button("⛶");
        fullscreen.setOnAction(e -> toggleFullscreen());

        volumeSlider = new Slider(0, 1, 0.5);
        volumeSlider.setPrefWidth(140);
        volumeSlider.valueProperty().addListener((obs, o, n) -> {
            if (mediaPlayer != null && !muted) mediaPlayer.setVolume(n.doubleValue());
        });

        HBox controls = new HBox(10, prevBtn, playPause, stopBtn, nextBtn, muteBtn, fullscreen,
                new Label("Vol"), volumeSlider);
        controls.setAlignment(Pos.CENTER_LEFT);

        heroFormat = new Label("—");
        heroFormat.getStyleClass().add("detail-value");
        heroSize = new Label("—");
        heroSize.getStyleClass().add("detail-value");

        HBox details = new HBox(30,
                detailPair("Format", heroFormat),
                detailPair("Size", heroSize),
                detailPair("Keys", new Label("Space · S · N · P · ↑ · ↓ · M · F · Esc")));
        details.setAlignment(Pos.CENTER_LEFT);

        hero.getChildren().addAll(
                topRow, middle, new Separator(),
                nowPlayingLabel, statusLabel, controls, details
        );
        return hero;
    }

    private HBox detailPair(String label, Label value) {
        Label key = new Label(label + ":");
        key.getStyleClass().add("detail-label");
        HBox box = new HBox(6, key, value);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private HBox buildHeroQueueButtons() {
        Button addBtn = new Button("+ Add");
        addBtn.setOnAction(e -> openFiles());
        Button remBtn = new Button("− Remove");
        remBtn.setOnAction(e -> removeSelected(heroPlaylistView));
        HBox box = new HBox(8, addBtn, remBtn);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    // ================================================================
    //  LIST VIEW
    // ================================================================

    private ListView<File> buildPlaylistView(double w, double h) {
        ListView<File> lv = new ListView<>(playlist.getFiles());
        lv.setPrefSize(w, h);
        lv.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "♫  " + item.getName());
            }
        });
        lv.getSelectionModel().selectedIndexProperty().addListener((obs, o, n) -> {
            if (n != null && n.intValue() >= 0) {
                playlist.setCurrentIndex(n.intValue());
                autoPlaying = false;
                loadAndPlayCurrent();
            }
        });
        return lv;
    }

    // ================================================================
    //  VIEW SWITCHING
    // ================================================================

    private void showHeroCard() {
        if (heroCard.isVisible()) return;
        FadeTransition fadeOut = new FadeTransition(Duration.millis(220), idleCards);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            idleCards.setVisible(false);
            idleCards.setManaged(false);
            heroCard.setVisible(true);
            heroCard.setManaged(true);
            heroCard.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(220), heroCard);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        });
        fadeOut.play();
    }

    private void showIdleCards() {
        if (idleCards.isVisible()) return;
        FadeTransition fadeOut = new FadeTransition(Duration.millis(220), heroCard);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            heroCard.setVisible(false);
            heroCard.setManaged(false);
            idleCards.setVisible(true);
            idleCards.setManaged(true);
            idleCards.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(220), idleCards);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        });
        fadeOut.play();
    }

    private void backToLibrary() {
        showIdleCards();
    }

    // ================================================================
    //  FILE / PLAYBACK
    // ================================================================

    private void openFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Media Files");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Media Files",
                        "*.mp3", "*.mp4", "*.m4a", "*.wav", "*.aac", "*.flv", "*.m4v"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        var selected = chooser.showOpenMultipleDialog(stage);
        if (selected != null) {
            for (File f : selected) playlist.addFile(f);
            if (heroPlaylistView != null
                    && heroPlaylistView.getSelectionModel().getSelectedIndex() < 0
                    && !playlist.isEmpty()) {
                heroPlaylistView.getSelectionModel().select(0);
            }
        }
    }

    private void removeSelected(ListView<File> lv) {
        File sel = lv.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        playlist.removeFile(sel);
        if (playlist.isEmpty()) {
            stop();
            showIdleCards();
        } else {
            int idx = Math.min(playlist.getCurrentIndex(),
                    playlist.getFiles().size() - 1);
            lv.getSelectionModel().select(idx);
        }
    }

    private void loadAndPlayCurrent() {
        File file = playlist.getCurrentFile();
        if (file == null) return;

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        try {
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);
            mediaPlayer.setVolume(muted ? 0 : volumeSlider.getValue());

            String name = file.getName();
            int dot = name.lastIndexOf('.');
            String base = dot > 0 ? name.substring(0, dot) : name;
            String ext = dot > 0 ? name.substring(dot + 1).toUpperCase() : "—";

            heroTitle.setText(base);
            heroArtist.setText("Local file · " + file.getParentFile().getName());
            heroFormat.setText(ext);
            heroSize.setText(humanSize(file.length()));
            nowPlayingLabel.setText("🎵  " + name);
            statusLabel.setText("Loading…");

            if (idleNowPlayingTitle != null) {
                idleNowPlayingTitle.setText(base);
                idleNowPlayingArtist.setText("Local file · " + file.getParentFile().getName());
                idleNowPlayingStatus.setText("⏳ Loading…");
                idleDetailFormat.setText(ext);
                idleDetailSize.setText(humanSize(file.length()));
                idleDetailFolder.setText(file.getParentFile().getName());
                idleDetailPath.setText(file.getAbsolutePath());
            }

            mediaPlayer.setOnReady(() -> {
                statusLabel.setText("Playing");
                if (idleNowPlayingStatus != null) idleNowPlayingStatus.setText("▶  Playing");
                mediaPlayer.play();

                // Auto-play keeps the 3-card view.
                // User-initiated playback jumps to the hero card.
                if (!autoPlaying) {
                    showHeroCard();
                }
                autoPlaying = false;
            });
            mediaPlayer.setOnError(() ->
                    statusLabel.setText("Error: " + mediaPlayer.getError().getMessage()));
            mediaPlayer.setOnEndOfMedia(this::playNext);

        } catch (Exception ex) {
            statusLabel.setText("Failed to load: " + file.getName());
        }
    }

    private String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) { loadAndPlayCurrent(); return; }
        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            statusLabel.setText("Paused");
            if (idleNowPlayingStatus != null) idleNowPlayingStatus.setText("⏸  Paused");
        } else {
            mediaPlayer.play();
            statusLabel.setText("Playing");
            if (idleNowPlayingStatus != null) idleNowPlayingStatus.setText("▶  Playing");
        }
    }

    private void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.seek(Duration.ZERO);
            statusLabel.setText("Stopped");
            if (idleNowPlayingStatus != null) idleNowPlayingStatus.setText("⏹  Stopped");
        }
    }

    private void playNext() {
        if (playlist.isEmpty()) return;
        playlist.next();
        if (heroPlaylistView != null)
            heroPlaylistView.getSelectionModel().select(playlist.getCurrentIndex());
        loadAndPlayCurrent();
    }

    private void playPrevious() {
        if (playlist.isEmpty()) return;
        playlist.previous();
        if (heroPlaylistView != null)
            heroPlaylistView.getSelectionModel().select(playlist.getCurrentIndex());
        loadAndPlayCurrent();
    }

    private void toggleMute() {
        if (mediaPlayer == null) return;
        muted = !muted;
        mediaPlayer.setVolume(muted ? 0 : volumeSlider.getValue());
        statusLabel.setText(muted ? "Muted" : "Unmuted");
    }

    private void increaseVolume() {
        if (mediaPlayer == null) return;
        double v = Math.min(1.0, mediaPlayer.getVolume() + 0.05);
        mediaPlayer.setVolume(v);
        volumeSlider.setValue(v);
        if (v > 0) muted = false;
    }

    private void decreaseVolume() {
        if (mediaPlayer == null) return;
        double v = Math.max(0.0, mediaPlayer.getVolume() - 0.05);
        mediaPlayer.setVolume(v);
        volumeSlider.setValue(v);
        if (v == 0) muted = true;
    }

    private void handleKeyPress(KeyCode code) {
        switch (code) {
            case SPACE -> togglePlayPause();
            case S -> stop();
            case N -> { autoPlaying = false; playNext(); }
            case P -> { autoPlaying = false; playPrevious(); }
            case UP -> increaseVolume();
            case DOWN -> decreaseVolume();
            case M -> toggleMute();
            case F -> toggleFullscreen();
            case ESCAPE -> {
                if (stage.isFullScreen()) toggleFullscreen();
                else backToLibrary();
            }
            case BACK_SPACE -> backToLibrary();
            default -> { }
        }
    }
}