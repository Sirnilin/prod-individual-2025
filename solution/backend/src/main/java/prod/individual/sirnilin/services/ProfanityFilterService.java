package prod.individual.sirnilin.services;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import prod.individual.sirnilin.models.BannedWordModel;
import prod.individual.sirnilin.repositories.BannedWordRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProfanityFilterService {

    private final BannedWordRepository bannedWordRepository;
    private Set<String> bannedWords = new HashSet<>();;
    private Pattern bannedPattern;

    @PostConstruct
    public void init() {
        loadBannedWords();
    }

    public void loadBannedWords() {
        List<BannedWordModel> bannedWordList = bannedWordRepository.findAll();
        bannedWords.clear();
        for (BannedWordModel bw : bannedWordList) {
            bannedWords.add(bw.getWord().toLowerCase());
        }
        compilePattern();
    }

    private void compilePattern() {
        if (bannedWords.isEmpty()) {
            bannedPattern = Pattern.compile("$a", Pattern.CASE_INSENSITIVE);
        } else {
            StringBuilder patternBuilder = new StringBuilder();
            for (String word : bannedWords) {
                if (patternBuilder.length() > 0) {
                    patternBuilder.append("|");
                }

                patternBuilder.append("(?i)" + Pattern.quote(word));
            }
            bannedPattern = Pattern.compile(patternBuilder.toString());
        }
    }

    public boolean containsProfanity(String text) {
        if (text == null || text.isEmpty()) return false;
        return bannedPattern.matcher(text.toLowerCase()).find();
    }

    public String sanitizeText(String text) {
        if (text == null || text.isEmpty()) return text;
        return bannedPattern.matcher(text).replaceAll("****");
    }

    public void addBannedWord(List<String> words) {
        for (String word : words) {
            if (word != null && !word.trim().isEmpty()) {
                bannedWordRepository.findByWordIgnoreCase(word.toLowerCase())
                        .ifPresentOrElse(existing -> {
                            throw new IllegalArgumentException("Word already exists");
                        }, () -> {
                            BannedWordModel bannedWord = new BannedWordModel(word.toLowerCase());
                            bannedWordRepository.save(bannedWord);
                            bannedWords.add(word.toLowerCase());
                            compilePattern();
                        });
            }
        }
    }

    public void removeBannedWord(List<String> words) {
        for (String word : words) {
            bannedWordRepository.findByWordIgnoreCase(word.toLowerCase()).ifPresent(bw -> {
                bannedWordRepository.delete(bw);
                bannedWords.remove(word.toLowerCase());
                compilePattern();
            });
        }
    }
}
