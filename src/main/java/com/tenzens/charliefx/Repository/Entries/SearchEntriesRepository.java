package com.tenzens.charliefx.Repository.Entries;

import com.tenzens.charliefx.Repository.Entries.Model.ClientEntry;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SearchEntriesRepository {

    private final SortedList<ClientEntry> allEntries;

    private final ObservableList<ClientEntry> searchResult;

    private final IntegerProperty numberOfSearchResultEntries;

    public SearchEntriesRepository(SortedList<ClientEntry> allEntries) {
        this.allEntries = allEntries;
        this.numberOfSearchResultEntries = new SimpleIntegerProperty();
        this.searchResult = FXCollections.observableArrayList();
    }

    public void search(String query) {
        searchResult.clear();

        List<ClientEntry> result = allEntries.stream().filter((clientEntry) -> {
            String[] searchWords = query.split("\\s+");

            String text = clientEntry.getContent().toSearchableString();

            StringBuilder regexBuilder = new StringBuilder();
            for (String word : searchWords) {
                if (!regexBuilder.isEmpty()) {
                    regexBuilder.append(".*\\b");
                }
                regexBuilder.append(Pattern.quote(word));
            }

            String regex = "\\b" + regexBuilder.toString();
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);

            return matcher.find();

        }).collect(Collectors.toList());

        searchResult.addAll(
                result
        );

        numberOfSearchResultEntries.set(searchResult.size());
    }

    public ObservableList<ClientEntry> getSearchResult() {
        return searchResult;
    }

    public IntegerProperty getNumberOfSearchResultEntries() {
        return numberOfSearchResultEntries;
    }

    public IntegerProperty numberOfSearchResultEntriesProperty() {
        return numberOfSearchResultEntries;
    }
}
