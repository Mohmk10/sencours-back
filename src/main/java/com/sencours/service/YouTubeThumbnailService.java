package com.sencours.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class YouTubeThumbnailService {

    private static final Pattern[] YOUTUBE_PATTERNS = {
            Pattern.compile("(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/)([a-zA-Z0-9_-]{11})"),
            Pattern.compile("^([a-zA-Z0-9_-]{11})$")
    };

    /**
     * Extrait l'ID de la video YouTube depuis une URL
     */
    public String extractVideoId(String youtubeUrl) {
        if (youtubeUrl == null || youtubeUrl.isBlank()) {
            return null;
        }

        for (Pattern pattern : YOUTUBE_PATTERNS) {
            Matcher matcher = pattern.matcher(youtubeUrl);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        log.warn("Could not extract YouTube video ID from: {}", youtubeUrl);
        return null;
    }

    /**
     * Genere l'URL du thumbnail YouTube haute qualite (hqdefault pour compatibilite)
     */
    public String getThumbnailUrl(String youtubeUrl) {
        String videoId = extractVideoId(youtubeUrl);
        if (videoId == null) {
            return null;
        }
        return "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
    }

    /**
     * Verifie si une URL est une URL YouTube valide
     */
    public boolean isYouTubeUrl(String url) {
        return extractVideoId(url) != null;
    }
}
